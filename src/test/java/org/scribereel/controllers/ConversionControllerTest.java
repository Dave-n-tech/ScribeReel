package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.exceptions.VideoValidationException;
import org.scribereel.services.ConvertProcessingService;
import org.scribereel.services.JobFileService;
import org.scribereel.services.JobRegistryService;
import org.scribereel.services.VideoValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoValidationService videoValidationService;

    @MockBean
    private JobFileService jobFileService;

    @MockBean
    private ConvertProcessingService convertProcessingService;

    @MockBean
    private JobRegistryService jobRegistryService;

    @Test
    void convertToMp3_passesConfiguredConvertDurationLimitToValidation() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(appProperties.getMaxDurationSecondsConvert()).thenReturn(180);

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/convert").file(video))
                .andExpect(status().isAccepted());

        verify(videoValidationService, times(1)).validateDuration(inputPath, 180);
    }

    @Test
    void convertToMp3_returns202WithJobIdOnAccept() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/convert").file(video))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("fake-job-id"))
                .andExpect(jsonPath("$.statusUrl").value("/api/jobs/fake-job-id"));

        verify(jobRegistryService, times(1)).createPending("fake-job-id");
        verify(convertProcessingService, times(1))
                .process(eq("fake-job-id"), eq(jobDir), eq(inputPath));
    }

    @Test
    void convertToMp3_validatesBeforeAcceptingJob() throws Exception {
        doThrow(new VideoValidationException("Only .mp4 and .mov files are supported."))
                .when(videoValidationService).validate(any());

        MockMultipartFile file = new MockMultipartFile("video", "clip.avi", "video/avi", "bytes".getBytes());

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only .mp4 and .mov files are supported."));

        verify(jobRegistryService, never()).createPending(any());
        verify(convertProcessingService, never()).process(any(), any(), any());
    }

    @Test
    void convertToMp3_enforcesConvertDurationLimit() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        doThrow(new VideoValidationException("Media is 200s - max allowed is 180s."))
                .when(videoValidationService).validateDuration(eq(inputPath), anyInt());

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/convert").file(video))
                .andExpect(status().isBadRequest());

        verify(convertProcessingService, never()).process(any(), any(), any());
    }
}