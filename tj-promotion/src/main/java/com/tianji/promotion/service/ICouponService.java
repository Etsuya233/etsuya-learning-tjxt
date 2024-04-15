package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;

import java.util.List;

/**id
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
public interface ICouponService extends IService<Coupon> {

	void addCoupon(CouponFormDTO dto);

	PageDTO<CouponPageVO> pageQueryCoupons(CouponQuery couponQuery);

	void beginIssue(CouponIssueFormDTO dto);

	void modifyCoupon(CouponFormDTO dto);

	void deleteCoupon(Long id);

	CouponDetailVO getCouponDetail(Long id);

	void beginIssueBatch(List<Coupon> records);

	void stopIssueBatch(List<Coupon> records);

	void pauseIssue(Long id);
}
