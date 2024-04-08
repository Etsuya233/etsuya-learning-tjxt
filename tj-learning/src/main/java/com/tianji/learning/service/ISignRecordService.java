package com.tianji.learning.service;

import com.tianji.learning.domain.po.SignRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

/**
 * <p>
 * 签到记录表 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
public interface ISignRecordService extends IService<SignRecord> {

	SignResultVO addSignRecord();

	List<Integer> querySignRecord();

}
