package org.scribereel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class ConvertProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ConvertProcessingService.class);

    private final FfmpegService ffmpegService;
    private final JobRegistryService jobRegistryService;

    public ConvertProcessingService(FfmpegService ffmpegService, JobRegistryService jobRegistryService) {
        this.ffmpegService = ffmpegService;
        this.jobRegistryService = jobRegistryService;
    }

    @Async("videoProcessingExecutor")
    public void process(String jobId, Path jobDir, Path inputVideoPath) {
        jobRegistryService.markProcessing(jobId);
        try {
            Path resultAudioPath = jobDir.resolve("result.mp3");
            ffmpegService.extractAudio(inputVideoPath, resultAudioPath);

            jobRegistryService.markDone(jobId, "/api/download/" + jobId);
        } catch (Exception e) {
            log.error("Convert job {} failed", jobId, e);
            jobRegistryService.markFailed(jobId, "Conversion failed. Please try again.");
        }
    }
}