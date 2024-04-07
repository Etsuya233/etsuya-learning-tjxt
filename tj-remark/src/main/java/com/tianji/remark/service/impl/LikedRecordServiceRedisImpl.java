package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constant.RedisConstant;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
	private final StringRedisTemplate redisTemplate;

	@Override
	public void addLikeRecord(LikeRecordFormDTO dto) {
		if(dto == null) throw new BizIllegalException("点赞失败！");
		Long userId = UserContext.getUser();
		//查询用户是否点赞过
		Boolean liked = redisTemplate.opsForSet()
				.isMember(RedisConstant.LIKES_BIZ_KEY_PREFIX + dto.getBizId(), userId.toString());
		if(liked == null) liked = false;
		Long res = null;
		if(!liked && dto.getLiked()){ //之前没点过赞且这次点赞
			res = redisTemplate.opsForSet()
					.add(RedisConstant.LIKES_BIZ_KEY_PREFIX + dto.getBizId(), userId.toString());
		} else if(liked && !dto.getLiked()) { //之前点过，现在取消
			res = redisTemplate.opsForSet()
					.remove(RedisConstant.LIKES_BIZ_KEY_PREFIX + dto.getBizId(), userId.toString());
		}
		if(res == null || res == 0) throw new BizIllegalException("点赞失败！");
		//统计点赞次数并存在Redis中
		Long likedTimes = redisTemplate.opsForSet()
				.size(RedisConstant.LIKES_BIZ_KEY_PREFIX + dto.getBizId());
		if(likedTimes == null) return;
		redisTemplate.opsForZSet()
				.add(RedisConstant.LIKES_STAT_KEY_PREFIX + dto.getBizType(), //key
						dto.getBizId().toString(), //member
						likedTimes); //score
	}

	@Override
	public Set<Long> whetherUserLikedBatch(List<Long> ids) {
		Long userId = UserContext.getUser();
		List<Object> result = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
			StringRedisConnection src = (StringRedisConnection) connection;
			for (Long id : ids) {
				String key = RedisConstant.LIKES_BIZ_KEY_PREFIX + id;
				src.sIsMember(key, userId.toString());
			}
			return null;
		}); //返回的其实是Boolean集合
		return IntStream.range(0, result.size())
				.filter(i -> (boolean) result.get(i))
				.mapToObj(ids::get)
				.collect(Collectors.toSet());
	}
}
