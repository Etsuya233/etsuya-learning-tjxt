package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.enums.UserCouponStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
public interface CouponMapper extends BaseMapper<Coupon> {
	@Update("UPDATE coupon SET issue_num = issue_num + 1 WHERE id = #{couponId} and issue_num < coupon.total_num")
	int incrIssueNum(@Param("couponId") Long couponId);

	List<Coupon> queryCouponsByUserCouponIds(
			@Param("userCouponIds") List<Long> userCouponIds,
			@Param("status") UserCouponStatus status);

	@Update("UPDATE coupon SET used_num = used_num + #{amount} WHERE id = #{couponId}")
	int incrUsedNum(
			@Param("userCouponIds") List<Long> couponIds,
			@Param("amount") int amount);
}
