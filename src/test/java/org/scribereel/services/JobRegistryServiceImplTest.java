package org.scribereel.services;

import org.junit.jupiter.api.Test;
import org.scribereel.dtos.internal.JobRecord;
import org.scribereel.enums.JobStatus;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JobRegistryServiceImplTest {

    private final JobRegistryServiceImpl registry = new JobRegistryServiceImpl();

    @Test
    void createPending_startsInPendingStatus() {
        registry.createPending("job-1");

        Optional<JobRecord> record = registry.get("job-1");

        assertThat(record).isPresent();
        assertThat(record.get().status()).isEqualTo(JobStatus.PENDING);
        assertThat(record.get().downloadUrl()).isNull();
    }

    @Test
    void markProcessing_transitionsStatusButKeepsCreatedAt() {
        registry.createPending("job-1");
        var createdAt = registry.get("job-1").get().createdAt();

        registry.markProcessing("job-1");

        JobRecord record = registry.get("job-1").get();
        assertThat(record.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(record.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void markDone_setsDownloadUrl() {
        registry.createPending("job-1");
        registry.markDone("job-1", "/api/download/job-1");

        JobRecord record = registry.get("job-1").get();
        assertThat(record.status()).isEqualTo(JobStatus.DONE);
        assertThat(record.downloadUrl()).isEqualTo("/api/download/job-1");
    }

    @Test
    void markDoneWithText_setsTextNotDownloadUrl() {
        registry.createPending("job-1");
        registry.markDoneWithText("job-1", "hello world");

        JobRecord record = registry.get("job-1").get();
        assertThat(record.status()).isEqualTo(JobStatus.DONE);
        assertThat(record.text()).isEqualTo("hello world");
        assertThat(record.downloadUrl()).isNull();
    }

    @Test
    void markFailed_setsErrorMessage() {
        registry.createPending("job-1");
        registry.markFailed("job-1", "Processing failed.");

        JobRecord record = registry.get("job-1").get();
        assertThat(record.status()).isEqualTo(JobStatus.FAILED);
        assertThat(record.errorMessage()).isEqualTo("Processing failed.");
    }

    @Test
    void get_returnsEmptyForUnknownJobId() {
        assertThat(registry.get("does-not-exist")).isEmpty();
    }

    @Test
    void updatingUnknownJobId_doesNothingSilently() {
        // computeIfPresent means this should be a no-op, not throw
        registry.markProcessing("never-created");
        assertThat(registry.get("never-created")).isEmpty();
    }

    @Test
    void purgeOlderThan_removesExpiredEntriesOnly() throws InterruptedException {
        registry.createPending("old-job");
        Thread.sleep(50); // ensure a measurable age gap
        registry.createPending("new-job");

        // Purge anything older than 25ms - "old-job" should go, "new-job" should survive
        registry.purgeOlderThan(Duration.ofMillis(25));

        assertThat(registry.get("old-job")).isEmpty();
        assertThat(registry.get("new-job")).isPresent();
    }

    @Test
    void concurrentUpdates_toSameJobId_areThreadSafe() throws InterruptedException {
        registry.createPending("job-1");
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                registry.markProcessing("job-1");
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // No exceptions thrown, and the record is still in a valid, consistent state
        assertThat(registry.get("job-1")).isPresent();
        assertThat(registry.get("job-1").get().status()).isEqualTo(JobStatus.PROCESSING);
    }
}