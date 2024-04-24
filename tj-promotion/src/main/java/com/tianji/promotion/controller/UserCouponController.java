package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-15
 */
@RestController
@RequestMapping("/user-coupons")
@RequiredArgsConstructor
@Api(tags = "用户优惠券相关接口")
public class UserCouponController {

	private final IUserCouponService userCouponService;
	private final IDiscountService discountService;

	@PostMapping("/{id}/receive")
	@ApiOperation("手动领取优惠券")
	public void obtainPublicCoupon(@PathVariable Long id){
		userCouponService.obtainPublicCoupon(id);
	}

	@ApiOperation("兑换码兑换优惠券接口")
	@PostMapping("/{code}/exchange")
	public void exchangeCoupon(@PathVariable("code") String code){
		userCouponService.exchangeCoupon(code);
	}

	@GetMapping("/page")
	@ApiOperation("查询用户优惠券")
	public PageDTO<CouponVO> pageQueryMyCoupon(UserCouponQuery couponQuery){
		return userCouponService.pageQueryMyCoupon(couponQuery);
	}

	@ApiOperation("查询我的优惠券可用方案")
	@PostMapping("/available")
	public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses){
		return discountService.findDiscountSolution(orderCourses);
	}

	@ApiOperation("下订单时使用优惠券")
	@PostMapping("/discount")
	public CouponDiscountDTO queryDiscountByOrder(@RequestBody OrderCouponDTO dto){
		return discountService.queryDiscountByOrder(dto);
	}

	@ApiOperation("核销优惠券")
	@PutMapping("/use")
	public void useCoupon(@RequestBody List<Long> couponIds){
		userCouponService.useCoupon(couponIds);
	}

	@ApiOperation("退回优惠券")
	@PutMapping("/refund")
	public void refundCoupon(@RequestBody List<Long> couponIds){
		userCouponService.refundCoupon(couponIds);
	}

	@ApiOperation("查询优惠券使用规则")
	@GetMapping("/rules")
	public List<String> queryCouponRules(@RequestParam("couponIds") List<Long> userCouponIds){
		return userCouponService.queryCouponRules(userCouponIds);
	}



}
