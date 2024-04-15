package com.tianji.promotion.domain.vo;

import com.tianji.promotion.domain.po.ExchangeCode;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("兑换码vo")
public class CodeVO {
	private String code;
	private Integer id;

	public static CodeVO cast2CodeVO(ExchangeCode exchangeCode){
		CodeVO codeVO = new CodeVO();
		codeVO.setCode(exchangeCode.getCode());
		codeVO.setId(exchangeCode.getId());
		return codeVO;
	}
}
