package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-15
 */
public interface IUserCouponService extends IService<UserCoupon> {

	void obtainPublicCoupon(Long id);

	void exchangeCoupon(String code);

	void obtainCouponById(Long couponId, Long userId);

	PageDTO<CouponVO> pageQueryMyCoupon(UserCouponQuery couponQuery);
}
