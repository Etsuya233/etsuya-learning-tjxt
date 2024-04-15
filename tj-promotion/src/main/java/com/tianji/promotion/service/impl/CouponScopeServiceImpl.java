package com.tianji.promotion.service.impl;

import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.mapper.CouponScopeMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券作用范围信息 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
@Service
public class CouponScopeServiceImpl extends ServiceImpl<CouponScopeMapper, CouponScope> implements ICouponScopeService {

	@Override
	@Transactional
	public void addCouponScopeById(Long couponId, List<Long> scopes, Integer type) {
		List<CouponScope> couponScopes = scopes.stream().map(s -> {
			CouponScope couponScope = new CouponScope();
			couponScope.setCouponId(couponId);
			couponScope.setBizId(s);
			couponScope.setType(type);
			return couponScope;
		}).collect(Collectors.toList());
		this.saveBatch(couponScopes);
	}
}
