package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.services.FfmpegService;
import org.scribereel.services.JobFileService;
import org.scribereel.services.VideoValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest extends BaseControllerTest{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoValidationService videoValidationService;

    @MockBean
    private FfmpegService ffmpegService;

    @MockBean
    private JobFileService jobFileService;

    @Test
    void convertToMp3_returnsDownloadUrlOnSuccess() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(jobDir.resolve("input.mp4"));
        doNothing().when(videoValidationService).validate(any());
        doNothing().when(videoValidationService).validateDuration(any());
        doNothing().when(ffmpegService).extractAudio(any(), any());

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/convert").file(video))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("/api/download/fake-job-id"));
    }

    @Test
    void convertToMp3_returns400OnInvalidFile() throws Exception {
        doThrow(new org.scribereel.exceptions.VideoValidationException("Only .mp4 and .mov files are supported."))
                .when(videoValidationService).validate(any());

        MockMultipartFile file = new MockMultipartFile("video", "clip.avi", "video/avi", "bytes".getBytes());

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only .mp4 and .mov files are supported."));
    }
}