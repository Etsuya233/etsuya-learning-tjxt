package com.tianji.learning.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constant.RedisConstant;
import com.tianji.learning.domain.po.SignRecord;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mapper.SignRecordMapper;
import com.tianji.learning.mq.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 签到记录表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl extends ServiceImpl<SignRecordMapper, SignRecord> implements ISignRecordService {

	private final StringRedisTemplate redisTemplate;
	private final RabbitMqHelper rabbitMqHelper;

	@Override
	public SignResultVO addSignRecord() {
		//获取基本信息
		LocalDateTime now = LocalDateTime.now();
		Long userId = UserContext.getUser();
		//添加签到记录
		int offset = now.getDayOfMonth() - 1;
		String key = RedisConstant.SIGN_RECORD_KET_PREFIX + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
		Boolean signed = redisTemplate.opsForValue().setBit(key, offset, true);
		if(signed != null && signed) throw new BizIllegalException("不允许重复签到！");
		//连续签到几天 bitfield key GET i32(i?) 0
		List<Long> list = redisTemplate.opsForValue().bitField(key,
				BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.signed(now.getDayOfMonth())).valueAt(0));
		int consecutiveDays = 0;
		if(CollUtils.isNotEmpty(list)){
			int num = list.get(0).intValue();
			while((num & 1) == 1){
				consecutiveDays++;
				num >>= 1;
			}
		}
		//积分
		int rewardPoints = 0;
		switch (consecutiveDays) {
			case 7:
				rewardPoints = 10;
				break;
			case 14:
				rewardPoints = 20;
				break;
			case 28:
				rewardPoints = 40;
				break;
		}
		//保存积分明细
		rabbitMqHelper.send(
				MqConstants.Exchange.LEARNING_EXCHANGE,
				MqConstants.Key.SIGN_IN,
				SignInMessage.of(userId, rewardPoints + 2)
		);
		//封装信息
		SignResultVO vo = new SignResultVO();
		vo.setRewardPoints(rewardPoints);
		vo.setSignDays(consecutiveDays);
		return vo;
	}

	@Override
	public List<Integer> querySignRecord() {
		//获取基本信息
		Long userId = UserContext.getUser();
		LocalDateTime now = LocalDateTime.now();
		//查询
		String key = RedisConstant.SIGN_RECORD_KET_PREFIX + userId + ":"
				+ now.format(DateTimeFormatter.ofPattern("yyyyMM"));
		List<Long> list = redisTemplate.opsForValue().bitField(key,
				BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(now.getDayOfMonth())).valueAt(0));
		//封装数据
		if(CollUtils.isEmpty(list)) return List.of();
		Long bitmap = list.get(0);
		long auxNum = 1 << (now.getDayOfMonth() - 1);
		ArrayList<Integer> records = new ArrayList<>();
		for(int i = 0; i < now.getDayOfMonth(); i++){
			records.add((auxNum & bitmap) > 0? 1 : 0); //1是已签到 0是未签到
			auxNum >>= 1;
		}
		return records;
	}
}
