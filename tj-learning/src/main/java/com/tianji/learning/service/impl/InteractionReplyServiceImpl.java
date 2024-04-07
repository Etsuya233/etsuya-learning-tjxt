package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-31
 */
//@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

	private final IInteractionQuestionService interactionQuestionService;
	private final UserClient userClient;
	private final RemarkClient remarkClient;

	@Override
	public void postReply(ReplyDTO replyDTO) {
		//写入数据库
		InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
		reply.setUserId(UserContext.getUser());
		this.save(reply);
		//如果是回答
		if(replyDTO.getAnswerId() == null){
			interactionQuestionService.lambdaUpdate()
					.eq(InteractionQuestion::getId, replyDTO.getQuestionId())
					.set(InteractionQuestion::getLatestAnswerId, reply.getId())
					.setSql("answer_times = answer_times + 1")
					.update();
		} else { //如果是评论
			this.lambdaUpdate()
					.eq(InteractionReply::getId, replyDTO.getAnswerId())
					.setSql("reply_times = reply_times + 1")
					.update();
		}
		//如果是学生回答，就将问题改为未查看
		if(replyDTO.getIsStudent()){
			interactionQuestionService.lambdaUpdate()
					.eq(InteractionQuestion::getId, replyDTO.getQuestionId())
					.set(InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
					.update();
		}
	}

	@Override
	public PageDTO<ReplyVO> pageQueryReply(ReplyPageQuery queryDto) {
		Page<InteractionReply> pageResult;
		boolean queryQuestion = (queryDto.getAnswerId() == null);
		if(queryQuestion){ //分页查询回答
			pageResult = this.lambdaQuery()
					.eq(InteractionReply::getQuestionId, queryDto.getQuestionId())
					.eq(InteractionReply::getAnswerId, 0)
					.eq(InteractionReply::getHidden, false)
					.page(new Page<>(queryDto.getPageNo(), queryDto.getPageSize()));
		} else { //分页查询评论
			pageResult = this.lambdaQuery()
					.eq(InteractionReply::getAnswerId, queryDto.getAnswerId())
					.eq(InteractionReply::getHidden, false)
					.page(new Page<>(queryDto.getPageNo(), queryDto.getPageSize()));
		}
		List<InteractionReply> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(pageResult);
		}
//		//查询点赞信息
		List<Long> ids = records.stream().map(InteractionReply::getId).collect(Collectors.toList());
		Set<Long> likedReplies = remarkClient.whetherUserLikedBatch(ids);
		//查询用户信息
		Set<Long> userIds = records.stream()
				.map(InteractionReply::getUserId)
				.collect(Collectors.toSet());
		Map<Long, Boolean> targetAnonymity = new HashMap<>(); //被回复帖子是否匿名，关系到targetUser的名字是否展示
		if(!queryQuestion){
			Set<Long> targetUserId = records.stream()
					.map(InteractionReply::getTargetUserId)
					.collect(Collectors.toSet());
			userIds.addAll(targetUserId);
			//查询被回复的帖子，因为可能是匿名的
			Set<Long> targetReplyId = records.stream()
					.map(InteractionReply::getTargetReplyId)
					.collect(Collectors.toSet());
			List<InteractionReply> targetReply = this.lambdaQuery()
					.in(InteractionReply::getId, targetReplyId)
					.list();
			targetAnonymity.putAll(
					targetReply.stream()
							.collect(Collectors.toMap(InteractionReply::getId, InteractionReply::getAnonymity))
			);
		}
		List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDTOS.stream()
				.collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//封装信息
		List<ReplyVO> list = records.stream().map(record -> {
			ReplyVO vo = BeanUtils.copyBean(record, ReplyVO.class);
			if (!record.getAnonymity()) {
				UserDTO user = userMap.get(record.getUserId());
				if (user != null) {
					vo.setUserIcon(user.getIcon());
					vo.setUserName(user.getName());
				} else vo.setUserId(null);
			}
			if(!queryQuestion){ //如果是评论还要搜索被回复的用户（被评论用户也可能是匿名的）
				Boolean isAnonymity = targetAnonymity.get(record.getTargetReplyId());
				if(!isAnonymity){
					UserDTO targetUser = userMap.get(record.getTargetUserId());
					if(targetUser != null) {
						vo.setTargetUserName(targetUser.getName());
					}
				}
			}
			//如果点过赞
			if(likedReplies.contains(record.getId())){
				vo.setLiked(true);
			}
			return vo;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}

	@Override
	public PageDTO<ReplyVO> pageQueryReplyAdmin(ReplyPageQuery queryDto) {
		Page<InteractionReply> pageResult;
		boolean queryQuestion = (queryDto.getAnswerId() == null);
		if(queryQuestion){ //分页查询回答
			pageResult = this.lambdaQuery()
					.eq(InteractionReply::getQuestionId, queryDto.getQuestionId())
					.eq(InteractionReply::getAnswerId, 0)
					.page(new Page<>(queryDto.getPageNo(), queryDto.getPageSize()));
		} else { //分页查询评论
			pageResult = this.lambdaQuery()
					.eq(InteractionReply::getAnswerId, queryDto.getAnswerId())
					.page(new Page<>(queryDto.getPageNo(), queryDto.getPageSize()));
		}
		List<InteractionReply> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(pageResult);
		}
		//查询用户信息
		Set<Long> userIds = records.stream()
				.map(InteractionReply::getUserId)
				.collect(Collectors.toSet());
		if(!queryQuestion){
			Set<Long> targetUserId = records.stream()
					.map(InteractionReply::getTargetUserId)
					.collect(Collectors.toSet());
			userIds.addAll(targetUserId);
		}
		List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDTOS.stream()
				.collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//封装信息
		List<ReplyVO> list = records.stream().map(record -> {
			ReplyVO vo = BeanUtils.copyBean(record, ReplyVO.class);
			UserDTO user = userMap.get(record.getUserId());
			if (user != null) {
				vo.setUserIcon(user.getIcon());
				vo.setUserName(user.getName());
			} else vo.setUserId(null);
			if(!queryQuestion){ //如果是评论还要搜索被回复的用户（被评论用户也可能是匿名的）
				UserDTO targetUser = userMap.get(record.getTargetUserId());
				if(targetUser != null) {
					vo.setTargetUserName(targetUser.getName());
				}
			}
			return vo;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}

	@Override
	public void setHidden(Long id, Boolean hidden) {
		if(id == null || hidden == null) throw new BizIllegalException("请求参数异常！");
		boolean updated = this.lambdaUpdate()
				.eq(InteractionReply::getId, id)
				.eq(InteractionReply::getHidden, hidden)
				.update();
		if(!updated) throw new DbException("操作失败！");
	}
}
