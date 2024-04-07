package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.dto.remark.LikedTimesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {
	@Override
	public RemarkClient create(Throwable cause) {
		log.error("查询点赞服务异常", cause);
		return new RemarkClient() {
			@Override
			public Set<Long> whetherUserLikedBatch(List<Long> ids) {
				return Set.of();
			}

			@Override
			public List<LikedTimesDTO> bizLikedTimesBatch(String bizType, List<Long> bizIds) {
				return List.of();
			}
		};
	}
}
