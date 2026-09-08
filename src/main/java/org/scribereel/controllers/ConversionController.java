// ConversionController.java
package org.scribereel.controllers;

import org.scribereel.dtos.response.CaptionResponse;
import org.scribereel.services.FfmpegService;
import org.scribereel.services.JobFileService;
import org.scribereel.services.VideoValidationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/convert")
public class ConversionController {

    private final VideoValidationService videoValidationService;
    private final FfmpegService ffmpegService;
    private final JobFileService jobFileService;

    public ConversionController(VideoValidationService videoValidationService,
                                FfmpegService ffmpegService,
                                JobFileService jobFileService) {
        this.videoValidationService = videoValidationService;
        this.ffmpegService = ffmpegService;
        this.jobFileService = jobFileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CaptionResponse> convertToMp3(@RequestParam("video") MultipartFile video) {
        videoValidationService.validate(video);

        JobFileService.JobContext job = jobFileService.createJob();
        Path inputVideoPath = jobFileService.saveUpload(video, job.jobDir());

        videoValidationService.validateDuration(inputVideoPath);

        Path resultAudioPath = job.jobDir().resolve("result.mp3");
        ffmpegService.extractAudio(inputVideoPath, resultAudioPath);

        return ResponseEntity.ok(new CaptionResponse("/api/download/" + job.jobId()));
    }
}