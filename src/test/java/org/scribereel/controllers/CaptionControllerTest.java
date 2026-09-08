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

    @Test
    void createVideoCaption_returnsDownloadUrlOnSuccess() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(transcriptionService.transcribe(any()))
                .thenReturn(List.of(new TranscriptWord("hello", 0.0, 0.5)));

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("/api/download/fake-job-id"));

        verify(ffmpegService, times(1)).extractAudio(any(), any());
        verify(subtitleGeneratorService, times(1)).generate(any(), any(), eq(CaptionStyle.PUNCH)); // default style
        verify(ffmpegService, times(1)).burnSubtitles(any(), any(), any());
    }

    @Test
    void createVideoCaption_passesThroughStyle() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(transcriptionService.transcribe(any()))
                .thenReturn(List.of(new TranscriptWord("hello", 0.0, 0.5)));

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption")
                        .file(video)
                        .param("style", "NEON"))
                .andExpect(status().isOk());

        verify(subtitleGeneratorService, times(1)).generate(any(), any(), eq(CaptionStyle.NEON));
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
        when(transcriptionService.transcribe(any()))
                .thenReturn(List.of(new TranscriptWord("hello", 0.0, 0.5)));

        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video).param("style", "neon"))
                .andExpect(status().isOk());

        verify(subtitleGeneratorService, times(1)).generate(any(), any(), eq(CaptionStyle.NEON));
    }

    @Test
    void createVideoCaption_rejectsUnknownStyle() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/caption").file(video).param("style", "not-a-real-style"))
                .andExpect(status().isBadRequest());
    }
}