package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.po.Note;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.NotePageVO;
import com.tianji.learning.service.INoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 笔记表 前端控制器
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-24
 */
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Api(tags = "笔记相关接口")
public class NoteController {
	private final INoteService noteService;

	@ApiOperation("添加笔记")
	@PostMapping
	public void addNote(@RequestBody Note note) {
		noteService.addNote(note);
	}

	@ApiOperation("采集笔记")
	@PostMapping("/gathers/{id}")
	public void gatherNote(@PathVariable("id") Long id){
		noteService.gatherNote(id);
	}

	@ApiOperation("取消采集笔记")
	@DeleteMapping("/gather/{id}")
	public void deleteGatheredNote(@PathVariable("id") Long id){
		noteService.deleteGatheredNote(id);
	}

	@ApiOperation("更新笔记")
	@PutMapping("/{id}")
	public void updateNote(@RequestBody Note note, @PathVariable Long id){
		noteService.updateNote(note, id);
	}

	@ApiOperation("删除笔记")
	@DeleteMapping("/{id}")
	public void deleteNote(@PathVariable Long id){
		noteService.deleteNote(id);
	}

	@ApiOperation("分页查询笔记")
	@GetMapping("/page")
	private PageDTO<NotePageVO> pageQuery(NotePageQuery notePageQuery) {
		return noteService.pageQuery(notePageQuery);
	}
}
