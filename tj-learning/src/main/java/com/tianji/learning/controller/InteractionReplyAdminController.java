package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "管理端评论接口")
@RequestMapping("/admin/replies")
@RequiredArgsConstructor
@RestController
public class InteractionReplyAdminController {

	private final IInteractionReplyService interactionReplyService;

	@GetMapping
	@ApiOperation("管理端查看问题回答与回复")
	public PageDTO<ReplyVO> pageQueryReplyAdmin(ReplyPageQuery queryDto){
		return interactionReplyService.pageQueryReplyAdmin(queryDto);
	}

	@PutMapping("/{id}/hidden/{hidden}")
	public void setHidden(@PathVariable Boolean hidden, @PathVariable Long id){
		interactionReplyService.setHidden(id, hidden);
	}

}
