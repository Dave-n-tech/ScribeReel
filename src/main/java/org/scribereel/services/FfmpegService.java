package org.scribereel.services;


import java.nio.file.Path;

public interface FfmpegService {
    void extractAudio(Path inputVideoPath, Path audioPath);
    void burnSubtitles(Path inputVideoPath, Path assPath, Path outputVideoPath);
    double probeDurationSeconds(Path videoPath);
}
