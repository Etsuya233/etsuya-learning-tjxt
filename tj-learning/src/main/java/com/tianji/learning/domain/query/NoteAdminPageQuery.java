package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "笔记分页查询条件")
public class NoteAdminPageQuery extends PageQuery {
	private Boolean hidden;
	private String name;
	private LocalDateTime beginTime;
	private LocalDateTime endTime;
}
