package org.scribereel.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scribereel.exceptions.VideoProcessingException;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscriptionProcessingServiceTest {

    @Mock private FfmpegService ffmpegService;
    @Mock private TranscriptionService transcriptionService;
    @Mock private VideoValidationService videoValidationService;
    @Mock private JobRegistryService jobRegistryService;

    private TranscriptionProcessingService service;

    private void setUp() {
        service = new TranscriptionProcessingService(
                ffmpegService, transcriptionService, videoValidationService, jobRegistryService);
    }

    @Test
    void process_extractsAudioFirst_whenInputIsVideo() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        when(videoValidationService.isAudioFile("input.mp4")).thenReturn(false);
        when(transcriptionService.transcribeText(any())).thenReturn("Hello world");

        service.process("job-1", jobDir, inputPath);

        verify(ffmpegService, times(1)).extractAudioForTranscription(eq(inputPath), any());
        verify(jobRegistryService).markDoneWithText("job-1", "Hello world");
        verify(jobRegistryService, never()).markFailed(any(), any());
    }

    @Test
    void process_skipsExtraction_whenInputIsAlreadyAudio() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp3");

        when(videoValidationService.isAudioFile("input.mp3")).thenReturn(true);
        when(transcriptionService.transcribeText(inputPath)).thenReturn("Hello from audio");

        service.process("job-1", jobDir, inputPath);

        verify(ffmpegService, never()).extractAudio(any(), any());
        verify(transcriptionService, times(1)).transcribeText(inputPath);
        verify(jobRegistryService).markDoneWithText("job-1", "Hello from audio");
    }

    @Test
    void process_whenTranscriptionThrows_marksFailed() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp3");

        when(videoValidationService.isAudioFile("input.mp3")).thenReturn(true);
        when(transcriptionService.transcribeText(any()))
                .thenThrow(new VideoProcessingException("failed"));

        service.process("job-1", jobDir, inputPath);

        verify(jobRegistryService).markFailed(eq("job-1"), any());
        verify(jobRegistryService, never()).markDoneWithText(any(), any());
    }

    @Test
    void process_whenExtractionThrows_marksFailedWithoutCallingTranscription() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        when(videoValidationService.isAudioFile("input.mp4")).thenReturn(false);
        doThrow(new VideoProcessingException("extraction failed"))
                .when(ffmpegService).extractAudioForTranscription(any(), any());

        service.process("job-1", jobDir, inputPath);

        verify(jobRegistryService).markFailed(eq("job-1"), any());
        verify(transcriptionService, never()).transcribeText(any());
    }
}