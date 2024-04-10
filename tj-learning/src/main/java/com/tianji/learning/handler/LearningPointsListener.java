package com.tianji.learning.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.SignRecord;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.ISignRecordService;
import feign.Logger;
import javassist.bytecode.ExceptionsAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LearningPointsListener {

	private final IPointsRecordService pointsRecordService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue("sign.points.queue"),
			key = MqConstants.Key.SIGN_IN
	))
	public void signInMessageListener(SignInMessage msg){
		if(msg == null || msg.getUserId() == null || msg.getPoints() == null){
			log.error("MQ签到积分参数错误！");
			return;
		}
		pointsRecordService.addPointsRecord(msg.getUserId(), msg.getPoints(), PointsRecordType.SIGN);
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(value = "section.points.queue", durable = "true"),
			key = MqConstants.Key.LEARN_SECTION
	))
	public void sectionFirstFinishedListener(Long userId){
		if(userId == null){
			log.error("MQ信息学习积分参数错误！");
			return;
		}
		pointsRecordService.addPointsRecord(userId, PointsRecordType.LEARNING.getValue(), PointsRecordType.LEARNING);
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(value = "qa.points.queue", durable = "true"),
			key = MqConstants.Key.LEARN_SECTION
	))
	public void writeReplyListener(Long userId){
		if(userId == null){
			log.error("MQ信息回答问题积分参数错误！");
			return;
		}
		pointsRecordService.addPointsRecord(userId, PointsRecordType.QA.getValue(), PointsRecordType.QA);
	}


}
