package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constant.LearningConstant;
import com.tianji.learning.constant.RedisConstant;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

	private final StringRedisTemplate redisTemplate;
	private final UserClient userClient;

	@Override
	public PointsBoardVO getPointsBoardBySeasonId(PointsBoardQuery pointsBoardQuery) {
		if(pointsBoardQuery == null) throw new BizIllegalException("请求参数错误！");
		//查询其他排名
		if(pointsBoardQuery.getSeason() == 0){ //查询当前赛季
			return getCurrentBoard(pointsBoardQuery);
		} else { //查询其他赛季
			return getBoardHistory(pointsBoardQuery);
		}
	}

	@Override
	public void createPointsBoardTableBySeason(Integer season) {
		getBaseMapper().createPointsBoardTable(LearningConstant.POINTS_BOARD_DB_PREFIX + season);
	}

	private PointsBoardVO getCurrentBoard(PointsBoardQuery pointsBoardQuery) {
		//获取基本信息
		LocalDateTime now = LocalDateTime.now();
		String date = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
		String key = RedisConstant.POINT_BOARDS_KEY_PREFIX + date;
		Long userId = UserContext.getUser();
		//查询排行榜 zrange boards:202404 from end REV
		Set<ZSetOperations.TypedTuple<String>> tuples = getCurrentPointsBoard(key, pointsBoardQuery.getPageNo(), pointsBoardQuery.getPageSize());
		if(tuples == null){
			return new PointsBoardVO();
		}
		//查询排行榜用户信息
		List<Long> userIds = tuples.stream()
				.map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
				.collect(Collectors.toList());
		List<UserDTO> userDtos = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDtos.stream().collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//封装排行榜信息
		int rank = (pointsBoardQuery.getPageNo() - 1) * pointsBoardQuery.getPageSize() + 1;
		List<PointsBoardItemVO> boardList = new ArrayList<>(tuples.size());
		for(ZSetOperations.TypedTuple<String> tuple: tuples){
			String currentUserId = tuple.getValue();
			Double points = tuple.getScore();
			if(currentUserId == null || points == null) continue;
			PointsBoardItemVO itemVo = new PointsBoardItemVO();
			UserDTO userDto = userMap.get(Long.parseLong(currentUserId));
			if(userDto == null) continue;
			itemVo.setPoints(points.intValue());
			itemVo.setName(userDto.getName());
			itemVo.setRank(rank++);
			boardList.add(itemVo);
		}
		//查询我的排名 zrank boards:202404 1
		Long myRank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());
		if(myRank == null) myRank = 0L;
		else myRank++;
		Double myPoints = redisTemplate.opsForZSet().score(key, userId.toString());
		if(myPoints == null) myPoints = 0.0;
		//封装信息
		PointsBoardVO vo = new PointsBoardVO();
		vo.setBoardList(boardList);
		vo.setRank(myRank.intValue());
		vo.setPoints(myPoints.intValue());
		return vo;
	}

	@Override
	public Set<ZSetOperations.TypedTuple<String>> getCurrentPointsBoard(String key, int pageNo, int pageSize) {
		int from = (pageNo - 1) * pageSize;
		return redisTemplate.opsForZSet().reverseRangeWithScores(
				key, from, from + pageSize);
	}

	private PointsBoardVO getBoardHistory(PointsBoardQuery pointsBoardQuery) {
		//基本信息
		String tableName = LearningConstant.POINTS_BOARD_DB_PREFIX + pointsBoardQuery.getSeason();
		Long userId = UserContext.getUser();
		//分页查询排行榜
		Integer from = (pointsBoardQuery.getPageNo() - 1) * pointsBoardQuery.getPageSize();
		Integer limit = pointsBoardQuery.getPageSize();
		List<PointsBoard> list =  getBaseMapper().queryHistoryPointsBoard(tableName, from, limit);
		//查询排行榜用户信息
		List<Long> userIds = list.stream()
				.map(PointsBoard::getUserId)
				.collect(Collectors.toList());
		List<UserDTO> userDtos = userClient.queryUserByIds(userIds);
		Map<Long, UserDTO> userMap = userDtos.stream()
				.collect(Collectors.toMap(UserDTO::getId, Function.identity()));
		//查询我的排名
		PointsBoard my = getBaseMapper().queryHistoryBoardInfoByUserId(tableName, userId);
		//封装数据
		PointsBoardVO vo = new PointsBoardVO();
		vo.setRank(Math.toIntExact(my.getId()));
		vo.setPoints(my.getPoints());
		List<PointsBoardItemVO> boardList = list.stream().map(p -> {
			PointsBoardItemVO itemVO = new PointsBoardItemVO();
			itemVO.setPoints(p.getPoints());
			UserDTO user = userMap.get(p.getUserId());
			if (user != null) {
				itemVO.setName(userMap.get(p.getUserId()).getName());
			}
			itemVO.setRank(Math.toIntExact(p.getId()));
			return itemVO;
		}).collect(Collectors.toList());
		vo.setBoardList(boardList);
		return vo;
	}
}
