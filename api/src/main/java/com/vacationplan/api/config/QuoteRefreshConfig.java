package com.vacationplan.api.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuoteRefreshConfig {

	public static final String EXECUTOR = "quoteRefreshExecutor";

	@Bean(name = EXECUTOR)
	public Executor quoteRefreshExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1,
				2,
				60L,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(16),
				runnable -> {
					Thread thread = new Thread(runnable, "quote-refresh");
					thread.setDaemon(true);
					return thread;
				},
				new ThreadPoolExecutor.CallerRunsPolicy());
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}
}
