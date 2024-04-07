package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import com.tianji.remark.service.ILikedStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikesTimesSync {

	private final static List<String> BIZ_TYPE = List.of("QA", "NOTE");
	private final static int MAX_BIZ_SIZE = 30; //一次性处理的biz个数

	private final ILikedStatService likedStatService;

	/**
	 * 同步点赞数量到db
	 */
	@Scheduled(fixedDelay = 20000)
	public void likeTimesSync(){
		for(String bizType: BIZ_TYPE){
			likedStatService.updateLikedTimes(bizType, MAX_BIZ_SIZE);
		}
	}

}
