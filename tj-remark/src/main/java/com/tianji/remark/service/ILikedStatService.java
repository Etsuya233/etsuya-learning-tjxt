package com.tianji.remark.service;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedStat;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 点赞统计表 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-05
 */
public interface ILikedStatService extends IService<LikedStat> {

	void updateLikedTimesDB(LikeRecordFormDTO dto);

	List<LikedTimesDTO> bizLikedTimesBatch(String bizType, List<Long> bizIds);

	void updateLikedTimes(String bizType, int maxBizSize);
}
