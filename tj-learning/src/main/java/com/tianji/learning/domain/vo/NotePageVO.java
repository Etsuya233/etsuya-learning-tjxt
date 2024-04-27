package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("笔记分页查询展示类")
public class NotePageVO {
	private Long id;
	private String content;
	private Integer noteMoment;
	private Boolean isPrivate;
	private Boolean isGathered;
	private Long authorId;
	private String authorName;
	private String authorIcon;
	private LocalDateTime createTime;
}
