package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.Note;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 笔记表 Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-24
 */
public interface NoteMapper extends BaseMapper<Note> {
	List<Note> adminPageQuery(@Param("hidden") Boolean hidden,
							   @Param("ids") List<Long> ids,
							   @Param("beginTime") LocalDateTime beginTime,
							   @Param("endTime") LocalDateTime endTime);
}
