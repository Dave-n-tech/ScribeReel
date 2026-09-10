package org.scribereel.services;

import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.enums.CaptionStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class CaptionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(CaptionProcessingService.class);

    private final FfmpegService ffmpegService;
    private final TranscriptionService transcriptionService;
    private final SubtitleGeneratorService subtitleGeneratorService;
    private final JobRegistryService jobRegistryService;

    public CaptionProcessingService(FfmpegService ffmpegService,
                                    TranscriptionService transcriptionService,
                                    SubtitleGeneratorService subtitleGeneratorService,
                                    JobRegistryService jobRegistryService) {
        this.ffmpegService = ffmpegService;
        this.transcriptionService = transcriptionService;
        this.subtitleGeneratorService = subtitleGeneratorService;
        this.jobRegistryService = jobRegistryService;
    }

    @Async("videoProcessingExecutor")
    public void process(String jobId, Path jobDir, Path inputVideoPath, CaptionStyle style) {
        jobRegistryService.markProcessing(jobId);
        try {
            Path audioPath = jobDir.resolve("audio.mp3");
            Path assPath = jobDir.resolve("captions.ass");
            Path outputVideoPath = jobDir.resolve("result.mp4");

            ffmpegService.extractAudioForTranscription(inputVideoPath, audioPath);
            List<TranscriptWord> words = transcriptionService.transcribe(audioPath);
            subtitleGeneratorService.generate(words, assPath, style);
            ffmpegService.burnSubtitles(inputVideoPath, assPath, outputVideoPath);

            jobRegistryService.markDone(jobId, "/api/download/" + jobId);
        } catch (Exception e) {
            log.error("Caption job {} failed", jobId, e);
            jobRegistryService.markFailed(jobId, "Processing failed. Please try again.");
        }
    }
}