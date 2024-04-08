package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 签到记录表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@RestController
@Api(tags = "签到相关接口")
@RequiredArgsConstructor
@RequestMapping("/sign-records")
public class SignRecordController {
	private final ISignRecordService signRecordService;

	@PostMapping
	@ApiOperation("每日签到")
	public SignResultVO addSignRecord(){
		return signRecordService.addSignRecord();
	}

	@GetMapping
	@ApiOperation("查询本月签到记录")
	public List<Integer> querySignRecord(){
		return signRecordService.querySignRecord();
	}
}
