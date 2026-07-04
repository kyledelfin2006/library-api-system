package testAPI;

import api.util.BookIDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BookIDGeneratorTest {

    @BeforeEach
    void resetGenerator() {
        // Reset the ID generator to a known state before each test
        BookIDGenerator.setNextId(0);
    }

    // ===================== BASIC TESTS =====================

    @Test
    void generateNextID_ShouldReturnFourDigitStringStartingFromZero() {
        String id = BookIDGenerator.generateNextID();
        assertEquals("0000", id);
    }

    @Test
    void generateNextID_ShouldIncrementSequentially() {
        assertEquals("0000", BookIDGenerator.generateNextID());
        assertEquals("0001", BookIDGenerator.generateNextID());
        assertEquals("0002", BookIDGenerator.generateNextID());
    }

    @Test
    void setNextId_ShouldChangeStartingPoint() {
        BookIDGenerator.setNextId(10);
        assertEquals("0010", BookIDGenerator.generateNextID());
        assertEquals("0011", BookIDGenerator.generateNextID());
    }

    // ===================== CONCURRENCY / THREAD‑SAFETY TESTS =====================

    @Test
    void generateNextID_ShouldProduceUniqueIDsUnderConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int idsPerThread = 100;
        int totalIds = threadCount * idsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);  // start all threads at once
        Set<String> allIds = new HashSet<>();

        // Collect results in a thread-safe set
        Set<String> synchronizedSet = new HashSet<>();

        // For every threadCount
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();  // wait for the signal
                    for (int j = 0; j < idsPerThread; j++) {
                        String id = BookIDGenerator.generateNextID();
                        synchronized (synchronizedSet) {
                            synchronizedSet.add(id);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Start all threads simultaneously
        latch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(finished, "Tasks did not complete in time");

        // Verify we got exactly the expected number of unique IDs
        assertEquals(totalIds, synchronizedSet.size(),
                "Duplicate IDs generated under concurrent access");
    }

    @Test
    void generateNextID_ShouldNotProduceDuplicatesUnderHeavyLoad() throws InterruptedException {
        int threadCount = 50;
        int idsPerThread = 200;
        int totalIds = threadCount * idsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<String> allIds = new HashSet<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    String id = BookIDGenerator.generateNextID();
                    synchronized (allIds) {
                        allIds.add(id);
                    }
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Tasks did not complete in time");

        assertEquals(totalIds, allIds.size(),
                "Duplicate IDs under heavy concurrent load");
    }

    @Test
    void generateNextID_ShouldBeThreadSafeWithResets() throws InterruptedException {
        // This test resets the generator while threads are running to ensure no corruption
        int threadCount = 10;
        int idsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<String> allIds = new HashSet<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    String id = BookIDGenerator.generateNextID();
                    synchronized (allIds) {
                        allIds.add(id);
                    }
                    // Occasionally reset to a random value (simulate external reset)
                    if (j % 20 == 0) {
                        BookIDGenerator.setNextId(0);  // This will cause overlapping IDs, but we just test no exceptions
                    }
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(finished);

        // We don't check uniqueness here because resets intentionally cause duplicates.
        // We just ensure the test completes without errors.
        // Assert if no Ids were ever generated
        assertFalse(allIds.isEmpty(), "No IDs generated");
    }
}