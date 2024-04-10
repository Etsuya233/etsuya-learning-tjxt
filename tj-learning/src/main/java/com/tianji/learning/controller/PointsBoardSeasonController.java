package com.tianji.learning.controller;


import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.service.IPointsBoardSeasonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@RestController
@RequestMapping("/boards/seasons")
@Api(tags = "天梯榜季度接口")
@RequiredArgsConstructor
public class PointsBoardSeasonController {

	private final IPointsBoardSeasonService pointsBoardSeasonService;

	@GetMapping("/list")
	@ApiOperation("查询所有赛季")
	public List<PointsBoardSeason> seasonList(){
		return pointsBoardSeasonService.querySeasonList();
	}

}
