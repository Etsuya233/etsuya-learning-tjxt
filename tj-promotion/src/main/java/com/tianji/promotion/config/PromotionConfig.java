package com.tianji.promotion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class PromotionConfig {

	/**
	 * 用于生成兑换码的线程池
	 * @return 线程池Executor
	 */
	@Bean
	public Executor generateExchangeCodeExecutor(){
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// 1.核心线程池大小
		executor.setCorePoolSize(2);
		// 2.最大线程池大小
		executor.setMaxPoolSize(5);
		// 3.队列大小
		executor.setQueueCapacity(200);
		// 4.线程名称
		executor.setThreadNamePrefix("exchange-code-handler-");
		// 5.拒绝策略
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	@Bean
	public Executor discountSolutionExecutor(){
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// 1.核心线程池大小
		executor.setCorePoolSize(12);
		// 2.最大线程池大小
		executor.setMaxPoolSize(12);
		// 3.队列大小
		executor.setQueueCapacity(99999);
		// 4.线程名称
		executor.setThreadNamePrefix("discount-solution-calculator-");
		// 5.拒绝策略
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.initialize();
		return executor;
	}

}
