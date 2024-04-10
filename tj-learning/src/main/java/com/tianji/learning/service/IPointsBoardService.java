package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
public interface IPointsBoardService extends IService<PointsBoard> {

	PointsBoardVO getPointsBoardBySeasonId(PointsBoardQuery pointsBoardQuery);

	void createPointsBoardTableBySeason(Integer season);

	Set<ZSetOperations.TypedTuple<String>> getCurrentPointsBoard(String key, int pageNo, int pageSize);
}
