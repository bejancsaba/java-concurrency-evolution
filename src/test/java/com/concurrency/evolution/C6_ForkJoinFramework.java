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
import static com.concurrency.evolution.ConcurrencySupport.ITERATION;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_A_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_B_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.persistence;
import static com.concurrency.evolution.ConcurrencySupport.service;
import static com.concurrency.evolution.ConcurrencySupport.start;
import static com.concurrency.evolution.ConcurrencySupport.stop;

@Slf4j
public class C6_ForkJoinFramework {

    private static final ForkJoinPool commonPool = new ForkJoinPool(10);

    @Test
    public void shouldExecuteIterationsConcurrently() throws InterruptedException {
        start();

        commonPool.submit(new IterationRecursiveAction(IntStream.rangeClosed(1, ITERATION)
                .boxed()
                .collect(Collectors.toList())));

        // Stop Condition
        commonPool.shutdown();
        commonPool.awaitTermination(60, TimeUnit.SECONDS);

        stop();
    }

    public static class IterationRecursiveAction extends RecursiveAction {

        private final List<Integer> workload;

        public IterationRecursiveAction(List<Integer> workload) {
            this.workload = workload;
        }

        @Override
        protected void compute() {
            if (workload.size() > 1) {
                commonPool.submit(new IterationRecursiveAction(workload.subList(1, workload.size())));
            }

            int act = workload.get(0);

            ForkJoinTask<String> taskA = commonPool.submit(() -> service("A", SERVICE_A_LATENCY, act));
            ForkJoinTask<String> taskB = commonPool.submit(() -> service("B", SERVICE_B_LATENCY, act));

            for (int i = 1; i <= PERSISTENCE_FORK_FACTOR; i++) {
                int finalI = i;
                commonPool.submit(() -> persistence(finalI, taskA.join(), taskB.join()));
            }
        }
    }
}
