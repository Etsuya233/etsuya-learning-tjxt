package com.tianji.learning.service;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-27
 */
public interface ILearningLessonService extends IService<LearningLesson> {

	void addUserLesson(Long userId, List<Long> courseIds);

	PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

	LearningLessonVO learningNow();

	void deleteUserLesson(Long userId, Long courseId);

	Long isLessonValid(Long courseId);

	LearningLesson getLessonStatus(Long courseId);

	Integer countLearningLessonByCourse(Long courseId);

	void createPlan(LearningPlanDTO plan);

	LearningPlanPageVO getPlan(PageQuery pageQuery);
}
