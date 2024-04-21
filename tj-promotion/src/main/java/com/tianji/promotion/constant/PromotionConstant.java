package com.tianji.promotion.constant;

public interface PromotionConstant {
	String COUPON_CODE_SERIAL_KEY_REDIS = "serial:coupon";
	String COUPON_CODE_MAP_KEY = "coupon:code:map";
	String COUPON_OBTAIN_USER_LOCK_KEY_PREFIX = "coupon:lock:uid:";
	String COUPON_OBTAIN_LOCK_KEY_PREFIX = "coupon:lock";
	String COUPON_INFO_PREFIX = "coupon:info:";
	String COUPON_USER_PREFIX = "coupon:user:";
	String COUPON_RANGE_KEY = "coupon:range";

	String[] LUA_RETURN = new String[]{
			"", "优惠券不存在！", "优惠券库存不足！", "不再优惠券抢购时间内",
			"用户持有该优惠券已达上限！", "该兑换码已被使用！", "兑换码无效！"
	};
}
