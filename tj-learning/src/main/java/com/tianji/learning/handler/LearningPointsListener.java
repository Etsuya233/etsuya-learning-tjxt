package com.tianji.learning.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.SignRecord;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningPointsListener {

	private final IPointsRecordService pointsRecordService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue("sign.points.queue"),
			key = MqConstants.Key.SIGN_IN
	))
	public void signInMessageListener(SignInMessage msg){
		pointsRecordService.addPointsRecord(msg.getUserId(), msg.getPoints(), PointsRecordType.SIGN);
	}


}
