package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**promotion
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-28
 */
@RestController
@RequestMapping("/learning-records")
@RequiredArgsConstructor
@Api(tags = "学习记录接口")
public class LearningRecordController {

	private final ILearningRecordService learningRecordService;

	@GetMapping("/learning-records/course/{courseId}")
	@ApiOperation("查询某课程学习记录")
	LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
		return learningRecordService.queryLearningRecordByCourse(courseId);
	}

	@PostMapping
	@ApiOperation("提交学习记录")
	public void submitLearningRecord(@RequestBody LearningRecordFormDTO learningRecord){
		learningRecordService.submitLearningRecord(learningRecord);
	}


}
