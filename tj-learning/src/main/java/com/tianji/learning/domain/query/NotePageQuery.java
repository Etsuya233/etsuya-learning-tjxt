package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "笔记分页查询条件")
public class NotePageQuery extends PageQuery {
	private Long courseId;
	private Long sectionId;
	private Boolean onlyMine;
}
