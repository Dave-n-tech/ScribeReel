package org.scribereel.controllers;

import org.scribereel.dtos.response.JobAcceptedResponse;
import org.scribereel.services.JobFileService;
import org.scribereel.services.JobRegistryService;
import org.scribereel.services.TranscriptionProcessingService;
import org.scribereel.services.VideoValidationService;
import org.scribereel.config.AppPropertiesConfig;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/transcribe")
public class TranscriptionController {

    private final VideoValidationService videoValidationService;
    private final JobFileService jobFileService;
    private final TranscriptionProcessingService transcriptionProcessingService;
    private final JobRegistryService jobRegistryService;
    private final AppPropertiesConfig appProperties;

    public TranscriptionController(VideoValidationService videoValidationService,
                                   JobFileService jobFileService,
                                   TranscriptionProcessingService transcriptionProcessingService,
                                   JobRegistryService jobRegistryService,
                                   AppPropertiesConfig appProperties) {
        this.videoValidationService = videoValidationService;
        this.jobFileService = jobFileService;
        this.transcriptionProcessingService = transcriptionProcessingService;
        this.jobRegistryService = jobRegistryService;
        this.appProperties = appProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobAcceptedResponse> transcribe(@RequestParam("file") MultipartFile file) {
        videoValidationService.validateMedia(file);

        JobFileService.JobContext job = jobFileService.createJob();
        Path inputPath = jobFileService.saveUpload(file, job.jobDir());

        videoValidationService.validateDuration(inputPath, appProperties.getMaxDurationSecondsTranscribe());

        jobRegistryService.createPending(job.jobId());
        transcriptionProcessingService.process(job.jobId(), job.jobDir(), inputPath);

        return ResponseEntity.accepted()
                .body(new JobAcceptedResponse(job.jobId(), "/api/jobs/" + job.jobId()));
    }
}