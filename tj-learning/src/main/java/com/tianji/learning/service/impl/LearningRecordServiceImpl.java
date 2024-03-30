package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-28
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

	private final ILearningLessonService learningLessonService;
	private final CourseClient courseClient;

	@Override
	public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
		Long userId = UserContext.getUser();
		LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
		//查询用户改课程对应的LessonID
		LearningLesson learningLesson = learningLessonService.lambdaQuery()
				.eq(LearningLesson::getUserId, userId)
				.eq(LearningLesson::getCourseId, courseId)
				.one();
		Long lessonId = learningLesson.getId();
		//查询学习记录
		List<LearningRecord> records = this.lambdaQuery()
				.eq(LearningRecord::getLessonId, lessonId)
				.list();
		List<LearningRecordDTO> recordDTOS = records.stream().map(record -> BeanUtils.copyBean(record, LearningRecordDTO.class))
				.collect(Collectors.toList());
		//获取最后学习的小节
		Long latestLearningSection = 0L;
		latestLearningSection = learningLesson.getLatestSectionId();
		//装载DTO
		learningLessonDTO.setId(lessonId);
		learningLessonDTO.setRecords(recordDTOS);
		learningLessonDTO.setLatestSectionId(latestLearningSection);
		return learningLessonDTO;
	}

	@Override
	@Transactional
	public void submitLearningRecord(LearningRecordFormDTO record) {
		boolean finished;
		if(record.getSectionType() == SectionType.VIDEO.getValue()){ //如果是视频
			finished = handleVideoRecord(record);
		} else { //如果是考试
			handleExamRecord(record);
			finished = true;
		}
		//更新lesson
		handleRecordUpdate(record, finished);
	}

	private boolean handleVideoRecord(LearningRecordFormDTO record) {
		Long userId = UserContext.getUser();
		LearningRecord old = this.lambdaQuery()
				.eq(LearningRecord::getLessonId, record.getLessonId()).one();
		if(old == null){
			//第一次提交
			LearningRecord learningRecord = BeanUtils.copyBean(record, LearningRecord.class);
			learningRecord.setUserId(userId);
			boolean saved = this.save(learningRecord);
			if(!saved) throw new DbException("添加课程记录失败！");
			return false; //第一次不会学完的
		}
		boolean firstTimeFinished = !old.getFinished() && record.getMoment().doubleValue() / record.getDuration() >= 0.5;
		boolean updated = this.lambdaUpdate()
				.eq(LearningRecord::getLessonId, record.getLessonId())
				.set(LearningRecord::getMoment, record.getMoment())
				.set(firstTimeFinished, LearningRecord::getFinished, true)
				.set(firstTimeFinished, LearningRecord::getFinishTime, record.getCommitTime())
				.update();
		if(!updated) throw new DbException("课程记录更新失败！");
		return firstTimeFinished;
	}

	private void handleExamRecord(LearningRecordFormDTO record) {
		Long userId = UserContext.getUser();
		LearningRecord learningRecord = BeanUtils.copyBean(record, LearningRecord.class);
		learningRecord.setFinishTime(record.getCommitTime());
		learningRecord.setFinished(true);
		learningRecord.setUserId(userId);
		boolean saved = this.save(learningRecord);
		if(!saved) throw new DbException("课程记录更新失败！");
	}

	private void handleRecordUpdate(LearningRecordFormDTO record, boolean finished) {
		LearningLesson learningLesson = learningLessonService.getById(record.getLessonId());
		if(learningLesson == null) throw new BizIllegalException("课程不存在！");
		CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
		if(courseInfo == null) throw new BizIllegalException("课程不存在！");
		boolean allLearned = finished && learningLesson.getLearnedSections() + 1 >= courseInfo.getSectionNum();
		boolean updated = learningLessonService.lambdaUpdate()
				.eq(LearningLesson::getId, record.getLessonId())
				.set(LearningLesson::getLatestLearnTime, record.getCommitTime())
				.set(LearningLesson::getLatestSectionId, record.getSectionId())
				.setSql(finished, "learned_sections = learned_sections + 1")
				//小节全部学完
				.set(allLearned, LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
				//第一次学习
				.set(learningLesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
				.update();
		if(!updated) throw new DbException("课程记录更新失败！");
	}
}
