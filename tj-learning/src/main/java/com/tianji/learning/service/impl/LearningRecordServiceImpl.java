package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constant.RedisConstant;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
	private final StringRedisTemplate redisTemplate;
	private final RabbitTemplate rabbitTemplate;
	private final RabbitMqHelper rabbitMqHelper;

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
		boolean firstTimeFinished;
		if(record.getSectionType() == SectionType.VIDEO.getValue()){ //如果是视频
			firstTimeFinished = handleVideoRecord(record);
		} else { //如果是考试
			handleExamRecord(record);
			firstTimeFinished = true;
		}
		//更新一下lesson
		handleRecordUpdate(record, firstTimeFinished);
		//添加积分
		if(firstTimeFinished){
			//添加积分
			rabbitMqHelper.send(
					MqConstants.Exchange.LEARNING_EXCHANGE,
					MqConstants.Key.LEARN_SECTION,
					UserContext.getUser()
			);
		}
	}

	private boolean handleVideoRecord(LearningRecordFormDTO record) {
		Long userId = UserContext.getUser();
		//从redis中读缓存
		Object cacheData = redisTemplate.opsForHash().get(record.getLessonId().toString(), record.getSectionId().toString());
		LearningRecord old;
		if(cacheData == null){
			//如果redis未命中
			old = this.lambdaQuery()
					.eq(LearningRecord::getSectionId, record.getSectionId())
					.eq(LearningRecord::getLessonId, record.getLessonId()).one();
			if(old == null){
				//数据库也没有，那就是第一次提交，先不存Redis（存不存都无所谓）
				LearningRecord learningRecord = BeanUtils.copyBean(record, LearningRecord.class);
				learningRecord.setUserId(userId);
				boolean saved = this.save(learningRecord);
				if(!saved) throw new DbException("添加课程记录失败！");
				return false;
			}
			//存数据到redis
			String jsonStr = JsonUtils.toJsonStr(old);
			String redisKey = RedisConstant.LEARNING_RECORD_PUSH_KEY_PREFIX + record.getLessonId();
			redisTemplate.opsForHash().put(redisKey,
					record.getSectionId().toString(), jsonStr);
		} else {
			//redis命中
			old = JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
		}
		old.setMoment(record.getMoment());
		//判断是否第一次完成（之前未完成且这次观看进度超过50%）
		boolean firstTimeFinished = !old.getFinished() && record.getMoment().doubleValue() / record.getDuration() >= 0.5;
		if(!firstTimeFinished){
			//更新记录到redis
			String jsonStr = JsonUtils.toJsonStr(old);
			redisTemplate.opsForHash().put(record.getLessonId().toString(),
					record.getSectionId().toString(), jsonStr);
			//添加延迟消息
			rabbitTemplate.convertAndSend(MqConstants.Exchange.LEARNING_DELAY_EXCHANGE,
					MqConstants.Key.LEARNING_RECORD_PUSH_KEY,
					old,
					post -> {
						//前端15s提交一次消息，这边在这次的下一次提交时间后检测这次的提交和下一次的提交的观看时间是否一致
						post.getMessageProperties().setDelay(20000);
						return post;
					});
			return false;
		}
		//是第一次学习完该小节：
		//清理缓存，下次读的时候就可以更新redis数据了
		redisTemplate.opsForHash().delete(record.getLessonId().toString(), record.getSectionId().toString());
		//更新数据库
		boolean updated = this.lambdaUpdate()
				.eq(LearningRecord::getLessonId, record.getLessonId())
				.set(LearningRecord::getMoment, record.getMoment())
				.set(LearningRecord::getFinished, true)
				.set(LearningRecord::getFinishTime, record.getCommitTime())
				.update();
		if(!updated) throw new DbException("课程记录更新失败！");
		return true;
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

	private void handleRecordUpdate(LearningRecordFormDTO record, boolean firstTimeFinished) {
		LearningLesson learningLesson = learningLessonService.getById(record.getLessonId());
		if(learningLesson == null) throw new BizIllegalException("课程不存在！");
		CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
		if(courseInfo == null) throw new BizIllegalException("课程不存在！");
		boolean allLearned = firstTimeFinished && learningLesson.getLearnedSections() + 1 >= courseInfo.getSectionNum();
		boolean updated = learningLessonService.lambdaUpdate()
				.eq(LearningLesson::getId, record.getLessonId())
				.set(LearningLesson::getLatestLearnTime, record.getCommitTime())
				.set(LearningLesson::getLatestSectionId, record.getSectionId())
				.setSql(firstTimeFinished, "learned_sections = learned_sections + 1")
				//小节全部学完
				.set(allLearned, LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
				//第一次学习完该小节
				.set(learningLesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
				.update();
		if(!updated) throw new DbException("课程记录更新失败！");
	}
}
