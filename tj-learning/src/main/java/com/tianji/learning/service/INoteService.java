package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.Note;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.NoteAdminPageVO;
import com.tianji.learning.domain.vo.NoteDetailAdminVO;
import com.tianji.learning.domain.vo.NotePageVO;

/**
 * <p>
 * 笔记表 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-24
 */
public interface INoteService extends IService<Note> {

	void addNote(Note note);

	void updateNote(Note note, Long id);

	void deleteNote(Long id);

	PageDTO<NotePageVO> pageQuery(NotePageQuery notePageQuery);

	void gatherNote(Long id);

	void deleteGatheredNote(Long id);

	PageDTO<NoteAdminPageVO> noteAdminPageQuery(NoteAdminPageQuery pageQuery);

	NoteDetailAdminVO getNoteDetail(Long id);
}
