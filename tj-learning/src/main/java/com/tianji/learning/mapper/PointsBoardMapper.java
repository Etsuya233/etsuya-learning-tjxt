package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

	void createPointsBoardTable(@Param("tableName") String tableName);

	List<PointsBoard> queryHistoryPointsBoard(
			@Param("tableName") String tableName,
			@Param("from") Integer from,
			@Param("limit") Integer limit);

	PointsBoard queryHistoryBoardInfoByUserId(
			@Param("tableName") String tableName,
			@Param("userId") Long userId);
}
