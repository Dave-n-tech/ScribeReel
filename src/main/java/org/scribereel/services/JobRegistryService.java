package org.scribereel.services;

import org.scribereel.dtos.internal.JobRecord;

import java.time.Duration;
import java.util.Optional;

public interface JobRegistryService {
    void createPending(String jobId);
    void markProcessing(String jobId);
    void markDone(String jobId, String downloadUrl);
    void markDoneWithText(String jobId, String text);
    void markFailed(String jobId, String errorMessage);
    Optional<JobRecord> get(String jobId);
    void purgeOlderThan(Duration ttl);
}