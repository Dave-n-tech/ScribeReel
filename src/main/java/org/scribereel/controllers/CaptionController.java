package org.scribereel.controllers;

import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.dtos.response.CaptionResponse;
import org.scribereel.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/caption")
public class CaptionController {

    private static final Logger log = LoggerFactory.getLogger(CaptionController.class);

    private final VideoValidationService videoValidationService;
    private final FfmpegService ffmpegService;
    private final TranscriptionService transcriptionService;
    private final SubtitleGeneratorService subtitleGeneratorService;
    private final JobFileService jobFileService;

    public CaptionController(
            VideoValidationService videoValidationService,
            FfmpegService ffmpegService,
            TranscriptionService transcriptionService,
            SubtitleGeneratorService subtitleGeneratorService,
            JobFileService jobFileService
    ){
        this.videoValidationService = videoValidationService;
        this.ffmpegService = ffmpegService;
        this.transcriptionService = transcriptionService;
        this.subtitleGeneratorService = subtitleGeneratorService;
        this.jobFileService = jobFileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CaptionResponse> createVideoCaption (
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "wordsPerChunk", defaultValue = "1") int wordsPerChunk
            ) {

        if (wordsPerChunk < 1) {
            throw new IllegalArgumentException("wordsPerChunk must be at least 1");
        }

        // server-side validation
        videoValidationService.validate(video);

        JobFileService.JobContext job = jobFileService.createJob();
        Path inputVideoPath = jobFileService.saveUpload(video, job.jobDir());

        videoValidationService.validateDuration(inputVideoPath);

        Path audioPath = job.jobDir().resolve("audio.mp3");
        Path assPath = job.jobDir().resolve("captions.ass");
        Path outputVideoPath = job.jobDir().resolve("result.mp4");

        ffmpegService.extractAudio(inputVideoPath, audioPath);
        List<TranscriptWord> words = transcriptionService.transcribe(audioPath);
        subtitleGeneratorService.generate(words, assPath, wordsPerChunk);
        ffmpegService.burnSubtitles(inputVideoPath, assPath, outputVideoPath);

        return ResponseEntity.ok(new CaptionResponse("/api/download/" + job.jobId()));
    }
}
