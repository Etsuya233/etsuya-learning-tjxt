package com.tianji.learning.handler;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
@Slf4j
public class LikedTimesChangedListener {

	private final IInteractionReplyService replyService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "learning.qa.liked.times.queue", durable = "true"),
			key = MqConstants.Key.QA_LIKED_TIMES_KEY
	))
	public void likeTimesChanged(LikedTimesDTO dto){
		if(dto == null) throw new BizIllegalException("MQ信息参数异常！");
		log.debug("监听到回答或评论{}的点赞数变更:{}", dto.getBizId(), dto.getLikedTimes());
		replyService.lambdaUpdate()
				.eq(InteractionReply::getId, dto.getBizId())
				.set(InteractionReply::getLikedTimes, dto.getLikedTimes())
				.update();
	}

}
