package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-15
 */
//@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

	private final CouponMapper couponMapper;
	private final IExchangeCodeService exchangeCodeService;
	private final RedissonClient redissonClient;

	@Override
	@Transactional
	public void obtainPublicCoupon(Long id) {
		//获取基本信息
		Long userId = UserContext.getUser();
		//抢购优惠券
		IUserCouponService currentProxy = (IUserCouponService) AopContext.currentProxy();
		currentProxy.obtainCouponById(id, userId);
	}

	@Override
	@Transactional
	public void exchangeCoupon(String code) {
		long serialNumber = CodeUtil.parseCode(code);
		Long userId = UserContext.getUser();
		//查看是否已被兑换
		boolean success = exchangeCodeService.updateExchangeCodeStatus(serialNumber, true);
		if(!success){
			throw new BizIllegalException("该兑换码已被使用");
		}
		//查询优惠券
		ExchangeCode exchangeCode = exchangeCodeService.lambdaQuery()
				.eq(ExchangeCode::getId, serialNumber).one();
		Long couponId = exchangeCode.getExchangeTargetId();
		IUserCouponService currentProxy = (IUserCouponService) AopContext.currentProxy();
		currentProxy.obtainCouponById(couponId, userId);
	}

	@Transactional
	@Override
	@Lock(name = "coupon:lock:uid:#{userId}")
	public void obtainCouponById(Long couponId, Long userId) {
		//查询优惠券是否存在
		Coupon coupon = couponMapper.selectById(couponId);
		if(coupon == null) {
			throw new BizIllegalException("优惠券不存在！");
		}
		//判断是否可以抢
		LocalDateTime now = LocalDateTime.now();
		if(coupon.getIssueBeginTime().isAfter(now) || coupon.getIssueEndTime().isBefore(now)) {
			throw new BizIllegalException("不在优惠券抢购时间内！");
		}
		//用户拥有该优惠券的数量
		Integer count = this.lambdaQuery()
				.eq(UserCoupon::getUserId, userId)
				.eq(UserCoupon::getCouponId, couponId).count();
		//判断是否可以抢
		if(coupon.getIssueNum() >= coupon.getTotalNum() || coupon.getUserLimit() <= count){
			throw new BizIllegalException("该优惠券已达上限！");
		}
		//封装信息写入数据库
		UserCoupon userCoupon = new UserCoupon();
		userCoupon.setUserId(userId);
		userCoupon.setCouponId(couponId);
		if(coupon.getTermDays() == null || coupon.getTermDays() == 0){
			userCoupon.setTermBeginTime(coupon.getTermBeginTime());
			userCoupon.setTermEndTime(coupon.getTermEndTime());
		} else {
			userCoupon.setTermBeginTime(now);
			userCoupon.setTermEndTime(now.plusDays(coupon.getTermDays()));
		}
		this.save(userCoupon);
		//优惠券数目+1
		int added = couponMapper.incrIssueNum(couponId);
		if(added == 0){
			throw new BizIllegalException("抢购过于火热！抢购失败！");
		}
	}

	@Override
	public PageDTO<CouponVO> pageQueryMyCoupon(UserCouponQuery couponQuery) {
		Long userId = UserContext.getUser();
		//查询
		Page<UserCoupon> pageResult = this.lambdaQuery()
				.eq(UserCoupon::getUserId, userId)
				.eq(UserCoupon::getStatus, couponQuery.getStatus())
				.page(couponQuery.toMpPage());
		//封装
		List<UserCoupon> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)) return PageDTO.empty(pageResult);
		List<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
		List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
		List<CouponVO> list = coupons.stream()
				.map(c -> BeanUtils.copyBean(c, CouponVO.class))
				.collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}

	@Override
	public void useCoupon(List<Long> couponIds) {

	}

	@Override
	public void refundCoupon(List<Long> couponIds) {

	}

	@Override
	public List<String> queryCouponRules(List<Long> userCouponIds) {
		return List.of();
	}

}
