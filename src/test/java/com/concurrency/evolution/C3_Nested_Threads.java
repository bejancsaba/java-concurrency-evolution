package com.concurrency.evolution;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_FORK_FACTOR;
import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.ITERATION;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_A_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_B_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.persistence;
import static com.concurrency.evolution.ConcurrencySupport.serviceA;
import static com.concurrency.evolution.ConcurrencySupport.serviceB;
import static com.concurrency.evolution.ConcurrencySupport.start;
import static com.concurrency.evolution.ConcurrencySupport.stop;

@Slf4j
public class C3_Nested_Threads {
    @Test
    public void shouldExecuteIterationsConcurrently() throws InterruptedException {
        start();

        List<Thread> threads = new ArrayList<>();
        for (int iteration = 1; iteration <= ITERATION; iteration++) {
            Thread thread = new Thread(new Iteration(iteration));
            thread.start();
            threads.add(thread);
        }

        // Not the most optimal but gets the work done
        for (Thread thread : threads) {
            thread.join();
        }

        stop(ITERATION + ITERATION * PERSISTENCE_FORK_FACTOR,
                SERVICE_A_LATENCY + SERVICE_B_LATENCY + PERSISTENCE_LATENCY);
    }

    static class Iteration implements Runnable {

        private final int iteration;

        Iteration(int iteration) {
            this.iteration =iteration;
        }

        @SneakyThrows
        @Override
        public void run() {
            String serviceA = serviceA(iteration);
            String serviceB = serviceB(iteration);

            List<Thread> threads = new ArrayList<>();
            for (int i = 1; i <= PERSISTENCE_FORK_FACTOR; i++) {
                Thread thread = new Thread(new Persistence(i, serviceA, serviceB));
                thread.start();
                threads.add(thread);
            }

            // Stop Condition - Not the most optimal but gets the work done
            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    static class Persistence implements Runnable {

        private final int fork;
        private final String serviceA;
        private final String serviceB;

        Persistence(int fork, String serviceA, String serviceB) {
            this.fork = fork;
            this.serviceA = serviceA;
            this.serviceB = serviceB;
        }

        @Override
        public void run() {
            persistence(fork, serviceA, serviceB);
        }
    }
}
