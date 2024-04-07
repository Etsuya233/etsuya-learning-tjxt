package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.StringUtils;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.domain.po.LikedStat;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.mapper.LikedStatMapper;
import com.tianji.remark.service.ILikedStatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 点赞统计表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-05
 */
//@Service
@RequiredArgsConstructor
public class LikedStatServiceImpl extends ServiceImpl<LikedStatMapper, LikedStat> implements ILikedStatService {

	private final RabbitMqHelper rabbitMqHelper;

	@Override
	@Transactional
	public void updateLikedTimesDB(LikeRecordFormDTO dto) {
		//更新总赞数
		LikedStat likedStat = this.lambdaQuery()
				.eq(LikedStat::getBizId, dto.getBizId())
				.one();
		if(likedStat == null){
			likedStat = new LikedStat();
			likedStat.setBizId(dto.getBizId());
			likedStat.setBizType(dto.getBizType());
			likedStat.setLikedTimes(1);
			this.save(likedStat);
		} else {
			this.lambdaUpdate()
					.eq(LikedStat::getId, likedStat.getId())
					.setSql(dto.getLiked(), "liked_times = liked_times + 1")
					.setSql(!dto.getLiked(), "liked_times = liked_times - 1")
					.update();
		}
		//MQ
		likedStat = this.getById(likedStat.getId()); //获取最新点赞数
		rabbitMqHelper.send(
				MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
				StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, dto.getBizType()),
				LikedTimesDTO.of(dto.getBizId(), likedStat.getLikedTimes()));
	}

	@Override
	public List<LikedTimesDTO> bizLikedTimesBatch(String bizType, List<Long> bizIds) {
		//只有redis实现要用到
		return null;
	}

	@Override
	public void updateLikedTimes(String bizType, int maxBizSize) {

	}
}
