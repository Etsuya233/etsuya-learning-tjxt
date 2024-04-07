package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.remark.constant.RedisConstant;
import com.tianji.remark.controller.LikedStatController;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedStat;
import com.tianji.remark.mapper.LikedStatMapper;
import com.tianji.remark.service.ILikedStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;

@Service
@RequiredArgsConstructor
public class LikedStatServiceRedisImpl extends ServiceImpl<LikedStatMapper, LikedStat> implements ILikedStatService {

	private final StringRedisTemplate redisTemplate;
	private final RabbitMqHelper rabbitMqHelper;

	@Override
	public void updateLikedTimesDB(LikeRecordFormDTO dto) {
		//只有DB实现需要用到
	}

	@Override
	public List<LikedTimesDTO> bizLikedTimesBatch(String bizType, List<Long> bizIds) {
		List<Object> list = redisTemplate.executePipelined(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				StringRedisConnection src = (StringRedisConnection) connection;
				for (Long bizId : bizIds) {
					String key = RedisConstant.LIKES_STAT_KEY_PREFIX + bizType;
					src.zScore(key, bizId.toString());
				}
				return null;
			}
		});
		return IntStream.range(0, list.size())
				.mapToObj(i -> {
					int likedTimes = 0;
					if(list.get(i) != null){
						likedTimes = ((Double) list.get(i)).intValue();
					}
					LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
					likedTimesDTO.setBizId(bizIds.get(i));
					likedTimesDTO.setLikedTimes(likedTimes);
					return likedTimesDTO;
				}).collect(Collectors.toList());
	}

	@Override
	public void updateLikedTimes(String bizType, int maxBizSize) { //TODO need 2 be test!
		// 1.读取并移除Redis中缓存的点赞总数
		String key = RedisConstant.LIKES_STAT_KEY_PREFIX + bizType;
		Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
		if (CollUtils.isEmpty(tuples)) {
			return;
		}
		// 2.数据转换
		List<LikedTimesDTO> list = new ArrayList<>(tuples.size());
		for (ZSetOperations.TypedTuple<String> tuple : tuples) {
			String bizId = tuple.getValue();
			Double likedTimes = tuple.getScore();
			if (bizId == null || likedTimes == null) {
				continue;
			}
			list.add(LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.intValue()));
		}
		// 3.发送MQ消息
		rabbitMqHelper.send(
				LIKE_RECORD_EXCHANGE,
				StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType),
				list);
	}

}
