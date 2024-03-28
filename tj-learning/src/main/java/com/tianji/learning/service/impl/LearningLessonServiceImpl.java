package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-27
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

	private final CourseClient courseClient;
	private final CatalogueClient catalogueClient;

	@Override
	@Transactional
	public void addUserLesson(Long userId, List<Long> courseIds) {
		//查询课程基本信息
		List<CourseSimpleInfoDTO> infoList = courseClient.getSimpleInfoList(courseIds);
		//创建LearningLesson列表并计算过期时间，因为就过期时间没提供现在
		List<LearningLesson> learningLessons = infoList.stream().map(dto -> {
			Integer validDuration = dto.getValidDuration();
			LearningLesson learningLesson = new LearningLesson();
			//如果课程没有有效期或者有效期为0,则是永久课程，置空
			if (dto.getValidDuration() != null && dto.getValidDuration() > 0)
				learningLesson.setExpireTime(LocalDateTime.now().plusMonths(validDuration));
			learningLesson.setCourseId(dto.getId());
			learningLesson.setUserId(userId);
			return learningLesson;
		}).collect(Collectors.toList());
		saveBatch(learningLessons);
	}

	@Override
	public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
		Long userId = UserContext.getUser();
		//分页查询课程基本信息
		//select * from learning_lesson where user_id = #{userId} order by latest_learn_time limit 0, 5
		Page<LearningLesson> page = this.lambdaQuery()
				.eq(LearningLesson::getUserId, userId)
				.page(query.toMpPage("latest_learn_time", false));
		List<LearningLesson> records = page.getRecords();
		//查询课程信息
		Map<Long, CourseSimpleInfoDTO> info = queryCourseSimpleInfoList(records);
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(page);
		}
		//将LearningLesson转化为LearningLessonVO
		ArrayList<LearningLessonVO> list = new ArrayList<>();
		records.forEach(record -> {
			LearningLessonVO learningLessonVO = BeanUtils.copyBean(record, LearningLessonVO.class);
			CourseSimpleInfoDTO courseInfo = info.get(record.getCourseId());
			learningLessonVO.setCourseName(courseInfo.getName());
			learningLessonVO.setCourseCoverUrl(courseInfo.getCoverUrl());
			learningLessonVO.setSections(courseInfo.getSectionNum()); //课程总节数
			list.add(learningLessonVO);
		});
		return PageDTO.of(page, list);
	}

	@Override
	public LearningLessonVO learningNow() {
		Long userId = UserContext.getUser();
		//获取当前在学习的课程，也就是最后学习的课程
		LearningLesson learningLesson = this.lambdaQuery()
				.eq(LearningLesson::getUserId, userId)
				.eq(LearningLesson::getStatus, LessonStatus.LEARNING)
				.orderByDesc(LearningLesson::getLatestLearnTime)
				.last("limit 1")
				.one();
		if(learningLesson == null) return null;
		LearningLessonVO learningLessonVO = BeanUtils.copyBean(learningLesson, LearningLessonVO.class);
		//获取课程总数
		Integer count = this.lambdaQuery()
				.eq(LearningLesson::getUserId, userId)
				.eq(LearningLesson::getStatus, LessonStatus.LEARNING)
				.count();
		learningLessonVO.setCourseAmount(count);
		//查询课程基本信息（名字，总课程数目）
		CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), true, false);
		if(courseInfo == null) throw new BadRequestException("课程信息不存在！");
		learningLessonVO.setCourseName(courseInfo.getName());
		learningLessonVO.setCourseCoverUrl(courseInfo.getCoverUrl());
		learningLessonVO.setSections(courseInfo.getSectionNum());
		//获取小节
		List<CataSimpleInfoDTO> catalogs = catalogueClient
				.batchQueryCatalogue(CollUtils.singletonList(
						learningLesson.getLatestSectionId()));
		if(CollUtils.isNotEmpty(catalogs)){
			CataSimpleInfoDTO catalogInfo = catalogs.get(0);
			learningLessonVO.setLatestSectionIndex(catalogInfo.getCIndex());
			learningLessonVO.setLatestSectionName(catalogInfo.getName());
		}
		return learningLessonVO;
	}

	@Override
	public void deleteUserLesson(Long userId, Long courseId) {
		boolean remove = this.remove(new QueryWrapper<LearningLesson>()
				.eq("user_id", userId)
				.eq("course_id", courseId));
		if(!remove) log.error("用户{}课程{}删除失败！", userId, courseId);
	}

	@Override
	public Long isLessonValid(Long courseId) {
		Long userId = UserContext.getUser();
		LearningLesson learningLesson = this.lambdaQuery()
				.eq(LearningLesson::getUserId, userId)
				.eq(LearningLesson::getCourseId, courseId).one();
		if(learningLesson == null) throw new BadRequestException("课程信息不存在！");
		if(learningLesson.getStatus().equalsValue(LessonStatus.EXPIRED.getValue())){
			return null;
		} else return learningLesson.getId(); //返回lessonId
	}

	@Override
	public LearningLesson getLessonStatus(Long courseId) {
		Long userId = UserContext.getUser();
		return this.lambdaQuery()
				.eq(LearningLesson::getUserId, userId)
				.eq(LearningLesson::getCourseId, courseId)
				.one();
	}

	@Override
	public Integer countLearningLessonByCourse(Long courseId) {
		return this.lambdaQuery()
				.eq(LearningLesson::getCourseId, courseId)
				.count();
	}

	private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
		List<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toList());
		List<CourseSimpleInfoDTO> infoList = courseClient.getSimpleInfoList(courseIds);
		if(CollUtils.isEmpty(infoList)){
			throw new BadRequestException("课程信息不存在！");
		}
		return infoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
	}
}
