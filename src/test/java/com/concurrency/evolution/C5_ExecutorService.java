package com.concurrency.evolution;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_FORK_FACTOR;
import static com.concurrency.evolution.ConcurrencySupport.USERS;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_A_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_B_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.persistence;
import static com.concurrency.evolution.ConcurrencySupport.service;
import static com.concurrency.evolution.ConcurrencySupport.start;
import static com.concurrency.evolution.ConcurrencySupport.stop;

public class C5_ExecutorService {

    private static final int EXECUTOR_THREAD_COUNT = 2000;
    private static final ExecutorService executor = Executors.newScheduledThreadPool(EXECUTOR_THREAD_COUNT);
    private static final CountDownLatch latch = new CountDownLatch(USERS);

    @Test
    public void shouldExecuteIterationsConcurrently() throws InterruptedException {
        start();

        for (int user = 1; user <= USERS; user++) {
            executor.execute(new UserFlow(user));
        }

        // Stop Condition
        latch.await();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        stop();
    }

    static class UserFlow implements Runnable {

        private final int user;

        UserFlow(int user) {
            this.user = user;
        }

        @SneakyThrows
        @Override
        public void run() {
            Future<String> serviceA = executor.submit(new Service("A", SERVICE_A_LATENCY, user));
            Future<String> serviceB = executor.submit(new Service("B", SERVICE_B_LATENCY, user));

            for (int i = 1; i <= PERSISTENCE_FORK_FACTOR; i++) {
                executor.execute(new Persistence(i, serviceA.get(), serviceB.get()));
            }

            latch.countDown();
        }
    }

    static class Service implements Callable<String> {

        private final String name;
        private final long latency;
        private final int iteration;

        Service(String name, long latency, int iteration) {
            this.name = name;
            this.latency = latency;
            this.iteration = iteration;
        }

        @Override
        public String call() {
            return service(name, latency, iteration);
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
