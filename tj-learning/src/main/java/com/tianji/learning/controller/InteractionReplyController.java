package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-31
 */
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
@Api(tags = "回答与评论接口")
public class InteractionReplyController {

	private final IInteractionReplyService interactionReplyService;

	@PostMapping
	@ApiOperation("添加回答或评论")
	public void postReply(@RequestBody ReplyDTO replyDTO){
		interactionReplyService.postReply(replyDTO);
	}

	@GetMapping("/page")
	@ApiOperation("分页查询回答或评论")
	public PageDTO<ReplyVO> pageQueryReply(ReplyPageQuery queryDto){
		return interactionReplyService.pageQueryReply(queryDto);
	}
}
