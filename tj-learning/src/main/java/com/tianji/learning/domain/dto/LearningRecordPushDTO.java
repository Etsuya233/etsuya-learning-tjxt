package com.tianji.learning.domain.dto;

import com.tianji.learning.domain.po.LearningRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用于更新视频学习进度，如播放进度和最后的学习小节等")
public class LearningRecordPushDTO {
	@ApiModelProperty("课程ID")
	private Long lessonId;
	@ApiModelProperty("小节ID")
	private Long sectionId;
	@ApiModelProperty("视频播放进度")
	private Integer moment;
	@ApiModelProperty("是否完成该小节的学习")
	private Boolean finished;

	public static LearningRecordPushDTO of(LearningRecord record){
		LearningRecordPushDTO learningRecordPushDTO = new LearningRecordPushDTO();
		learningRecordPushDTO.setSectionId(record.getSectionId());
		learningRecordPushDTO.setFinished(record.getFinished());
		learningRecordPushDTO.setMoment(record.getMoment());
		learningRecordPushDTO.setLessonId(record.getLessonId());
		return learningRecordPushDTO;
	}
}
