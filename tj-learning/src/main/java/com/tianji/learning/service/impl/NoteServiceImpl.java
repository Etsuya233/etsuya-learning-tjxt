package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.Note;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.NoteAdminPageVO;
import com.tianji.learning.domain.vo.NoteDetailAdminVO;
import com.tianji.learning.domain.vo.NotePageVO;
import com.tianji.learning.mapper.NoteMapper;
import com.tianji.learning.service.INoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 笔记表 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-24
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {

	private final UserClient userClient;
	private final SearchClient searchClient;
	private final CourseClient courseClient;
	private final RemarkClient remarkClient;
	private final CatalogueClient catalogueClient;
	private final CategoryClient categoryClient;
	private final CategoryCache categoryCache;

	@Override
	public void addNote(Note note) {
		Long userId = UserContext.getUser();
		note.setUserId(userId);
		note.setAuthorId(userId);
		boolean saved = this.save(note);
		if(!saved){
			throw new BizIllegalException("保存失败！");
		}
	}

	@Override
	public void updateNote(Note note, Long id) {
		if(note == null || id == null){
			throw new BadRequestException("请求参数异常！");
		}
		note.setId(id);
		boolean updated = this.updateById(note);
		if(!updated){
			throw new BizIllegalException("更新失败！");
		}
	}

	@Override
	public void deleteNote(Long id) {
		boolean b = this.removeById(id);
		if(!b){
			throw new BizIllegalException("删除失败！可能是笔记不存在！");
		}
	}

	@Override
	public PageDTO<NotePageVO> pageQuery(NotePageQuery notePageQuery) {
		if(notePageQuery.getOnlyMine()){ //查询我的笔记，包括我采集的笔记
			return pageQueryMyNote(notePageQuery);
		} else { //查询全部笔记，不查询被我采集的笔记，但是要有标识
			return pageQueryAllNote(notePageQuery);
		}
	}

	@Override
	public void gatherNote(Long id) {
		Note original = this.getById(id);
		if(original == null){
			throw new BizIllegalException("原笔记不存在！");
		}
		Long userId = UserContext.getUser();
		Note note = new Note();
		note.setGatheredNoteId(id);
		note.setUserId(userId);
		note.setCourseId(original.getCourseId());
		note.setSectionId(original.getSectionId());
		note.setChapterId(original.getChapterId());
		note.setAuthorId(original.getAuthorId());
		note.setIsGathered(true);
		this.save(note);
	}

	@Override
	public void deleteGatheredNote(Long id) {
		Long userId = UserContext.getUser();
		Note note = this.lambdaQuery()
				.eq(Note::getUserId, userId)
				.eq(Note::getIsGathered, true)
				.eq(Note::getGatheredNoteId, id)
				.one();
		if(note == null){
			return;
		}
		this.removeById(note.getId());
	}

	@Override
	public PageDTO<NoteAdminPageVO> noteAdminPageQuery(NoteAdminPageQuery pageQuery) {
		//查询课程Ids
		List<Long> courseIds = null;
		if(pageQuery.getName() != null){
			courseIds = searchClient.queryCoursesIdByName(pageQuery.getName());
		}
		//搜索
		Page<Note> pageResult = this.lambdaQuery()
				.in(courseIds != null, Note::getCourseId, courseIds)
				.eq(pageQuery.getHidden() != null, Note::getHidden, pageQuery.getHidden())
				.ge(pageQuery.getBeginTime() != null, Note::getCreateTime, pageQuery.getBeginTime())
				.le(pageQuery.getEndTime() != null, Note::getCreateTime, pageQuery.getEndTime())
				.page(pageQuery.toMpPage());
		List<Note> notes = pageResult.getRecords();
		if(CollUtils.isEmpty(notes)){
			return PageDTO.empty(pageResult);
		}
		//课程数据
		List<CourseSimpleInfoDTO> courseInfo = courseClient.getSimpleInfoList(courseIds);
		Map<Long, CourseSimpleInfoDTO> courseMap = courseInfo.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, Function.identity()));
		List<Long> sectionIds = notes.stream().map(Note::getSectionId).collect(Collectors.toList());
		List<Long> chapterIds = notes.stream().map(Note::getChapterId).collect(Collectors.toList());
		sectionIds.addAll(chapterIds);
		List<CataSimpleInfoDTO> cataInfo = catalogueClient.batchQueryCatalogue(sectionIds);
		Map<Long, CataSimpleInfoDTO> cataMap = cataInfo.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, Function.identity()));
		//用户数据
		List<Long> userIds = notes.stream()
				.map(Note::getAuthorId)
				.collect(Collectors.toList());
		List<UserDTO> userDtos = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDtos.stream().collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//封装信息
		List<NoteAdminPageVO> list = notes.stream().map(n -> {
			NoteAdminPageVO vo = BeanUtils.copyBean(n, NoteAdminPageVO.class);
			CourseSimpleInfoDTO course = courseMap.get(n.getCourseId());
			if (course != null) vo.setCourseName(course.getName());
			CataSimpleInfoDTO chapter = cataMap.get(n.getChapterId());
			if (chapter != null) vo.setChapterName(chapter.getName());
			CataSimpleInfoDTO section = cataMap.get(n.getSectionId());
			if (section != null) vo.setSectionName(section.getName());
			UserDTO user = userMap.get(n.getUserId());
			if (user != null) vo.setAuthorName(user.getName());
			//引用次数
			Integer count = this.lambdaQuery()
					.eq(Note::getGatheredNoteId, n.getId())
					.count();
			vo.setUsedTime(count);
			return vo;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}

	@Override
	public NoteDetailAdminVO getNoteDetail(Long id) {
		//查询基本信息
		Note note = this.lambdaQuery()
				.eq(Note::getId, id)
				.one();
		if(note == null){
			throw new BizIllegalException("笔记不存在！");
		}
		NoteDetailAdminVO vo = BeanUtils.copyBean(note, NoteDetailAdminVO.class);
		//查询课程信息
		List<CataSimpleInfoDTO> cataList = catalogueClient.batchQueryCatalogue(List.of(note.getChapterId(), note.getSectionId()));
		if(cataList != null && cataList.size() == 2){
			if(cataList.get(0).getId().equals(note.getChapterId())){
				vo.setChapterName(cataList.get(0).getName());
				vo.setSectionName(cataList.get(1).getName());
			} else {
				vo.setChapterName(cataList.get(1).getName());
				vo.setSectionName(cataList.get(0).getName());
			}
		}
		CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(note.getCourseId(), false, false);
		String categoryNames = categoryCache.getCategoryNames(courseInfo.getCategoryIds());
		vo.setCategoryNames(categoryNames);
		//查询用户信息
		UserDTO user = userClient.queryUserById(note.getAuthorId());
		if(user != null){
			vo.setAuthorName(user.getName());
			vo.setAuthorPhone(user.getCellPhone());
		}
		List<Note> gathers = this.lambdaQuery()
				.eq(Note::getIsGathered, true)
				.eq(Note::getGatheredNoteId, note.getId())
				.list();
		if(gathers != null){
			vo.setUsedTimes(gathers.size());
			List<Long> userIds = gathers.stream()
					.map(Note::getAuthorId)
					.collect(Collectors.toList());
			List<UserDTO> userDtos = userClient.queryUserByIds(userIds);
			List<String> gathersName = userDtos.stream().map(UserDTO::getName).collect(Collectors.toList());
			vo.setGathers(gathersName);
		}
		return vo;
	}

	private PageDTO<NotePageVO> pageQueryMyNote(NotePageQuery notePageQuery) {
		//查询我的笔记列表
		Long userId = UserContext.getUser();
		Page<Note> pageResult = this.lambdaQuery()
				.eq(Note::getUserId, userId)
				.eq(Note::getCourseId, notePageQuery.getCourseId())
				.eq(Note::getSectionId, notePageQuery.getSectionId())
				.page(notePageQuery.toMpPage());
		List<Note> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(pageResult);
		}
		//查询用户信息
		List<Long> userIds = records.stream()
				.map(Note::getAuthorId)
				.collect(Collectors.toList());
		List<UserDTO> userDtos = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDtos.stream().collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//查询采集的帖子
		List<Long> gatheredId = records.stream()
				.filter(Note::getIsGathered)
				.map(Note::getGatheredNoteId)
				.collect(Collectors.toList());
		gatheredId.add(0L);
		List<Note> gatheredNote = this.lambdaQuery()
				.in(Note::getId, gatheredId)
				.list();
		Map<Long, Note> gatheredNoteMap = gatheredNote.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
		//封装信息
		List<NotePageVO> list = records.stream().map(r -> {
			NotePageVO vo = BeanUtils.copyBean(r, NotePageVO.class);
			if (r.getIsGathered()) {
				Note original = gatheredNoteMap.get(r.getGatheredNoteId());
				vo.setContent(original.getContent());
			}
			UserDTO user = userMap.get(r.getAuthorId());
			if(user != null){
				vo.setAuthorName(user.getName());
				vo.setAuthorIcon(user.getIcon());
			}
			return vo;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}

	private PageDTO<NotePageVO> pageQueryAllNote(NotePageQuery notePageQuery) {
		//查询笔记列表
		Page<Note> pageResult = this.lambdaQuery()
				.eq(Note::getCourseId, notePageQuery.getCourseId())
				.eq(Note::getSectionId, notePageQuery.getSectionId())
				.eq(Note::getIsGathered, false)
				.eq(Note::getIsPrivate, false)
				.page(notePageQuery.toMpPage());
		List<Note> records = pageResult.getRecords();
		if(CollUtils.isEmpty(records)){
			return PageDTO.empty(pageResult);
		}
		//查询用户信息
		List<Long> userIds = records.stream()
				.map(Note::getAuthorId)
				.collect(Collectors.toList());
		List<UserDTO> userDtos = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDtos.stream()
				.collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//查询被我采集的帖子
		Long userId = UserContext.getUser();
		List<Long> noteIds = records.stream()
				.map(Note::getId)
				.collect(Collectors.toList());
		List<Note> gatheredNotes = this.lambdaQuery()
				.eq(Note::getUserId, userId)
				.in(Note::getGatheredNoteId, noteIds)
				.eq(Note::getIsGathered, true)
				.list();
		Map<Long, Note> gatheredNoteMap = gatheredNotes.stream()
				.collect(Collectors.toMap(Note::getGatheredNoteId, Function.identity()));
		//封装信息
		List<NotePageVO> list = records.stream().map(r -> {
			NotePageVO vo = BeanUtils.copyBean(r, NotePageVO.class);
			UserDTO user = userMap.get(r.getUserId());
			if (user != null) {
				vo.setAuthorName(user.getName());
				vo.setAuthorIcon(user.getIcon());
			}
			Note note = gatheredNoteMap.get(r.getId());
			vo.setIsGathered(note != null);
			return vo;
		}).collect(Collectors.toList());
		return PageDTO.of(pageResult, list);
	}


}
