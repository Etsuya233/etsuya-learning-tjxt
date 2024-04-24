package com.tianji.api.client.promotion.fallback;

import com.tianji.api.client.promotion.PromotionClient;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.exceptions.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

@Slf4j
public class PromotionClientFallback implements FallbackFactory<PromotionClient> {
	@Override
	public PromotionClient create(Throwable cause) {
		log.error("促销模块出现异常:{}", cause.getMessage());
		return new PromotionClient(){
			@Override
			public CouponDiscountDTO queryDiscountByOrder(OrderCouponDTO dto) {
				return null;
			}

			@Override
			public void useCoupon(List<Long> couponIds) {
				throw new BizIllegalException(500, "核销优惠券异常", cause);
			}

			@Override
			public void refundCoupon(List<Long> couponIds) {
				throw new BizIllegalException(500, "退回优惠券异常", cause);
			}

			@Override
			public List<String> queryCouponRules(List<Long> userCouponIds) {
				return List.of();
			}

			@Override
			public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
				return List.of();
			}
		};
	}
}
