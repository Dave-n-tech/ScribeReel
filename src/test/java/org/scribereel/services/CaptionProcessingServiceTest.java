package org.scribereel.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.enums.CaptionStyle;
import org.scribereel.exceptions.VideoProcessingException;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptionProcessingServiceTest {

    @Mock private FfmpegService ffmpegService;
    @Mock private TranscriptionService transcriptionService;
    @Mock private SubtitleGeneratorService subtitleGeneratorService;
    @Mock private JobRegistryService jobRegistryService;

    private CaptionProcessingService service;

    private void setUp() {
        service = new CaptionProcessingService(
                ffmpegService, transcriptionService, subtitleGeneratorService, jobRegistryService);
    }

    @Test
    void process_happyPath_marksProcessingThenDone() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        when(transcriptionService.transcribe(any()))
                .thenReturn(List.of(new TranscriptWord("hi", 0.0, 0.3)));

        service.process("job-1", jobDir, inputPath, CaptionStyle.PUNCH);

        var inOrder = inOrder(jobRegistryService, ffmpegService, transcriptionService,
                subtitleGeneratorService);
        inOrder.verify(jobRegistryService).markProcessing("job-1");
        inOrder.verify(ffmpegService).extractAudioForTranscription(eq(inputPath), any());
        inOrder.verify(transcriptionService).transcribe(any());
        inOrder.verify(subtitleGeneratorService).generate(any(), any(), eq(CaptionStyle.PUNCH));
        inOrder.verify(ffmpegService).burnSubtitles(eq(inputPath), any(), any());
        inOrder.verify(jobRegistryService).markDone(eq("job-1"), eq("/api/download/job-1"));

        verify(jobRegistryService, never()).markFailed(any(), any());
    }

    @Test
    void process_whenFfmpegThrows_marksFailedInsteadOfPropagating() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        doThrow(new VideoProcessingException("audio extraction failed"))
                .when(ffmpegService).extractAudioForTranscription(any(), any()); // <- changed

        service.process("job-1", jobDir, inputPath, CaptionStyle.PUNCH);

        verify(jobRegistryService).markFailed(eq("job-1"), any());
        verify(jobRegistryService, never()).markDone(any(), any());
        verify(transcriptionService, never()).transcribe(any());
    }

    @Test
    void process_whenTranscriptionThrows_marksFailed() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        when(transcriptionService.transcribe(any()))
                .thenThrow(new VideoProcessingException("transcription failed"));

        service.process("job-1", jobDir, inputPath, CaptionStyle.PUNCH);

        verify(jobRegistryService).markFailed(eq("job-1"), any());
        verify(subtitleGeneratorService, never()).generate(any(), any(), any());
        verify(ffmpegService, never()).burnSubtitles(any(), any(), any());
    }
}