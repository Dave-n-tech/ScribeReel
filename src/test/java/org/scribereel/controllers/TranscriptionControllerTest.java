package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.services.FfmpegService;
import org.scribereel.services.JobFileService;
import org.scribereel.services.TranscriptionService;
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
class TranscriptionControllerTest extends BaseControllerTest{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoValidationService videoValidationService;

    @MockBean
    private FfmpegService ffmpegService;

    @MockBean
    private TranscriptionService transcriptionService;

    @MockBean
    private JobFileService jobFileService;

    @Test
    void transcribe_extractsAudioFirst_whenInputIsVideo() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp4");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(videoValidationService.isAudioFile("clip.mp4")).thenReturn(false);
        when(transcriptionService.transcribeText(any())).thenReturn("Hello world");

        MockMultipartFile video = new MockMultipartFile("file", "clip.mp4", "video/mp4", "bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(video))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello world"));

        // Confirms extraction actually happened for a video input.
        verify(ffmpegService, times(1)).extractAudio(eq(inputPath), any());
    }

    @Test
    void transcribe_skipsExtraction_whenInputIsAlreadyAudio() throws Exception {
        Path jobDir = Path.of(System.getProperty("java.io.tmpdir"), "fake-job");
        JobFileService.JobContext context = new JobFileService.JobContext("fake-job-id", jobDir);
        Path inputPath = jobDir.resolve("input.mp3");

        when(jobFileService.createJob()).thenReturn(context);
        when(jobFileService.saveUpload(any(), any())).thenReturn(inputPath);
        when(videoValidationService.isAudioFile("clip.mp3")).thenReturn(true);
        when(transcriptionService.transcribeText(any())).thenReturn("Hello from audio");

        MockMultipartFile audio = new MockMultipartFile("file", "clip.mp3", "audio/mpeg", "bytes".getBytes());

        mockMvc.perform(multipart("/api/transcribe").file(audio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello from audio"));

        // The key assertion: extraction must NOT run for an already-audio input.
        verify(ffmpegService, never()).extractAudio(any(), any());
        verify(transcriptionService, times(1)).transcribeText(inputPath);
    }
}