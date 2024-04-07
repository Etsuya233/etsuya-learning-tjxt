package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-31
 */
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@Api(tags = "问题接口")
public class InteractionQuestionController {
	private final IInteractionQuestionService interactionQuestionService;

	@PostMapping
	@ApiOperation("提交问题")
	public void submitQuestion(@Valid @RequestBody QuestionFormDTO dto){
		interactionQuestionService.submitQuestion(dto);
	}

	@PutMapping("/{id}")
	@ApiOperation("修改问题")
	public void modifyQuestion(@PathVariable("id") Long id, @RequestBody QuestionFormDTO dto){
		interactionQuestionService.modifyQuestion(id, dto);
	}

	@GetMapping("/page")
	@ApiOperation("查询某小节下的问题")
	public PageDTO<QuestionVO> queryQuestion(QuestionPageQuery dto){
		return interactionQuestionService.queryQuestion(dto);
	}

	@GetMapping("/{id}")
	@ApiOperation("查询问题详情")
	public QuestionVO queryQuestionById(@PathVariable Long id){
		return interactionQuestionService.queryQuestionById(id);
	}

	@DeleteMapping("/{id}")
	@ApiOperation("删除问题")
	public void deleteQuestion(@PathVariable Long id){
		interactionQuestionService.deleteQuestion(id);
	}

}
