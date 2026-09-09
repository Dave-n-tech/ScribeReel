package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.enums.CaptionStyle;
import org.scribereel.exceptions.VideoValidationException;
import org.scribereel.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaptionController.class)
class CaptionControllerTest extends BaseControllerTest{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoValidationService videoValidationService;

    @MockBean
    private FfmpegService ffmpegService;

    @MockBean
    private TranscriptionService transcriptionService;

    @MockBean
    private SubtitleGeneratorService subtitleGeneratorService;

    @MockBean
    private JobFileService jobFileService;

    @MockBean
    private CaptionProcessingService captionProcessingService;
    @MockBean
    private JobRegistryService jobRegistryService;

    @Test
    void createVideoCaption_passesConfiguredCaptionDurationLimitToValidation() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(appProperties.getMaxDurationSecondsCaption()).thenReturn(180);

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video))
                .andExpect(status().isAccepted());

        // The real point of this test: confirms CaptionController reads its OWN
        // duration limit, not one copy-pasted from Convert or Transcribe.
        verify(videoValidationService, times(1)).validateDuration(inputPath, 180);
    }

    @Test
    void createVideoCaption_returns202WithJobIdOnAccept() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("fake-job-id"))
                .andExpect(jsonPath("$.statusUrl").value("/api/jobs/fake-job-id"));

        // Confirms the job was registered before processing kicked off,
        // and that processing was actually dispatched (not skipped).
        verify(jobRegistryService, times(1)).createPending("fake-job-id");
        verify(captionProcessingService, times(1))
                .process(eq("fake-job-id"), eq(jobDir), eq(inputPath), eq(CaptionStyle.PUNCH));
    }

    @Test
    void createVideoCaption_validatesBeforeAcceptingJob() throws Exception {
        doThrow(new VideoValidationException("Only .mp4 and .mov files are supported."))
                .when(videoValidationService).validate(any());

        MockMultipartFile file = new MockMultipartFile("video", "clip.avi", "video/avi", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only .mp4 and .mov files are supported."));

        // Nothing should have been dispatched - validation failure short-circuits everything
        verify(jobRegistryService, never()).createPending(any());
        verify(captionProcessingService, never()).process(any(), any(), any(), any());
    }

    @Test
    void createVideoCaption_passesThroughStyle() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption")
                        .file(video)
                        .param("style", "NEON"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("fake-job-id"));

        verify(captionProcessingService, times(1))
                .process(eq("fake-job-id"), eq(jobDir), eq(inputPath), eq(CaptionStyle.NEON));
    }

    @Test
    void createVideoCaption_returns400OnInvalidFile() throws Exception {
        doThrow(new VideoValidationException("Only .mp4 and .mov files are supported."))
                .when(videoValidationService).validate(any());

        MockMultipartFile file = new MockMultipartFile("video", "clip.avi", "video/avi", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only .mp4 and .mov files are supported."));
    }

    @Test
    void createVideoCaption_passesThroughSelectedStyle() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video).param("style", "neon"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("fake-job-id"));

        verify(captionProcessingService, times(1))
                .process(eq("fake-job-id"), eq(jobDir), eq(inputPath), eq(CaptionStyle.NEON));
    }

    @Test
    void createVideoCaption_rejectsUnknownStyle() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video).param("style", "not-a-real-style"))
                .andExpect(status().isBadRequest());

        verify(captionProcessingService, never()).process(any(), any(), any(), any());
    }
}