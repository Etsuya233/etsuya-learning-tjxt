package com.tianji.learning.service.impl;

import cn.hutool.crypto.asymmetric.RSA;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.course.SubNumAndCourseNumDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-31
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

	private final UserClient userClient;
	private final InteractionReplyMapper interactionReplyMapper;
	private final SearchClient searchClient;
	private final CourseClient courseClient;
	private final CatalogueClient catalogueClient;
	private final CategoryCache categoryCache;
	private final RabbitMqHelper rabbitMqHelper;

	@Override
	public void submitQuestion(QuestionFormDTO dto) {
		Long userId = UserContext.getUser();
		InteractionQuestion interactionQuestion = BeanUtils.copyBean(dto, InteractionQuestion.class);
		interactionQuestion.setUserId(userId);
		boolean saved = this.save(interactionQuestion);
		if(!saved) throw new DbException("提交问题失败！");
	}

	@Override
	public void modifyQuestion(Long id, QuestionFormDTO dto) {
		InteractionQuestion interactionQuestion = BeanUtils.copyBean(dto, InteractionQuestion.class);
		interactionQuestion.setId(id);
		boolean updated = this.updateById(interactionQuestion);
		if(!updated) throw new DbException("更新回答失败！");
	}

	@Override
	public PageDTO<QuestionVO> queryQuestion(QuestionPageQuery dto) {
		Long userId = UserContext.getUser();
		//获取分页回答列表
		Page<InteractionQuestion> pageResult = this.lambdaQuery()
				.eq(dto.getOnlyMine(), InteractionQuestion::getUserId, userId)
				.eq(InteractionQuestion::getCourseId, dto.getCourseId())
				.eq(dto.getSectionId() != null, InteractionQuestion::getSectionId, dto.getSectionId())
				.eq(InteractionQuestion::getHidden, false)
				.page(new Page<>(dto.getPageNo(), dto.getPageSize()));
		List<InteractionQuestion> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(pageResult);
		}
		//查询最新评论信息
		List<Long> replyIds = records.stream().map(InteractionQuestion::getLatestAnswerId).collect(Collectors.toList());
		List<InteractionReply> replies = interactionReplyMapper.selectList(new QueryWrapper<InteractionReply>().lambda()
				.in(InteractionReply::getId, replyIds)
				.eq(InteractionReply::getHidden, false));
		Map<Long, InteractionReply> replyMap = replies.stream().collect(Collectors.toMap(InteractionReply::getId, r -> r));
		//查询帖子的和最新回复的用户信息
		List<Long> userIds = records.stream().map(InteractionQuestion::getUserId).collect(Collectors.toList());
		List<Long> replyUserIds = replies.stream().map(InteractionReply::getUserId).collect(Collectors.toList());
		userIds.addAll(replyUserIds);
		List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
		//封装信息
		List<QuestionVO> result = records.stream().map(record -> {
			QuestionVO res = BeanUtils.copyBean(record, QuestionVO.class);
			//用户
			UserDTO currentUser = userMap.get(record.getUserId());
			if(currentUser == null){
				res.setUserName("用户已注销");
			} else if(!record.getAnonymity()){ //匿名用户直接忽略
				res.setUserName(currentUser.getName());
				res.setUserIcon(currentUser.getIcon());
			}
			//最新回复
			if (record.getLatestAnswerId() != null) {
				InteractionReply reply = replyMap.get(record.getLatestAnswerId());
				if(reply != null) {
					res.setLatestReplyContent(reply.getContent());
					UserDTO replyUser = userMap.get(reply.getUserId());
					if(replyUser == null){
						res.setLatestReplyUser("用户已注销");
					} else if(!reply.getAnonymity()){ //匿名直接忽略
						res.setLatestReplyUser(replyUser.getName());
					}
				}
			}
			return res;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, result);
	}

	@Override
	public QuestionVO queryQuestionById(Long id) {
		InteractionQuestion interactionQuestion = this.getById(id);
		if(interactionQuestion == null || interactionQuestion.getHidden()) throw new BizIllegalException("问题不存在！");
		UserDTO userDTO = userClient.queryUserById(interactionQuestion.getUserId());
		QuestionVO questionVO = BeanUtils.copyBean(interactionQuestion, QuestionVO.class);
		if(userDTO == null){
			questionVO.setUserName("用户已注销");
		} else if(!interactionQuestion.getAnonymity()){
			questionVO.setUserName(userDTO.getName());
			questionVO.setUserIcon(userDTO.getIcon());
		}
		return questionVO;
	}

	@Override
	public void deleteQuestion(Long id) {
		Long userId = UserContext.getUser();
		InteractionQuestion question = this.getById(id);
		if(question == null) throw new BizIllegalException("问题不存在！");
		if(!userId.equals(question.getUserId())) throw new BizIllegalException("删除失败");
		boolean deleted = this.removeById(id);
		if(!deleted) throw new DbException("删除失败！");
		interactionReplyMapper.delete(new QueryWrapper<InteractionReply>().lambda()
				.eq(InteractionReply::getQuestionId, id));
	}

	@Override
	public PageDTO<QuestionAdminVO> adminPageQuery(QuestionAdminPageQuery queryDto) {
		if(queryDto == null) throw new BadRequestException("请求参数错误！");
		//设置问题为已查看
		this.lambdaUpdate()
				.set(InteractionQuestion::getStatus, QuestionStatus.CHECKED)
				.update();
		//获取对应搜索courseName关键字的courseId
		List<Long> searchCourseIds = null;
		if(StringUtils.isNotBlank(queryDto.getCourseName())){
			searchCourseIds = searchClient.queryCoursesIdByName(queryDto.getCourseName());
			if(CollUtils.isEmpty(searchCourseIds)){ //如果没有搜索到课程就返回空结果
				return PageDTO.empty(0L, 0L);
			}
		}
		//分页查询
		Page<InteractionQuestion> pageResult = this.lambdaQuery()
				.in(CollUtils.isNotEmpty(searchCourseIds), InteractionQuestion::getCourseId, searchCourseIds)
				.eq(queryDto.getStatus() != null, InteractionQuestion::getStatus, queryDto.getStatus())
				.ge(queryDto.getBeginTime() != null, InteractionQuestion::getCreateTime, queryDto.getBeginTime())
				.lt(queryDto.getEndTime() != null, InteractionQuestion::getCreateTime, queryDto.getEndTime())
				.page(new Page<>(queryDto.getPageNo(), queryDto.getPageSize()));
		List<InteractionQuestion> records = pageResult.getRecords();
		//获取各种数据
		Set<Long> userIds = new HashSet<>();
		Set<Long> courseIds = new HashSet<>();
		Set<Long> cataIds = new HashSet<>();
		records.forEach(record -> {
			userIds.add(record.getUserId());
			courseIds.add(record.getCourseId());
			cataIds.add(record.getChapterId());
			cataIds.add(record.getSectionId());
		});
		//获取用户
		List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDTOS.stream()
				.collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//获取课程
		List<CourseSimpleInfoDTO> courseInfo = courseClient.getSimpleInfoList(courseIds);
		Map<Long, CourseSimpleInfoDTO> courseMap = courseInfo.stream()
				.collect(Collectors.toMap(CourseSimpleInfoDTO::getId, Function.identity()));
		//获取课程目录
		List<CataSimpleInfoDTO> cataInfo = catalogueClient.batchQueryCatalogue(cataIds);
		Map<Long, String> cataMap = cataInfo.stream()
				.collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
		//封装结果
		List<QuestionAdminVO> ret = records.stream().map(record -> {
			QuestionAdminVO vo = BeanUtils.copyBean(record, QuestionAdminVO.class);
			UserDTO user = userMap.get(record.getUserId());
			if (user != null){
				vo.setUserName(user.getName());
				vo.setUserIcon(user.getIcon());
			}
			CourseSimpleInfoDTO course = courseMap.get(record.getCourseId());
			if (course != null) {
				vo.setCourseName(course.getName());
				vo.setChapterName(cataMap.getOrDefault(record.getChapterId(), ""));
				vo.setSectionName(cataMap.getOrDefault(record.getSectionId(), ""));
				vo.setCategoryName(categoryCache.getCategoryNames(course.getCategoryIds()));
			}
			return vo;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, ret);
	}

	@Override
	public void setHidden(Long id, Boolean hidden) {
		if(id == null || hidden == null) throw new BizIllegalException("请求参数错误！");
		boolean updated = this.lambdaUpdate()
				.eq(InteractionQuestion::getId, id)
				.set(InteractionQuestion::getHidden, hidden)
				.update();
		if(!updated) throw new DbException("操作失败！");
	}

	@Override
	public QuestionAdminVO questionAdminDetail(Long id) {
		if(id == null) throw new BizIllegalException("请求参数错误！");
		InteractionQuestion interactionQuestion = this.getById(id);
		QuestionAdminVO vo = BeanUtils.copyBean(interactionQuestion, QuestionAdminVO.class);
		//用户
		UserDTO user = userClient.queryUserById(interactionQuestion.getUserId());
		if(user != null){
			vo.setUserName(user.getUsername());
			vo.setUserIcon(user.getIcon());
		}
		//课程 教师 分类
		CourseFullInfoDTO course = courseClient.getCourseInfoById(interactionQuestion.getCourseId(), false, true);
		if(course != null){
			vo.setCourseName(course.getName());
			List<UserDTO> teachers = userClient.queryUserByIds(course.getTeacherIds());
			String teacher = teachers.stream()
					.map(UserDTO::getName)
					.collect(Collectors.joining("-"));
			vo.setTeacherName(teacher);
			vo.setCategoryName(categoryCache.getCategoryNames(course.getCategoryIds()));
		}
		//章节
		List<CataSimpleInfoDTO> cata = catalogueClient.batchQueryCatalogue(List.of(interactionQuestion.getChapterId(), interactionQuestion.getSectionId()));
		if(CollUtils.isNotEmpty(cata)){
			if(Objects.equals(cata.get(0).getId(), interactionQuestion.getChapterId())){
				vo.setChapterName(cata.get(0).getName());
				vo.setSectionName(cata.get(1).getName());
			} else {
				vo.setChapterName(cata.get(1).getName());
				vo.setSectionName(cata.get(0).getName());
			}
		}
		return vo;
	}

}
