package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Api(tags = "排行榜相关接口")
public class PointsBoardController {

	private final IPointsBoardService pointsBoardService;

	/**
	 * 当赛季号为0就是查询当前赛季，否则查询历史赛季
	 */
	@GetMapping
	@ApiOperation("查询积分榜")
	public PointsBoardVO getPointsBoardBySeasonId(PointsBoardQuery pointsBoardQuery){
		return pointsBoardService.getPointsBoardBySeasonId(pointsBoardQuery);
	}
}
