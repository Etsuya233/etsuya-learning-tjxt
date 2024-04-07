package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "管理端问题接口")
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

	private final IInteractionQuestionService interactionQuestionService;

	@GetMapping("/page")
	@ApiOperation("搜索问题")
	public PageDTO<QuestionAdminVO> pageQueryQuestion(QuestionAdminPageQuery queryDto){
		return interactionQuestionService.adminPageQuery(queryDto);
	}

	@PutMapping("/{id}/hidden/{hidden}")
	@ApiOperation("设置问题是否隐藏")
	public void setHidden(@PathVariable Boolean hidden, @PathVariable Long id){
		interactionQuestionService.setHidden(id, hidden);
	}

	@GetMapping("/{id}")
	@ApiOperation("根据问题ID查询详情")
	public QuestionAdminVO questionDetail(@PathVariable Long id){
		return interactionQuestionService.questionAdminDetail(id);
	}
}
