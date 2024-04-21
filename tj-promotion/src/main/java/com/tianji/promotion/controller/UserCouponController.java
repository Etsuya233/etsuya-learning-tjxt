package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

}
