package com.tianji.learning.task;

import com.tianji.common.utils.CollUtils;
import com.tianji.learning.constant.RedisConstant;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsBoardTask {

	private final IPointsBoardSeasonService pointsBoardSeasonService;

	private final IPointsBoardService pointsBoardService;

	private final StringRedisTemplate redisTemplate;

	@XxlJob("createTableTask")
	public void createPointsBoardTableOfLastSeason(){
		log.info("正在创建排行榜数据库表。");
		// 1.获取上月时间
		LocalDateTime time = LocalDateTime.now().minusMonths(1);
		// 2.查询赛季id
		// 这里查询points_board_season表，查询要创建的赛季是否存在。因为需要用points_board_season中的id来拼接表名。
		// 注意：这里创建的是上个月的数据库表而不是这个月的！
		Integer season = pointsBoardSeasonService.querySeasonByTime(time);
		if (season == null) {
			return;
		}
		// 3.创建表
		pointsBoardService.createPointsBoardTableBySeason(season);
	}

	@XxlJob("savePointsBoard2DB")
	public void pointsBoardCreateTableOfLastSeason(){ //TODO 应该实现XXLJOB分片
		//获取上月时间
		LocalDateTime time = LocalDateTime.now().minusMonths(1);
		String date = time.format(DateTimeFormatter.ofPattern("yyyyMM"));
		//将表名存入TableInfoContext
		TableInfoContext.setInfo(date);
		//查询上个月的榜单
		String key = RedisConstant.POINT_BOARDS_KEY_PREFIX + date;
		int pageNo = 1, pageSize = 100;
		long rank = 1;
		while(true){
			//分页查询
			Set<ZSetOperations.TypedTuple<String>> tuples = pointsBoardService.getCurrentPointsBoard(key, pageNo, pageSize++);
			if(tuples == null) break;
			//插入数据据库表
			if(CollUtils.isEmpty(tuples)) break;
			List<PointsBoard> list = new ArrayList<>(tuples.size());
			for(ZSetOperations.TypedTuple<String> tuple : tuples){
				Double points = tuple.getScore();
				String userId = tuple.getValue();
				rank++;
				if(points == null || userId == null) continue;
				PointsBoard pointsBoard = new PointsBoard();
				pointsBoard.setPoints(points.intValue());
				pointsBoard.setUserId(Long.parseLong(userId));
				pointsBoard.setId(rank++);
			}
			pointsBoardService.saveBatch(list);
		}
		TableInfoContext.remove();
	}

	@XxlJob("clearPointsBoardFromRedis")
	public void clearPointsBoardFromRedis(){
		// 1.获取上月时间
		LocalDateTime time = LocalDateTime.now().minusMonths(1);
		String date = time.format(DateTimeFormatter.ofPattern("yyyyMM"));
		// 2.计算key
		String key = RedisConstant.POINT_BOARDS_KEY_PREFIX + date;
		// 3.删除
		redisTemplate.unlink(key);
	}
}
