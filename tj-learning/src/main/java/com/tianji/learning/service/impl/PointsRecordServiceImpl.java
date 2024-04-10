package com.tianji.learning.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constant.RedisConstant;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

	private final StringRedisTemplate redisTemplate;

	@Override
	public void addPointsRecord(Long userId, Integer points, PointsRecordType type) {
		if(userId == null || points == null || type == null){
			log.error("添加积分记录信息参数异常！");
			return;
		}
		log.info("正在添加{}的{}类型积分：{}", userId, points, type.getDesc());
		PointsRecord record = new PointsRecord();
		record.setType(type);
		record.setUserId(userId);
		record.setPoints(points);
		if(type.getMaxPoints() != 0){ //假如改积分是有上限的
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime beginTime = LocalDateTimeUtil.beginOfDay(now);
			LocalDateTime endTime = LocalDateTimeUtil.endOfDay(now);
			//查询今日积分（用于查询是否达到上限）
			List<PointsRecord> records = this.lambdaQuery()
					.eq(PointsRecord::getUserId, userId)
					.ge(PointsRecord::getCreateTime, beginTime)
					.le(PointsRecord::getCreateTime, endTime)
					.eq(PointsRecord::getType, type)
					.list();
			int sum = records.stream()
					.mapToInt(PointsRecord::getPoints)
					.sum();
			//计算能得到的最大积分
			if(sum + points > type.getMaxPoints()){
				record.setPoints(type.getMaxPoints() - sum);
			} else {
				record.setPoints(points);
			}
		} else record.setPoints(points);
		this.save(record);
		//添加积分到排行榜
		if(record.getPoints() > 0){
			String key = RedisConstant.POINT_BOARDS_KEY_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
			redisTemplate.opsForZSet().incrementScore(key, userId.toString(), points);
		}
	}

	@Override
	public List<PointsStatisticsVO> queryPointsToday() {
		//准备基本信息
		Long userId = UserContext.getUser();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime beginTime = LocalDateTimeUtil.beginOfDay(now);
		LocalDateTime endTime = LocalDateTimeUtil.endOfDay(now);
		//查询积分
		List<PointsRecord> records = this.query()
				.eq("user_id", userId)
				.ge("create_time", beginTime)
				.le("create_time", endTime)
				.select("sum(points) as points", "type")
				.groupBy("type")
				.list();
		//封装
		return records.stream().map(record -> {
			PointsStatisticsVO vo = new PointsStatisticsVO();
			vo.setType(record.getType().getDesc());
			vo.setPoints(record.getPoints());
			vo.setMaxPoints(record.getType().getMaxPoints());
			return vo;
		}).collect(Collectors.toList());
	}
}
