package com.tianji.learning.task;

import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LessonTasks {

	private final ILearningLessonService learningLessonService;

	@Scheduled(cron = "* 0,30 * * * *") //每30分钟
	public void detectExpireLessons(){
		LocalDateTime now = LocalDateTime.now();
		learningLessonService.lambdaUpdate()
				.lt(LearningLesson::getExpireTime, now)
				.set(LearningLesson::getStatus, LessonStatus.EXPIRED)
				.update();
	}
}
