package org.scribereel.controllers;

import org.scribereel.dtos.internal.JobRecord;
import org.scribereel.dtos.response.JobStatusResponse;
import org.scribereel.services.JobRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRegistryService jobRegistryService;

    public JobController(JobRegistryService jobRegistryService) {
        this.jobRegistryService = jobRegistryService;
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobStatusResponse> getStatus(@PathVariable String jobId) {
        return jobRegistryService.get(jobId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private JobStatusResponse toResponse(JobRecord record) {
        return new JobStatusResponse(
                record.status().name(),
                record.downloadUrl(),
                record.text(),
                record.errorMessage()
        );
    }
}