package com.tianji.learning.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteDetailAdminVO {
	private Long id;
	private String courseName;
	private String chapterName;
	private String sectionName;
	private String categoryNames;
	private String content;
	private Integer noteMoment;
	private Boolean hidden;
	private Integer usedTimes;
	private String authorName;
	private String authorPhone;
	private LocalDateTime createTime;
	private List<String> gathers;
}
