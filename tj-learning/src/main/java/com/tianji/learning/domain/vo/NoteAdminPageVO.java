package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.models.auth.In;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("笔记管理段分页查询展示类")
public class NoteAdminPageVO {
	private Long id;
	private String courseName;
	private String chapterName;
	private String sectionName;
	private String content;
	private Integer usedTime;
	private Boolean hidden;
	private String authorName;
	private LocalDateTime createTime;
}

