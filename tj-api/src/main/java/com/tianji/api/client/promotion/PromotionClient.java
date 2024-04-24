package com.tianji.api.client.promotion;

import com.tianji.api.client.promotion.fallback.PromotionClientFallback;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "promotion-service", fallbackFactory = PromotionClientFallback.class)
public interface PromotionClient {
	@ApiOperation("下订单时使用优惠券")
	@PostMapping("/user-coupons/discount")
	CouponDiscountDTO queryDiscountByOrder(@RequestBody OrderCouponDTO dto);

	@ApiOperation("核销优惠券")
	@PutMapping("/user-coupons/use")
	void useCoupon(@RequestBody List<Long> couponIds);

	@ApiOperation("退回优惠券")
	@PutMapping("/user-coupons/refund")
	void refundCoupon(@RequestBody List<Long> couponIds);

	@ApiOperation("查询优惠券使用规则")
	@GetMapping("/user-coupons/rules")
	List<String> queryCouponRules(@RequestParam("couponIds") List<Long> userCouponIds);

	@ApiOperation("查询我的优惠券可用方案")
	@PostMapping("/user-coupons/available")
	List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses);
}
