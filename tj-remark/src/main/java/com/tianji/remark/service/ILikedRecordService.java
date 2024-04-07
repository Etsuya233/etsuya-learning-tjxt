package com.tianji.remark.service;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-05
 */
public interface ILikedRecordService extends IService<LikedRecord> {

	void addLikeRecord(LikeRecordFormDTO dto);

	Set<Long> whetherUserLikedBatch(List<Long> ids);

}
