package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-05
 */
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Api("点赞记录接口")
public class LikedRecordController {

	private final ILikedRecordService likedRecordService;

	@PostMapping
	@ApiOperation("点赞或取消")
	public void addLikeRecord(@RequestBody LikeRecordFormDTO dto){
		likedRecordService.addLikeRecord(dto);
	}

	@GetMapping("/list")
	@ApiOperation("查询用户是否点赞列表。点赞过的放回bizId.")
	public Set<Long> whetherUserLikedBatch(@RequestParam("bizIds") List<Long> ids){
		return likedRecordService.whetherUserLikedBatch(ids);
	}

}
