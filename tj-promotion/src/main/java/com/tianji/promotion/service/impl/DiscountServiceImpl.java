package com.tianji.promotion.service.impl;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.strategy.discount.DiscountStrategy;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountServiceImpl implements IDiscountService { //感觉这里要重新实现？

	private final UserCouponMapper userCouponMapper;
	private final ICouponScopeService couponScopeService;
	private final CouponMapper couponMapper;
	private final Executor discountSolutionExecutor;

	@Override
	public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
		//查询
		Long userId = UserContext.getUser();
		List<Coupon> coupons = userCouponMapper.queryMyCoupon(userId);
		if(CollUtils.isEmpty(coupons)){
			return List.of();
		}
		//初筛 筛选最佳情况下可用的券
		int totalPrice = orderCourses.stream()
				.mapToInt(OrderCourseDTO::getPrice)
				.sum();
		List<Coupon> availableCoupon = coupons.stream()
				.filter(c -> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalPrice, c))
				.collect(Collectors.toList());
		//细筛 按照每章券的要求来选出可以用的课程
		Map<Coupon, List<OrderCourseDTO>> couponCourseMap = getCouponListMap(orderCourses, availableCoupon);
		if(couponCourseMap.isEmpty()) return List.of();
		//全排列 获取每个组合的最佳叠加方案
		availableCoupon = new ArrayList<>(couponCourseMap.keySet()); //细筛后更新原来的availableCoupon
		List<List<Coupon>> solutions = PermuteUtil.permute(availableCoupon); //coupon作全排列
		availableCoupon.forEach(c -> solutions.add(List.of(c))); //添加单券的方案
		//计算价格 筛选最优解
		List<CouponDiscountDTO> list = Collections.synchronizedList(new ArrayList<>(solutions.size()));
		CountDownLatch countDownLatch = new CountDownLatch(solutions.size());
		solutions.forEach(solution -> {
			CompletableFuture.supplyAsync(
					() -> calculateDiscountWithCoupons(orderCourses, solution, couponCourseMap), discountSolutionExecutor)
					.thenAccept(dto -> {
						list.add(dto);
						countDownLatch.countDown();
					});
		});
		boolean success = false;
		try {
			success = countDownLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("优惠券组合计算超时！");
		}
		if(!success){
			return List.of();
		}
		return findBestSolution(list);
	}

	private Map<Coupon, List<OrderCourseDTO>> getCouponListMap(List<OrderCourseDTO> OrderCourseDtos, List<Coupon> coupons) {
		HashMap<Coupon, List<OrderCourseDTO>> couponCourseMap = new HashMap<>();
		coupons.forEach(c -> {
			boolean canUse = true;
			List<OrderCourseDTO> availableCourses = OrderCourseDtos;
			if(c.getSpecific()){
				//查询这一张券的范围
				List<CouponScope> scopes = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, c.getId()).list();
				Set<Long> scopeSet = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
				//遍历课程获取可以用的课程
				availableCourses = OrderCourseDtos.stream().filter(course -> scopeSet.contains(course.getId())).collect(Collectors.toList());
				//计算总价
				int sum = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
				//优惠券是否可用？
				canUse = DiscountStrategy.getDiscount(c.getDiscountType()).canUse(sum, c);
			}
			if(canUse) {
				couponCourseMap.put(c, availableCourses);
			}
		});
		return couponCourseMap;
	}

	@Override
	public CouponDiscountDTO queryDiscountByOrder(OrderCouponDTO dto) {
		//获取优惠券
		List<Coupon> coupons = couponMapper.queryCouponsByUserCouponIds(dto.getUserCouponIds(), UserCouponStatus.UNUSED);
		if(CollUtils.isEmpty(coupons)){
			return null;
		}
		Map<Coupon, List<OrderCourseDTO>> couponCourseMap = this.getCouponListMap(dto.getCourseList(), coupons);
		if(couponCourseMap.isEmpty()){
			return null;
		}
		//计算价格
		return this.calculateDiscountWithCoupons(dto.getCourseList(), coupons, couponCourseMap);
	}

	private CouponDiscountDTO calculateDiscountWithCoupons(List<OrderCourseDTO> courses,
														   List<Coupon> solution, //优惠券的顺序
														   Map<Coupon, List<OrderCourseDTO>> couponCourseMap) {
		CouponDiscountDTO ret = new CouponDiscountDTO();
		//初始化Price map
		Map<Long, Integer> priceMap = new HashMap<>(
				courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, OrderCourseDTO::getPrice)));
		Map<Long, Integer> discountDetailMap = new HashMap<>();
		for (Coupon coupon : solution) {
			List<OrderCourseDTO> availableCourses = couponCourseMap.get(coupon);
			//计算总价
			int sum = availableCourses.stream()
					.map(OrderCourseDTO::getId)
					.mapToInt(priceMap::get)
					.sum();
			Discount discountType = DiscountStrategy.getDiscount(coupon.getDiscountType());
			if(!discountType.canUse(sum, coupon)) {
				continue; //该券不可用
			}
			//计算总优惠
			int discount = discountType.calculateDiscount(sum, coupon);
			//平摊优惠
			int d = discount, size = availableCourses.size(), times = 0;
			for(OrderCourseDTO course : availableCourses) {
				Integer price = priceMap.get(course.getId());
				if(times + 1 == size){ //假如是最后一个
					int realDiscount = discount - d;
					priceMap.put(course.getId(), price - realDiscount);
					discountDetailMap.put(course.getId(), realDiscount);
					break;
				}
				int avgDiscount = (int) Math.ceil(1.0 * d / (size - times));
				int realDiscount = Math.min(avgDiscount, price);
				d -= realDiscount;
				priceMap.put(course.getId(), price - realDiscount);
				discountDetailMap.put(course.getId(), realDiscount);
				times++;
			}
			ret.setDiscountAmount(ret.getDiscountAmount() + discount);
			ret.getRules().add(discountType.getRule(coupon));
			ret.getIds().add(coupon.getId());
		}
		//优惠明细
		ret.setDiscountDetail(discountDetailMap);
		return ret;
	}

	private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
		// 1.准备Map记录最优解
		Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
		Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
		// 2.遍历，筛选最优解
		for (CouponDiscountDTO solution : list) {
			// 2.1.计算当前方案的id组合
			String ids = solution.getIds().stream()
					.sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
			// 2.2.比较用券相同时，优惠金额是否最大
			CouponDiscountDTO best = moreDiscountMap.get(ids);
			if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
				// 当前方案优惠金额少，跳过
				continue;
			}
			// 2.3.比较金额相同时，用券数量是否最少
			best = lessCouponMap.get(solution.getDiscountAmount());
			int size = solution.getIds().size();
			if (size > 1 && best != null && best.getIds().size() <= size) {
				// 当前方案用券更多，放弃
				continue;
			}
			// 2.4.更新最优解
			moreDiscountMap.put(ids, solution);
			lessCouponMap.put(solution.getDiscountAmount(), solution);
		}
		// 3.求交集
		Collection<CouponDiscountDTO> bestSolutions = CollUtils
				.intersection(moreDiscountMap.values(), lessCouponMap.values());
		// 4.排序，按优惠金额降序
		return bestSolutions.stream()
				.sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
				.collect(Collectors.toList());
	}
}
