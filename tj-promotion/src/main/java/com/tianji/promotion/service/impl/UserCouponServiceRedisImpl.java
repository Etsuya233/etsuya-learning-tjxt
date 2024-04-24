package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constant.PromotionConstant;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Service
@RequiredArgsConstructor
public class UserCouponServiceRedisImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

	private final CouponMapper couponMapper;
	private final IExchangeCodeService exchangeCodeService;
	private final StringRedisTemplate redisTemplate;
	private final RabbitMqHelper rabbitMqHelper;

	@Override
	@Transactional
	@Lock(name = "coupon:lock:#{couponId}")
	public void obtainPublicCoupon(Long couponId) {
		//获取基本信息
		Long userId = UserContext.getUser();
		//查询优惠券是否存在
		String couponInfoKey = PromotionConstant.COUPON_INFO_PREFIX + couponId;
		Map<Object, Object> entries = redisTemplate.opsForHash().entries(couponInfoKey);
		if(MapUtil.isEmpty(entries)){
			throw new BizIllegalException("优惠券不存在！");
		}
		Coupon coupon = queryCouponByCache(couponId);
		//判断是否可以抢
		LocalDateTime now = LocalDateTime.now();
		if(coupon.getIssueBeginTime().isAfter(now) || coupon.getIssueEndTime().isBefore(now)) {
			throw new BizIllegalException("不在优惠券抢购时间内！");
		}
		//用户拥有该优惠券的数量
		String userCountKey = PromotionConstant.COUPON_USER_PREFIX + couponId;
		Long count = redisTemplate.opsForHash().increment(userCountKey, userId.toString(), 1);
		//判断是否可以抢
		if(coupon.getTotalNum() == 0 || count > coupon.getUserLimit()){
			throw new BizIllegalException("该优惠券已达上限！");
		}
		//扣减库存
		redisTemplate.opsForHash().increment(couponInfoKey, "totalNum", -1);
		//发送MQ消息
		UserCouponDTO userCouponDTO = new UserCouponDTO();
		userCouponDTO.setCouponId(couponId);
		userCouponDTO.setUserId(userId);
		rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,
				MqConstants.Key.COUPON_RECEIVE, userCouponDTO);
	}

	@Override
	@Transactional
	@Lock(name = "coupon:lock:uid:#{T(com.tianji.common.utils.UserContext).getUser()}")
	public void exchangeCoupon(String code) {
		long serialNumber = CodeUtil.parseCode(code);
		Long userId = UserContext.getUser();
		//查看是否已被兑换
		boolean success = exchangeCodeService.updateExchangeCodeStatus(serialNumber, true);
		if(!success){
			throw new BizIllegalException("该兑换码已被使用");
		}
		try {
			// 3.查询兑换码对应的优惠券id
			Long couponId = exchangeCodeService.getCouponIdBySerialNumber(serialNumber);
			if (couponId == null) {
				throw new BizIllegalException("兑换码不存在！");
			}
			Coupon coupon = queryCouponByCache(couponId);
			// 4.是否过期
			LocalDateTime now = LocalDateTime.now();
			if (now.isAfter(coupon.getIssueEndTime()) || now.isBefore(coupon.getIssueBeginTime())) {
				throw new BizIllegalException("优惠券活动未开始或已经结束");
			}

			// 5.校验每人限领数量
			// 5.1.查询领取数量
			String key = PromotionConstant.COUPON_USER_PREFIX + couponId;
			Long count = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
			// 5.2.校验限领数量
			if(coupon.getTotalNum() < 1){
				throw new BizIllegalException("优惠券库存不足！");
			}
			if(count > coupon.getUserLimit()){
				throw new BadRequestException("超出领取数量");
			}
			// 5.3 减少库存
			redisTemplate.opsForHash().increment(PromotionConstant.COUPON_INFO_PREFIX + couponId, "totalNum", -1);

			// 6.发送MQ消息通知
			UserCouponDTO uc = new UserCouponDTO();
			uc.setUserId(userId);
			uc.setCouponId(couponId);
			uc.setSerialNumber((int) serialNumber);
			rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, uc);
		} catch (Exception e) {
			// 重置兑换的标记 0
			exchangeCodeService.updateExchangeCodeStatus(serialNumber, true);
			throw e;
		}
	}

	/**
	 * Redis实现不用实现这个方法
	 * @param couponId 优惠券Id
	 * @param userId 用户Id
	 */
	@Transactional
	@Override
	public void obtainCouponById(Long couponId, Long userId) {
		//不用实现
	}

	private Coupon queryCouponByCache(Long couponId) {
		Map<Object, Object> entries = redisTemplate.opsForHash().entries(PromotionConstant.COUPON_INFO_PREFIX + couponId);
		if(MapUtil.isEmpty(entries)){
			throw new BizIllegalException("优惠券不存在！");
		}
		Coupon coupon = BeanUtils.mapToBean(entries, Coupon.class, false, CopyOptions.create());
		coupon.setId(couponId);
		return coupon;
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
