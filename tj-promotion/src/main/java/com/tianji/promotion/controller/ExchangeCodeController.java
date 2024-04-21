package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.CodeVO;
import com.tianji.promotion.service.IExchangeCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 兑换码 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-11
 */
@RestController
@RequestMapping("/codes")
@RequiredArgsConstructor
@Api(tags = "兑换码相关接口")
public class ExchangeCodeController {

	private final IExchangeCodeService exchangeCodeService;

	@GetMapping("/page")
	@ApiOperation("查询兑换码")
	public PageDTO<CodeVO> codePageQuery(CodeQuery codeQuery){
		return exchangeCodeService.codePageQuery(codeQuery);
	}
}
