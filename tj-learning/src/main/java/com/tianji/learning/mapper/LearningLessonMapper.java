package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学生课程表 Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-27
 */
public interface LearningLessonMapper extends BaseMapper<LearningLesson> {

	Integer queryWeekPlanCount(@Param("userId") Long userId);
}
