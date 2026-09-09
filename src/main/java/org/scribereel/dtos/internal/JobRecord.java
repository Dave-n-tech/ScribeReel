package org.scribereel.dtos.internal;

import org.scribereel.enums.JobStatus;
import java.time.Instant;

/**
 * Immutable snapshot of a job's state. Updates replace the whole record
 * in the registry map rather than mutating fields, so reads are always
 * consistent without needing explicit locking.
 */
public record JobRecord(
        String jobId,
        JobStatus status,
        String downloadUrl,
        String text,
        String errorMessage,
        Instant createdAt
) {}