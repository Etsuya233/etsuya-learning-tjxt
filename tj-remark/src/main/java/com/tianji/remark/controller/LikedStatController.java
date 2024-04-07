package com.tianji.remark.controller;


import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.remark.service.ILikedRecordService;
import com.tianji.remark.service.ILikedStatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 点赞统计表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-05
 */
@RestController
@RequestMapping("/likes-stat")
@RequiredArgsConstructor
@Api("点赞接口统计类")
public class LikedStatController {

	private final ILikedStatService likedStatService;

	@GetMapping("/batch")
	@ApiOperation("批量查询帖子点赞数")
	public List<LikedTimesDTO> bizLikedTimesBatch(@RequestParam String bizType, @RequestParam List<Long> bizIds){
		return likedStatService.bizLikedTimesBatch(bizType, bizIds);
	}
}
