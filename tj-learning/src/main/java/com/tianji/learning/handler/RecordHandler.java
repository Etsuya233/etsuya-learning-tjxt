package com.tianji.learning.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.JsonUtils;
import com.tianji.learning.constant.RedisConstant;
import com.tianji.learning.domain.dto.LearningRecordPushDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordHandler {
	private final StringRedisTemplate redisTemplate;
	private final ILearningRecordService learningRecordService;
	private final ILearningLessonService learningLessonService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.LEARNING_DELAY_EXCHANGE,
					type = ExchangeTypes.TOPIC, delayed = "true"),
			value = @Queue(name = "record.delay.queue", durable = "true"),
			key = MqConstants.Key.LEARNING_RECORD_PUSH_KEY
	))
	public void handleRecord2DB(LearningRecordPushDTO dto){
		if(dto == null){
			log.error("读取MQ信息异常！");
			return;
		}
		String redisKey = RedisConstant.LEARNING_RECORD_PUSH_KEY_PREFIX + dto.getLessonId();
		Object cacheData = redisTemplate.opsForHash().get(redisKey, dto.getSectionId().toString());
		if(cacheData == null){ //如果Redis读不到，那就是缓存没更新，不用管
			return;
		}
		LearningRecord record = JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
		if(!Objects.equals(record.getMoment(), dto.getMoment())){
			return; //不一致就是还在看
		}
		//更新记录
		record.setFinished(null);
		learningRecordService.updateById(record);
		learningLessonService.lambdaUpdate()
				.eq(LearningLesson::getId, dto.getLessonId())
				.set(LearningLesson::getLatestLearnTime, LocalDateTime.now())
				.set(LearningLesson::getLatestSectionId, dto.getSectionId())
				.update();
	}
}
