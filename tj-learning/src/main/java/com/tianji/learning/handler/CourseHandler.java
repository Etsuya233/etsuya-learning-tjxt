package com.tianji.learning.handler;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseHandler {

	private final ILearningLessonService learningLessonService;

	@RabbitListener(bindings = @QueueBinding(
			value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
			exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
			key = MqConstants.Key.ORDER_PAY_KEY
	))
	public void listenLessonPay(OrderBasicDTO orderBasicDTO){
		if(orderBasicDTO == null || orderBasicDTO.getUserId() == null
				|| orderBasicDTO.getOrderId() == null
				|| CollUtils.isEmpty(orderBasicDTO.getCourseIds())){
			log.error("接收到的MQ消息有误，某些参数为空！");
			return;
		}
		log.info("监听到{}的订单{}，将{}写入课表中.", orderBasicDTO.getUserId(), orderBasicDTO.getOrderId(), orderBasicDTO.getCourseIds());
		learningLessonService.addUserLesson(orderBasicDTO.getUserId(), orderBasicDTO.getCourseIds());
	}

	@RabbitListener(bindings = @QueueBinding(
			value = @Queue(name = "learning.lesson.refund.key", durable = "true"),
			exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
			key = MqConstants.Key.ORDER_REFUND_KEY
	))
	public void listenRefundSuccess(OrderBasicDTO order){
		if(order == null || CollUtils.isEmpty(order.getCourseIds())){ //订单在发消息前设置状态
			log.info("接收到的MQ消息有误，某些参数为空！");
			return;
		}
		List<Long> courseIds = order.getCourseIds();
		Long courseId = courseIds.get(0);
		Long userId = UserContext.getUser();
		log.info("由于用户{}申请退款，删除{}课程", userId, courseId);
		learningLessonService.deleteUserLesson(userId, courseId);
	}

}
