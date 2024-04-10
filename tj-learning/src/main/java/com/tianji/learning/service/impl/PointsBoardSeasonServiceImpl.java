package com.tianji.learning.service.impl;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

	@Override
	public List<PointsBoardSeason> querySeasonList() {
		return this.lambdaQuery().list();
	}

	@Override
	public Integer querySeasonByTime(LocalDateTime time) {
		Optional<PointsBoardSeason> season = this.lambdaQuery()
				.le(PointsBoardSeason::getBeginTime, time)
				.gt(PointsBoardSeason::getEndTime, time)
				.oneOpt();
		return season.map(PointsBoardSeason::getId).orElse(null);
	}
}
