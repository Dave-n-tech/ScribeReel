package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.dtos.internal.JobRecord;
import org.scribereel.enums.JobStatus;
import org.scribereel.services.JobRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest extends BaseControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JobRegistryService jobRegistryService;
    @MockBean private JobStatus jobStatus;

    @Test
    void getStatus_returns404ForUnknownJob() throws Exception {
        when(jobRegistryService.get("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStatus_returnsPendingStatus() throws Exception {
        JobRecord record = new JobRecord("job-1", JobStatus.PENDING, null, null, null, Instant.now());
        when(jobRegistryService.get("job-1")).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.downloadUrl").doesNotExist());
    }

    @Test
    void getStatus_returnsDownloadUrlWhenDone() throws Exception {
        JobRecord record = new JobRecord("job-1", JobStatus.DONE, "/api/download/job-1", null, null, Instant.now());
        when(jobRegistryService.get("job-1")).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/download/job-1"));
    }

    @Test
    void getStatus_returnsTranscriptTextWhenDone() throws Exception {
        JobRecord record = new JobRecord("job-2", JobStatus.DONE, null, "hello world", null, Instant.now());
        when(jobRegistryService.get("job-2")).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/jobs/job-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("hello world"));
    }

    @Test
    void getStatus_returnsErrorMessageWhenFailed() throws Exception {
        JobRecord record = new JobRecord("job-3", JobStatus.FAILED, null, null, "Processing failed.", Instant.now());
        when(jobRegistryService.get("job-3")).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/jobs/job-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("Processing failed."));
    }
}