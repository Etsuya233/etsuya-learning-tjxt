package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
@RestController
@RequestMapping("/coupons")
@Api(tags = "优惠券相关接口")
@RequiredArgsConstructor
public class CouponController {

	private final ICouponService couponService;

	@PostMapping
	@ApiOperation("添加优惠券")
	public void addCoupon(@Valid @RequestBody CouponFormDTO dto){
		couponService.addCoupon(dto);
	}

	@GetMapping("/page")
	@ApiOperation("分页查询优惠券")
	public PageDTO<CouponPageVO> pageQueryCoupons(CouponQuery couponQuery){
		return couponService.pageQueryCoupons(couponQuery);
	}

	@ApiOperation("发放优惠券接口")
	@PutMapping("/{id}/issue")
	public void beginIssue(@RequestBody @Valid CouponIssueFormDTO dto) {
		couponService.beginIssue(dto);
	}

	@ApiOperation("修改优惠券")
	@PutMapping("/{id}")
	public void modifyCoupon(@Valid @RequestBody CouponFormDTO dto) {
		couponService.modifyCoupon(dto);
	}

	@DeleteMapping("/{id}")
	@ApiOperation("删除优惠券")
	public void deleteCoupon(@PathVariable Long id){
		couponService.deleteCoupon(id);
	}

	@ApiOperation("查询优惠券详情")
	@GetMapping("/{id}")
	public CouponDetailVO getCouponDetail(@PathVariable Long id){
		return couponService.getCouponDetail(id);
	}

	@PutMapping("/{id}/pause")
	@ApiOperation("暂停发放优惠券")
	public void pauseIssue(@PathVariable Long id){
		couponService.pauseIssue(id);
	}

	@GetMapping("/list")
	@ApiOperation("获取首页优惠券列表")
	public List<CouponVO> couponList(){
		return couponService.couponList();
	}
}
