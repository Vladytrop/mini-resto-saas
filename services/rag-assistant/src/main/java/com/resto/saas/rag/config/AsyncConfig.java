package com.resto.saas.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Отдельный пул потоков для асинхронного аудит-логирования RAG-запросов.
 *
 * Почему свой executor ("ragLogExecutor"), а не дефолтный SimpleAsyncTaskExecutor:
 * 1) Дефолтный в Spring 6 создаёт НОВЫЙ поток на каждую задачу без переиспользования —
 *    при 100 RAG-запросов/сек получим 100 потоков/сек (каждый ~1 МБ стека). Оуф.
 * 2) Изоляция (bulkhead): если PostgreSQL начнёт тормозить на INSERT логов, очереди
 *    будут расти только в этом маленьком пуле — общие task-пулы не пострадают.
 * 3) Намеренно маленький и с CallerRunsPolicy: при переполнении очереди лучше
 *    выполнить задачу в потоке вызывающего (чуть замедлимся), чем молча потерять
 *    запись аудита (AbortPolicy) или копить неограниченную очередь (OOM).
 */
@Configuration
public class AsyncConfig {

    @Bean("ragLogExecutor")
    public Executor ragLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);            // достаточно: INSERT в лог занимает ~5 мс
        executor.setMaxPoolSize(4);             // потолок на всплески трафика
        executor.setQueueCapacity(200);         // ~2 сек буфера при 100 rps
        executor.setThreadNamePrefix("rag-log-"); // видно в логах/трассировках — отлаживать приятно
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true); // дописать логи при graceful shutdown пода
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}