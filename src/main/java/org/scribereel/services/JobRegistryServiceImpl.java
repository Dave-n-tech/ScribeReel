package org.scribereel.services;

import org.scribereel.dtos.internal.JobRecord;
import org.scribereel.enums.JobStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobRegistryServiceImpl implements JobRegistryService {

    private final ConcurrentHashMap<String, JobRecord> jobs = new ConcurrentHashMap<>();

    @Override
    public void createPending(String jobId) {
        jobs.put(jobId, new JobRecord(jobId, JobStatus.PENDING, null, null, null, Instant.now()));
    }

    @Override
    public void markProcessing(String jobId) {
        update(jobId, r -> new JobRecord(jobId, JobStatus.PROCESSING, null, null, null, r.createdAt()));
    }

    @Override
    public void markDone(String jobId, String downloadUrl) {
        update(jobId, r -> new JobRecord(jobId, JobStatus.DONE, downloadUrl, null, null, r.createdAt()));
    }

    @Override
    public void markDoneWithText(String jobId, String text) {
        update(jobId, r -> new JobRecord(jobId, JobStatus.DONE, null, text, null, r.createdAt()));
    }

    @Override
    public void markFailed(String jobId, String errorMessage) {
        update(jobId, r -> new JobRecord(jobId, JobStatus.FAILED, null, null, errorMessage, r.createdAt()));
    }

    @Override
    public Optional<JobRecord> get(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public void purgeOlderThan(Duration ttl) {
        Instant cutoff = Instant.now().minus(ttl);
        jobs.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }

    private void update(String jobId, java.util.function.Function<JobRecord, JobRecord> updater) {
        jobs.computeIfPresent(jobId, (id, existing) -> updater.apply(existing));
    }
}