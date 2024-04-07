package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.remark.service.ILikedStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-05
 */
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

	private final ILikedStatService likedStatService;

	@Override
	@Transactional
	public void addLikeRecord(LikeRecordFormDTO dto) {
		if(dto == null) throw new BizIllegalException("点赞失败！");
		Long userId = UserContext.getUser();
		//查询用户是否点赞过
		Integer liked = this.lambdaQuery()
				.eq(LikedRecord::getUserId, userId)
				.eq(LikedRecord::getBizId, dto.getBizId())
				.count();
		boolean res = false;
		if(liked == 0 && dto.getLiked()){ //之前没点过赞且这次点赞
			LikedRecord likedRecord = new LikedRecord();
			likedRecord.setBizId(dto.getBizId());
			likedRecord.setBizType(dto.getBizType());
			likedRecord.setUserId(userId);
			res = this.save(likedRecord);
		} else if(liked == 1 && !dto.getLiked()) { //之前点过，现在取消
			res = this.remove(new QueryWrapper<LikedRecord>().lambda()
					.eq(LikedRecord::getBizId, dto.getBizId())
					.eq(LikedRecord::getUserId, userId));
		}
		if(!res) throw new BizIllegalException("操作失败！");
		else likedStatService.updateLikedTimesDB(dto);
	}

	@Override
	public Set<Long> whetherUserLikedBatch(List<Long> ids) {
		Long userId = UserContext.getUser();
		//查询这些帖子里那些是被该用户点赞过的
		List<LikedRecord> list = this.lambdaQuery()
				.eq(LikedRecord::getUserId, userId)
				.in(LikedRecord::getBizId, ids)
				.list();
		//转化为set
		return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
	}
}
