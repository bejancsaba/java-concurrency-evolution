package com.concurrency.evolution;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_FORK_FACTOR;
import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.USERS;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_A_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_B_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.persistence;
import static com.concurrency.evolution.ConcurrencySupport.serviceA;
import static com.concurrency.evolution.ConcurrencySupport.serviceB;
import static com.concurrency.evolution.ConcurrencySupport.start;
import static com.concurrency.evolution.ConcurrencySupport.stop;

@Slf4j
public class C2_Threads {

    @Test
    public void shouldExecuteIterationsConcurrently() throws InterruptedException {
        start();

        List<Thread> threads = new ArrayList<>();
        for (int user = 1; user <= USERS; user++) {
            Thread thread = new Thread(new UserFlow(user));
            thread.start();
            threads.add(thread);
        }

        // Stop Condition - Not the most optimal but gets the work done
        for (Thread thread : threads) {
            thread.join();
        }

        stop(USERS, SERVICE_A_LATENCY + SERVICE_B_LATENCY + PERSISTENCE_LATENCY * PERSISTENCE_FORK_FACTOR);
    }

    static class UserFlow implements Runnable {

        private final int user;

        UserFlow(int user) {
            this.user = user;
        }

        @Override
        public void run() {
            String serviceA = serviceA(user);
            String serviceB = serviceB(user);
            for (int i = 1; i <= PERSISTENCE_FORK_FACTOR; i++) {
                persistence(i, serviceA, serviceB);
            }
        }
    }
}