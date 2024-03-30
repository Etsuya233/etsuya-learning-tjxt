package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-27
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "用户课程接口")
@RequiredArgsConstructor
public class LearningLessonController {

	private final ILearningLessonService learningLessonService;

	@GetMapping("/page")
	@ApiOperation("查询我的课表，排序字段 latest_learn_time:学习时间排序，create_time:购买时间排序") //前端传过来的？
	public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query){
		return learningLessonService.queryMyLessons(query);
	}

	@GetMapping("/now")
	@ApiOperation("正在学习的课程信息以及总数（也存放到的这个VO里）")
	public LearningLessonVO learningNow(){ //话说为什么不用R包围？
		return learningLessonService.learningNow();
	}

	@DeleteMapping("/{courseId}")
	@ApiOperation("删除用户课程")
	public void userLessonDelete(@PathVariable("courseId") Long courseId){
		learningLessonService.deleteUserLesson(UserContext.getUser(), courseId);
	}

	@GetMapping("/{courseId}/valid")
	@ApiOperation("检查课程是否有效")
	public Long isLessonValid(@PathVariable("courseId") Long courseId){
		return learningLessonService.isLessonValid(courseId);
	}

	@GetMapping("/{courseId}")
	@ApiOperation("查看用户某课程状态")
	public LearningLesson getLessonStatus(@PathVariable("courseId") Long courseId){
		return learningLessonService.getLessonStatus(courseId);
	}

	@GetMapping("/lessons/{courseId}/count")
	@ApiOperation("查询课程学习总人数")
	public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
		return learningLessonService.countLearningLessonByCourse(courseId);
	}

	@PostMapping("/plans")
	@ApiOperation("创建学习计划")
	public void createPlan(@RequestBody @Valid LearningPlanDTO plan){
		learningLessonService.createPlan(plan);
	}

	@GetMapping("/plans")
	@ApiOperation("查看学习计划")
	public LearningPlanPageVO getPlan(PageQuery pageQuery){
		return learningLessonService.getPlan(pageQuery);
	}

}
