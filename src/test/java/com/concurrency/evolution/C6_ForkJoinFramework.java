package com.concurrency.evolution;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_FORK_FACTOR;
import static com.concurrency.evolution.ConcurrencySupport.USERS;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_A_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_B_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.persistence;
import static com.concurrency.evolution.ConcurrencySupport.service;
import static com.concurrency.evolution.ConcurrencySupport.start;
import static com.concurrency.evolution.ConcurrencySupport.stop;

@Slf4j
public class C6_ForkJoinFramework {

    private static final ForkJoinPool commonPool = new ForkJoinPool(2000);

    @Test
    public void shouldExecuteIterationsConcurrently() throws InterruptedException {
        start();

        commonPool.submit(new UserFlowRecursiveAction(IntStream.rangeClosed(1, USERS)
                .boxed()
                .collect(Collectors.toList())));

        // Stop Condition
        commonPool.shutdown();
        commonPool.awaitTermination(60, TimeUnit.SECONDS);

        stop();
    }

    public static class UserFlowRecursiveAction extends RecursiveAction {

        private final List<Integer> workload;

        public UserFlowRecursiveAction(List<Integer> workload) {
            this.workload = workload;
        }

        @Override
        protected void compute() {
            if (workload.size() > 1) {
                commonPool.submit(new UserFlowRecursiveAction(workload.subList(1, workload.size())));
            }

            int user = workload.get(0);

            ForkJoinTask<String> taskA = commonPool.submit(() -> service("A", SERVICE_A_LATENCY, user));
            ForkJoinTask<String> taskB = commonPool.submit(() -> service("B", SERVICE_B_LATENCY, user));

            IntStream.rangeClosed(1, PERSISTENCE_FORK_FACTOR)
                    .forEach(i -> commonPool.submit(() -> persistence(i, taskA.join(), taskB.join())));
        }
    }
}
