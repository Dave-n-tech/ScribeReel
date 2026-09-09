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
class ConvertProcessingServiceTest {

    @Mock private FfmpegService ffmpegService;
    @Mock private JobRegistryService jobRegistryService;

    private ConvertProcessingService service;

    private void setUp() {
        service = new ConvertProcessingService(ffmpegService, jobRegistryService);
    }

    @Test
    void process_happyPath_marksProcessingThenDone() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        service.process("job-1", jobDir, inputPath);

        var inOrder = inOrder(jobRegistryService, ffmpegService);
        inOrder.verify(jobRegistryService).markProcessing("job-1");
        inOrder.verify(ffmpegService).extractAudio(eq(inputPath), eq(jobDir.resolve("result.mp3")));
        inOrder.verify(jobRegistryService).markDone("job-1", "/api/download/job-1");

        verify(jobRegistryService, never()).markFailed(any(), any());
    }

    @Test
    void process_whenFfmpegThrows_marksFailed() {
        setUp();
        Path jobDir = Path.of("/tmp/job-1");
        Path inputPath = jobDir.resolve("input.mp4");

        doThrow(new VideoProcessingException("extraction failed"))
                .when(ffmpegService).extractAudio(any(), any());

        service.process("job-1", jobDir, inputPath);

        verify(jobRegistryService).markFailed(eq("job-1"), any());
        verify(jobRegistryService, never()).markDone(any(), any());
    }
}