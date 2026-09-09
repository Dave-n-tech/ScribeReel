package org.scribereel.controllers;

import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.dtos.response.JobAcceptedResponse;
import org.scribereel.enums.CaptionStyle;
import org.scribereel.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/caption")
public class CaptionController {

    private static final Logger log = LoggerFactory.getLogger(CaptionController.class);

    private final VideoValidationService videoValidationService;
    private final JobFileService jobFileService;
    private final AppPropertiesConfig appProperties;
    private final JobRegistryService jobRegistryService;
    private final CaptionProcessingService captionProcessingService;

    public CaptionController(
            VideoValidationService videoValidationService,
            JobFileService jobFileService,
            AppPropertiesConfig appProperties,
            JobRegistryService jobRegistryService,
            CaptionProcessingService captionProcessingService
    ){
        this.videoValidationService = videoValidationService;
        this.jobFileService = jobFileService;
        this.appProperties = appProperties;
        this.jobRegistryService = jobRegistryService;
        this.captionProcessingService = captionProcessingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobAcceptedResponse> createVideoCaption (
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "style", defaultValue = "PUNCH") String styleId
            ) {

        CaptionStyle style;
        try {
            style = CaptionStyle.valueOf(styleId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown style: " + styleId);
        }

        // server-side validation
        videoValidationService.validate(video);

        JobFileService.JobContext job = jobFileService.createJob();
        Path inputVideoPath = jobFileService.saveUpload(video, job.jobDir());

        videoValidationService.validateDuration(inputVideoPath, appProperties.getMaxDurationSecondsCaption());

        jobRegistryService.createPending(job.jobId());
        captionProcessingService.process(job.jobId(), job.jobDir(), inputVideoPath, style);

        return ResponseEntity.accepted()
                .body(new JobAcceptedResponse(job.jobId(), "/api/jobs/" + job.jobId()));
    }
}
