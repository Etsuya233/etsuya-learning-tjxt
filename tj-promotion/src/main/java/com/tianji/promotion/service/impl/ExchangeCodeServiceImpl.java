package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.constant.PromotionConstant;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.CodeVO;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

	private final BoundValueOperations<String, String> valueOps;
	private final StringRedisTemplate redisTemplate;

	public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
		this.valueOps = redisTemplate.boundValueOps(PromotionConstant.COUPON_CODE_SERIAL_KEY_REDIS);
		this.redisTemplate = redisTemplate;
	}

	@Override
	@Transactional
	@Async("generateExchangeCodeExecutor")
	public void asyncGenerateCode(Coupon coupon) {
		//基本信息
		Integer total = coupon.getTotalNum();
		Long couponId = coupon.getId();
		LocalDateTime endTime = coupon.getIssueEndTime();
		//生成兑换码
		List<ExchangeCode> list = new ArrayList<>(total);
		Long result = valueOps.increment(total); //第一个序列码
		if(result == null) return;
		int serialNumber = result.intValue(); //获取的是最后一个序列号，所以需要向前遍历
		for (int i = 0; i < total; i++) {
			ExchangeCode exchangeCode = new ExchangeCode();
			String code = CodeUtil.generateCode(serialNumber, couponId);//直接使用couponId作为fresh
			exchangeCode.setCode(code);
			exchangeCode.setExchangeTargetId(couponId);
			exchangeCode.setExpiredTime(endTime);
			exchangeCode.setId(serialNumber);
			exchangeCode.setStatus(ExchangeCodeStatus.UNUSED);
			exchangeCode.setType(1);
			serialNumber--;
			list.add(exchangeCode);
		}
		saveBatch(list);

		redisTemplate.opsForZSet().add(PromotionConstant.COUPON_RANGE_KEY, coupon.getId().toString(), serialNumber + total);
	}

	@Override
	public PageDTO<CodeVO> codePageQuery(CodeQuery codeQuery) {
		//查询
		Page<ExchangeCode> pageResult = this.lambdaQuery()
				.eq(ExchangeCode::getExchangeTargetId, codeQuery.getCouponId())
				.eq(ExchangeCode::getStatus, codeQuery.getStatus())
				.page(codeQuery.toMpPage());
		List<ExchangeCode> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)) return PageDTO.empty(pageResult);
		//封装
		List<CodeVO> list = records.stream().map(CodeVO::cast2CodeVO).collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}

	/**
	 * 设置该优惠券码被使用，也可以判断该有兑换码是否被使用。
	 * @param serialNumber 序列号
	 * @return true表示该兑换码没被使用，否则被使用了
	 */
	@Override
	public boolean updateExchangeCodeStatus(long serialNumber, boolean status) {
		Boolean b = redisTemplate.opsForValue().setBit(PromotionConstant.COUPON_CODE_MAP_KEY, serialNumber, status);
		return b != null && (!b == status);
	}

	@Override
	public Long getCouponIdBySerialNumber(long serialNumber) {
		Set<String> results = redisTemplate.opsForZSet().rangeByScore(
				PromotionConstant.COUPON_RANGE_KEY, serialNumber, 0x7fffffff, 0L, 1L);
		if (CollUtils.isEmpty(results)) {
			return null;
		}
		// 2.数据转换
		String next = results.iterator().next();
		return Long.parseLong(next);
	}
}
