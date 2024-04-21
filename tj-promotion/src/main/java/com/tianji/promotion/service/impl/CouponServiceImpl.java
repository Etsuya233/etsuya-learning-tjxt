package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.constant.PromotionConstant;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

	private final ICouponScopeService couponScopeService;
	private final IExchangeCodeService exchangeCodeService;
	private final CategoryClient categoryClient;
	private final CourseClient courseClient;
	private final CategoryCache categoryCache;
	private final IUserCouponService userCouponService;
	private final StringRedisTemplate redisTemplate;

	@Override
	@Transactional
	public void addCoupon(CouponFormDTO dto) {
		//保存优惠券条目
		Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
		coupon.setStatus(CouponStatus.DRAFT);
		save(coupon);
		//保存限定范围
		if(dto.getSpecific()){
			Long couponId = coupon.getId();
			List<Long> scopes = dto.getScopes();
			if(CollUtils.isEmpty(scopes)){
				throw new BizIllegalException("限定范围不能为空！");
			}
			couponScopeService.addCouponScopeById(couponId, scopes, 1);
		}
	}

	@Override
	public PageDTO<CouponPageVO> pageQueryCoupons(CouponQuery couponQuery) {
		//注意query里面的type是折扣类型DiscountType而不是优惠券类型Type
		//分页查询
		Page<Coupon> pageResult = this.lambdaQuery()
				.eq(couponQuery.getType() != null, Coupon::getDiscountType, couponQuery.getType())
				.eq(couponQuery.getName() != null, Coupon::getName, couponQuery.getName())
				.like(StringUtils.isNotBlank(couponQuery.getName()), Coupon::getStatus, couponQuery.getStatus())
				.page(couponQuery.toMpPageDefaultSortByCreateTimeDesc());
		List<Coupon> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(pageResult);
		}
		//封装数据
		List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
		return PageDTO.of(pageResult, list);
	}

	@Override
	public void beginIssue(CouponIssueFormDTO dto) { //Path Variable 可以直接放到pojo里？
		Coupon coupon = this.getById(dto.getId());
		//判断是否符合条件（未发放或暂停）
		if(coupon == null){
			throw new BizIllegalException("优惠券不存在！");
		}
		if(coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
			throw new BizIllegalException("优惠券状态错误！");
		}
		//修改状态
		LocalDateTime now = LocalDateTime.now();
		Coupon c = BeanUtils.copyBean(dto, Coupon.class);
		boolean isBegin = dto.getIssueBeginTime() == null  //是否立即发放
				|| dto.getIssueBeginTime().isBefore(now);
		if(isBegin) {
			c.setStatus(CouponStatus.ISSUING);
			c.setIssueBeginTime(now);
		} else c.setStatus(CouponStatus.UN_ISSUE);
		//修改生效时间与失效时间
		if(dto.getTermBeginTime() != null){ //指定开始与结束时间
			c.setTermBeginTime(dto.getTermBeginTime());
			c.setTermEndTime(dto.getTermEndTime());
		}
		//写入数据库
		this.updateById(c);

		//写入Redis
		if(isBegin){
			coupon.setIssueBeginTime(now);
			coupon.setIssueEndTime(dto.getIssueEndTime());
			cacheCouponInfo(coupon);
		}

		//生成兑换码（只针对于指定发放的优惠券 ObtainType）
		if(coupon.getObtainWay() == ObtainType.PUBLIC || coupon.getStatus() != CouponStatus.DRAFT) return;
		coupon.setIssueEndTime(c.getIssueEndTime()); //用于获取优惠券ID和设置兑换码的截止时间
		exchangeCodeService.asyncGenerateCode(coupon);
	}

	private void cacheCouponInfo(Coupon coupon) {
		String key = PromotionConstant.COUPON_INFO_PREFIX + coupon.getId();
		Map<String, String> map = new HashMap<>(4);
		map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
		map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
		map.put("totalNum", String.valueOf(coupon.getTotalNum()));
		map.put("userLimit", String.valueOf(coupon.getUserLimit()));
		// 2.写缓存
		redisTemplate.opsForHash().putAll(key, map);
	}

	@Override
	@Transactional
	public void modifyCoupon(CouponFormDTO dto) {
		Coupon c = this.getById(dto.getId());
		if(c == null){
			throw new BizIllegalException("优惠券不存在");
		}
		if(c.getStatus() != CouponStatus.DRAFT){
			throw new BizIllegalException("只有带发放的优惠券可以修改！");
		}
		//更新优惠券
		Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
		this.updateById(coupon);
		//更新范围
		couponScopeService.remove(new QueryWrapper<CouponScope>()
				.eq("coupon_id", coupon.getId())); //原来的范围一律清除
		List<Long> scopes = dto.getScopes();
		if(CollUtils.isNotEmpty(scopes)){ //添加范围
			couponScopeService.addCouponScopeById(dto.getId(), scopes, 1);
		}
		//删除兑换码
		exchangeCodeService.remove(new QueryWrapper<ExchangeCode>()
				.eq("exchange_target_id", dto.getId()));
		//生成兑换码
		if(coupon.getObtainWay() == ObtainType.PUBLIC) return;
		coupon.setIssueEndTime(c.getIssueEndTime()); //用于获取优惠券ID和设置兑换码的截止时间
		exchangeCodeService.asyncGenerateCode(coupon);
	}

	@Override
	public void deleteCoupon(Long id) {
		//删除优惠券
		this.removeById(id);
		//删除范围
		couponScopeService.remove(new QueryWrapper<CouponScope>()
				.eq("coupon_id", id));
		//删除兑换码
		exchangeCodeService.remove(new QueryWrapper<ExchangeCode>()
				.eq("exchange_target_id", id));
	}

	@Override
	public CouponDetailVO getCouponDetail(Long id) {
		//查询优惠券
		Coupon coupon = this.getById(id);
		if(coupon == null){
			throw new BizIllegalException("优惠券不存在！");
		}
		CouponDetailVO vo = BeanUtils.copyProperties(coupon, CouponDetailVO.class);
		//查询范围
		if(coupon.getSpecific()){
			List<CouponScope> scopes = couponScopeService.lambdaQuery()
					.eq(CouponScope::getCouponId, coupon.getId())
					.list();
			if(CollUtils.isNotEmpty(scopes)){

				//查询限定课程
				List<Long> courseIds = scopes.stream()
						.filter(s -> s.getType() == 2)
						.map(CouponScope::getBizId)
						.collect(Collectors.toList());
				List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
				Map<Long, CourseSimpleInfoDTO> courseMap = courseInfos.stream()
						.collect(Collectors.toMap(CourseSimpleInfoDTO::getId, Function.identity()));
				//设置分类
				List<CouponScopeVO> scopeVos = scopes.stream().map(s -> {
					CouponScopeVO scopeVo = new CouponScopeVO();
					scopeVo.setId(s.getId());
					if (s.getType() == 1) {
						scopeVo.setName(categoryCache.getNameByLv3Id(s.getBizId()));
					} else {
						CourseSimpleInfoDTO courseInfo = courseMap.get(s.getBizId());
						if (courseInfo != null) {
							scopeVo.setName(courseInfo.getName());
						}
					}
					return scopeVo;
				}).collect(Collectors.toList());
				vo.setScopes(scopeVos);
			}
		}
		return vo;
	}

	@Override
	public void beginIssueBatch(List<Coupon> records) {
		List<Long> couponIds = records.stream().map(Coupon::getId).collect(Collectors.toList());
		this.lambdaUpdate()
				.in(Coupon::getId, couponIds)
				.set(Coupon::getStatus, CouponStatus.ISSUING)
				.update();
		//写Redis缓存
		List<Coupon> coupons = this.lambdaQuery()
				.in(Coupon::getId, couponIds)
				.list();
		coupons.forEach(this::cacheCouponInfo);
	}

	@Override
	public void stopIssueBatch(List<Coupon> records) {
		List<Long> couponIds = records.stream().map(Coupon::getId).collect(Collectors.toList());
		this.lambdaUpdate()
				.in(Coupon::getId, couponIds)
				.set(Coupon::getStatus, CouponStatus.FINISHED)
				.update();
		//清除Redis缓存
		List<String> redisKeys = couponIds.stream()
				.map(id -> PromotionConstant.COUPON_INFO_PREFIX + id)
				.collect(Collectors.toList());
		redisTemplate.delete(redisKeys);
	}

	@Override
	public void pauseIssue(Long id) {
		// 1.查询旧优惠券
		Coupon coupon = getById(id);
		if (coupon == null) {
			throw new BadRequestException("优惠券不存在");
		}

		// 2.当前券状态必须是未开始或进行中
		CouponStatus status = coupon.getStatus();
		if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING) {
			// 状态错误，直接结束
			return;
		}

		// 3.更新状态
		boolean success = lambdaUpdate()
				.set(Coupon::getStatus, CouponStatus.PAUSE)
				.eq(Coupon::getId, id)
				.in(Coupon::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING)
				.update();
		if (!success) {
			// 可能是重复更新，结束
			log.error("重复暂停优惠券");
		}

		// 4.删除缓存
		redisTemplate.delete(PromotionConstant.COUPON_INFO_PREFIX + id);
	}

	@Override
	public List<CouponVO> couponList() {
		Long userId = UserContext.getUser();
		//查询优惠券（发放中且是可以抢的）
		List<Coupon> couponList = this.lambdaQuery()
				.eq(Coupon::getStatus, CouponStatus.ISSUING)
				.eq(Coupon::getObtainWay, ObtainType.PUBLIC)
				.list();
		//查询用户的优惠券信息
		List<Long> couponIds = couponList.stream()
				.map(Coupon::getId)
				.collect(Collectors.toList());
		List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
				.eq(UserCoupon::getUserId, userId)
				.in(UserCoupon::getCouponId, couponIds)
				.list();
		//用户优惠券数量
		Map<Long, Long> issuedMap = userCoupons.stream()
				.collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
		//用户优惠券未使用数量
		Map<Long, Long> usedMap = userCoupons.stream()
				.filter(c -> c.getStatus() == UserCouponStatus.UNUSED)
				.collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
		//封装数据
		return couponList.stream().map(c -> {
			CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);
			boolean available = c.getIssueNum() < c.getTotalNum(); //是否可以抢
			Long issueNum = issuedMap.getOrDefault(c.getId(), 0L);
			vo.setAvailable(available);
			Long usedNum = usedMap.getOrDefault(c.getId(), 0L);
			boolean received = usedNum < issueNum; //是否可以用了现在
			vo.setReceived(received);
			return vo;
		}).collect(Collectors.toList());
	}
}
