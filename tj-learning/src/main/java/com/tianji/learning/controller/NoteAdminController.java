package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.vo.NoteAdminPageVO;
import com.tianji.learning.domain.vo.NoteDetailAdminVO;
import com.tianji.learning.service.INoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "管理端笔记相关接口")
@RequiredArgsConstructor
@RequestMapping("/admin/notes")
public class NoteAdminController {

	private final INoteService noteService;

	@ApiOperation("管理端分页查询")
	@GetMapping("/page")
	public PageDTO<NoteAdminPageVO> notePageQuery(NoteAdminPageQuery pageQuery){
		return noteService.noteAdminPageQuery(pageQuery);
	}

	@ApiOperation("管理端查询笔记详情")
	@GetMapping("/{id}")
	public NoteDetailAdminVO getNoteDetail(@PathVariable("id") Long id){
		return noteService.getNoteDetail(id);
	}

}
