package org.scribereel.services;

import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.exceptions.VideoProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class FfmpegServiceImpl implements FfmpegService {

    private static final Logger log = LoggerFactory.getLogger(FfmpegServiceImpl.class);

    private final AppPropertiesConfig appProperties;

    public FfmpegServiceImpl(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void extractAudio(Path inputVideoPath, Path audioPath) {
        List<String> command = List.of(
                "ffmpeg",
                "-y",
                "-i", inputVideoPath.toString(),
                "-vn",
                "-acodec", "libmp3lame",
                audioPath.toString()
        );
        run(command, "audio extraction", null);
    }

    @Override
    public void burnSubtitles(Path inputVideoPath, Path assPath, Path outputVideoPath) {
        Path jobDir = assPath.getParent();
        String assFilename = assPath.getFileName().toString();

        List<String> command = List.of(
                "ffmpeg",
                "-y",
                "-i", inputVideoPath.toString(),
                "-vf", "ass=" + assFilename,
                "-c:a", "copy",
                outputVideoPath.toString()
        );
        run(command, "subtitle burn-in", jobDir);
    }

    @Override
    public double probeDurationSeconds(Path videoPath) {
        List<String> command = List.of(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath.toString()
        );
        String output = runAndCapture(command, "duration probe", null);
        try {
            return Double.parseDouble(output.trim());
        } catch (NumberFormatException e) {
            throw new VideoProcessingException("Could not read video duration.", e);
        }
    }

    private void run(List<String> command, String stepName, Path workingDir) {
        runAndCapture(command, stepName, workingDir);
    }

    private String runAndCapture(List<String> command, String stepName, Path workingDir) {
        log.info("Running {}: {}", stepName, String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        if (workingDir != null) {
            builder.directory(workingDir.toFile());
        }

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to start " + stepName + ". Is ffmpeg installed?", e);
        }

        StringBuilder output = new StringBuilder();
        try (var reader = process.inputReader()) {
            reader.lines().forEach(line -> output.append(line).append('\n'));
        } catch (IOException e) {
            log.warn("Failed reading output for {}", stepName, e);
        }

        boolean finished;
        try {
            finished = process.waitFor(appProperties.getFfmpegTimeoutMinutes(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new VideoProcessingException(stepName + " was interrupted.", e);
        }

        if (!finished) {
            process.destroyForcibly();
            log.error("{} timed out after {} minutes", stepName, appProperties.getFfmpegTimeoutMinutes());
            throw new VideoProcessingException(stepName + " timed out and was terminated.");
        }

        if (process.exitValue() != 0) {
            log.error("{} failed (exit {}): {}", stepName, process.exitValue(), output);
            throw new VideoProcessingException(stepName + " failed.");
        }

        return output.toString();
    }
}
