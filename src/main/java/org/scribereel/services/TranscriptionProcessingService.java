package org.scribereel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class TranscriptionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionProcessingService.class);

    private final FfmpegService ffmpegService;
    private final TranscriptionService transcriptionService;
    private final VideoValidationService videoValidationService;
    private final JobRegistryService jobRegistryService;

    public TranscriptionProcessingService(FfmpegService ffmpegService,
                                          TranscriptionService transcriptionService,
                                          VideoValidationService videoValidationService,
                                          JobRegistryService jobRegistryService) {
        this.ffmpegService = ffmpegService;
        this.transcriptionService = transcriptionService;
        this.videoValidationService = videoValidationService;
        this.jobRegistryService = jobRegistryService;
    }

    @Async("videoProcessingExecutor")
    public void process(String jobId, Path jobDir, Path inputPath) {
        jobRegistryService.markProcessing(jobId);
        try {
            Path audioPath;
            if (videoValidationService.isAudioFile(inputPath.getFileName().toString())) {
                audioPath = inputPath;
            } else {
                audioPath = jobDir.resolve("audio.mp3");
                ffmpegService.extractAudio(inputPath, audioPath);
            }

            String text = transcriptionService.transcribeText(audioPath);
            jobRegistryService.markDoneWithText(jobId, text);
        } catch (Exception e) {
            log.error("Transcription job {} failed", jobId, e);
            jobRegistryService.markFailed(jobId, "Transcription failed. Please try again.");
        }
    }
}