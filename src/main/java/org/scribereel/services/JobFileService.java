package org.scribereel.services;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface JobFileService {
    /** Creates a fresh UUID-named job directory under the configured temp dir. */
    JobContext createJob();

    /** Saves the upload into the job dir as "input.<ext>", returning the saved path. */
    Path saveUpload(MultipartFile file, Path jobDir);

    record JobContext(String jobId, Path jobDir) {}
}