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
import com.tianji.common.exceptions.DbException;
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
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.strategy.discount.DiscountStrategy;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCouponServiceRedisLuaImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

	private final CouponMapper couponMapper;
	private final StringRedisTemplate redisTemplate;
	private final RabbitMqHelper rabbitMqHelper;
	private static final RedisScript<Long> RECEIVE_COUPON_LUA;
	private static final RedisScript<String> EXCHANGE_COUPON_LUA;

	static {
		RECEIVE_COUPON_LUA = RedisScript.of(new ClassPathResource("lua/receive_coupon.lua"), Long.class);
		EXCHANGE_COUPON_LUA = RedisScript.of(new ClassPathResource("lua/exchange_coupon.lua"), String.class);
	}

	@Override
	public void obtainPublicCoupon(Long couponId) {
		//获取基本信息
		Long userId = UserContext.getUser();
		//调用Lua脚本
		Long res = redisTemplate.execute(RECEIVE_COUPON_LUA,
				List.of(PromotionConstant.COUPON_INFO_PREFIX, PromotionConstant.COUPON_USER_PREFIX),
				userId.toString(), couponId.toString());
		if(res == null) throw new BizIllegalException("抢购失败！");
		int r = res.intValue();
		if(r != 0){
			throw new BizIllegalException(PromotionConstant.LUA_RETURN[r]);
		}
		//发送MQ消息
		UserCouponDTO userCouponDTO = new UserCouponDTO();
		userCouponDTO.setCouponId(couponId);
		userCouponDTO.setUserId(userId);
		rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,
				MqConstants.Key.COUPON_RECEIVE, userCouponDTO);
	}

	@Override
	public void exchangeCoupon(String code) {
		long serialNumber = CodeUtil.parseCode(code);
		Long userId = UserContext.getUser();
		//调用Lua脚本
		String res = redisTemplate.execute(EXCHANGE_COUPON_LUA,
				List.of(PromotionConstant.COUPON_INFO_PREFIX, PromotionConstant.COUPON_USER_PREFIX,
						PromotionConstant.COUPON_CODE_MAP_KEY, PromotionConstant.COUPON_RANGE_KEY),
				userId.toString(), String.valueOf(serialNumber));
		if(res == null) throw new BizIllegalException("抢购失败！");
		long r = Long.parseLong(res); //返回的是couponId或者错误代码
		if(r >= 1 && r <= 6){
			throw new BizIllegalException(PromotionConstant.LUA_RETURN[(int) r]);
		}
		// 发送MQ消息通知
		UserCouponDTO uc = new UserCouponDTO();
		uc.setUserId(userId);
		uc.setCouponId(r);
		uc.setSerialNumber((int) serialNumber);
		rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, uc);
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
	@Transactional
	public void useCoupon(List<Long> couponIds) {
		// 1.查询优惠券
		List<UserCoupon> userCoupons = listByIds(couponIds);
		if (CollUtils.isEmpty(userCoupons)) {
			return;
		}
		// 2.处理数据
		List<UserCoupon> list = userCoupons.stream()
				// 过滤无效券
				.filter(coupon -> {
					if (coupon == null) {
						return false;
					}
					if (UserCouponStatus.UNUSED != coupon.getStatus()) {
						return false;
					}
					LocalDateTime now = LocalDateTime.now();
					return !now.isBefore(coupon.getTermBeginTime()) && !now.isAfter(coupon.getTermEndTime());
				})
				// 组织新增数据
				.map(coupon -> {
					UserCoupon c = new UserCoupon();
					c.setId(coupon.getId());
					c.setStatus(UserCouponStatus.USED);
					return c;
				})
				.collect(Collectors.toList());

		// 4.核销，修改优惠券状态
		boolean success = updateBatchById(list);
		if (!success) {
			return;
		}
		// 5.更新已使用数量
		List<Long> usedCouponIds = list.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
		int c = couponMapper.incrUsedNum(usedCouponIds, 1);
		if (c < 1) {
			throw new DbException("更新优惠券使用数量失败！");
		}
	}

	@Override
	@Transactional
	public void refundCoupon(List<Long> couponIds) {
		//查询优惠券
		List<UserCoupon> coupons = this.listByIds(couponIds);
		if(CollUtils.isEmpty(coupons)) return;
		List<UserCoupon> userCoupons = coupons.stream()
				.filter(c -> c != null && c.getStatus() == UserCouponStatus.USED)
				.map(c -> {
					UserCoupon userCoupon = new UserCoupon().setCouponId(c.getId());
					// 3.判断有效期，是否已经过期，如果过期，则状态为 已过期，否则状态为 未使用
					LocalDateTime now = LocalDateTime.now();
					UserCouponStatus status = now.isAfter(c.getTermEndTime()) ?
							UserCouponStatus.EXPIRED : UserCouponStatus.UNUSED;
					userCoupon.setStatus(status);
					return userCoupon;
				}).collect(Collectors.toList());
		boolean updated = this.updateBatchById(userCoupons);
		if(!updated) return;
		List<Long> validCouponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
		int c = couponMapper.incrUsedNum(validCouponIds, -1);
		if (c < 1) {
			throw new DbException("更新优惠券使用数量失败！");
		}
	}

	@Override
	public List<String> queryCouponRules(List<Long> userCouponIds) {
		List<Coupon> coupons = couponMapper.selectBatchIds(userCouponIds);
		return coupons.stream().map(c -> {
			if(c == null) return "";
			return DiscountStrategy.getDiscount(c.getDiscountType()).getRule(c);
		}).collect(Collectors.toList());
	}
}
