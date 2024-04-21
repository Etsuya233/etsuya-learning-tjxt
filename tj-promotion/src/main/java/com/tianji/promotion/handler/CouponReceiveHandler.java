package com.tianji.promotion.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CouponReceiveHandler {

	private final CouponMapper couponMapper;
	private final IUserCouponService userCouponService;
	private final IExchangeCodeService exchangeCodeService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.Exchange.PROMOTION_EXCHANGE),
			value = @Queue(name = "coupon.receive.queue"),
			key = MqConstants.Key.COUPON_RECEIVE
	))
	public void addUserCoupon2DB(UserCouponDTO dto){
		Coupon coupon = couponMapper.selectById(dto.getCouponId());
		if(coupon == null){
			throw new BizIllegalException("优惠券不存在！");
		}
		LocalDateTime now = LocalDateTime.now();
		//封装信息写入数据库
		UserCoupon userCoupon = new UserCoupon();
		userCoupon.setUserId(dto.getUserId());
		userCoupon.setCouponId(dto.getCouponId());
		if(coupon.getTermDays() == null || coupon.getTermDays() == 0){
			userCoupon.setTermBeginTime(coupon.getTermBeginTime());
			userCoupon.setTermEndTime(coupon.getTermEndTime());
		} else {
			userCoupon.setTermBeginTime(now);
			userCoupon.setTermEndTime(now.plusDays(coupon.getTermDays()));
		}
		userCouponService.save(userCoupon);
		//优惠券数目+1
		int added = couponMapper.incrIssueNum(dto.getCouponId());
		if(added == 0){
			throw new BizIllegalException("抢购过于火热！抢购失败！");
		}
		//兑换码已使用
		if(dto.getSerialNumber() == null) return;
		exchangeCodeService.lambdaUpdate()
				.eq(ExchangeCode::getId, dto.getSerialNumber())
				.set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
				.update();
	}

}
