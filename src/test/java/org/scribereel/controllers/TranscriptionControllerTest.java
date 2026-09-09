package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.exceptions.VideoValidationException;
import org.scribereel.services.JobFileService;
import org.scribereel.services.JobRegistryService;
import org.scribereel.services.TranscriptionProcessingService;
import org.scribereel.services.VideoValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoValidationService videoValidationService;

    @MockBean
    private JobFileService jobFileService;

    @MockBean
    private TranscriptionProcessingService transcriptionProcessingService;

    @MockBean
    private JobRegistryService jobRegistryService;

    @Test
    void transcribe_passesConfiguredTranscribeDurationLimitToValidation() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(appProperties.getMaxDurationSecondsTranscribe()).thenReturn(1800);

        MockMultipartFile video = new MockMultipartFile("file", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(video))
                .andExpect(status().isAccepted());

        verify(videoValidationService, times(1)).validateDuration(inputPath, 1800);
    }

    @Test
    void transcribe_returns202WithJobIdOnAccept() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);

        MockMultipartFile video = new MockMultipartFile("file", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(video))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("fake-job-id"))
                .andExpect(jsonPath("$.statusUrl").value("/api/jobs/fake-job-id"));

        verify(jobRegistryService, times(1)).createPending("fake-job-id");
        verify(transcriptionProcessingService, times(1))
                .process(eq("fake-job-id"), eq(jobDir), eq(inputPath));
    }

    @Test
    void transcribe_validatesBeforeAcceptingJob() throws Exception {
        doThrow(new VideoValidationException("Only video (.mp4, .mov) or audio (.mp3, .wav, .m4a, .aac) files are supported."))
                .when(videoValidationService).validateMedia(any());

        MockMultipartFile file = new MockMultipartFile("file", "clip.ogg", "audio/ogg", "bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(file))
                .andExpect(status().isBadRequest());

        verify(jobRegistryService, never()).createPending(any());
        verify(transcriptionProcessingService, never()).process(any(), any(), any());
    }

    @Test
    void transcribe_enforcesTranscriptionDurationLimit() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        doThrow(new VideoValidationException("Media is 2000s - max allowed is 1800s."))
                .when(videoValidationService).validateDuration(eq(inputPath), anyInt());

        MockMultipartFile video = new MockMultipartFile("file", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(video))
                .andExpect(status().isBadRequest());

        verify(transcriptionProcessingService, never()).process(any(), any(), any());
    }
}