package org.scribereel.controllers;

import org.scribereel.dtos.response.TranscriptionResponse;
import org.scribereel.services.FfmpegService;
import org.scribereel.services.JobFileService;
import org.scribereel.services.TranscriptionService;
import org.scribereel.services.VideoValidationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/transcribe")
public class TranscriptionController {

    private final VideoValidationService videoValidationService;
    private final FfmpegService ffmpegService;
    private final TranscriptionService transcriptionService;
    private final JobFileService jobFileService;

    public TranscriptionController(VideoValidationService videoValidationService,
                                   FfmpegService ffmpegService,
                                   TranscriptionService transcriptionService,
                                   JobFileService jobFileService) {
        this.videoValidationService = videoValidationService;
        this.ffmpegService = ffmpegService;
        this.transcriptionService = transcriptionService;
        this.jobFileService = jobFileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionResponse> transcribe(@RequestParam("file") MultipartFile file) {
        videoValidationService.validateMedia(file);

        JobFileService.JobContext job = jobFileService.createJob();
        Path inputPath = jobFileService.saveUpload(file, job.jobDir());

        videoValidationService.validateDuration(inputPath);

        Path audioPath;
        if (videoValidationService.isAudioFile(file.getOriginalFilename())) {
            // Already audio - no extraction needed, transcribe the upload directly.
            audioPath = inputPath;
        } else {
            audioPath = job.jobDir().resolve("audio.mp3");
            ffmpegService.extractAudio(inputPath, audioPath);
        }

        String text = transcriptionService.transcribeText(audioPath);

        // No downloadable deliverable - text returned directly. CleanupService
        // still sweeps the job dir on its normal schedule.
        return ResponseEntity.ok(new TranscriptionResponse(text));
    }
}