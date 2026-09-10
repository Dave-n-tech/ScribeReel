package org.scribereel.services;


import java.nio.file.Path;

public interface FfmpegService {
    void extractAudio(Path inputVideoPath, Path audioPath);
    /** Low-bitrate mono extraction, sized to stay under Groq's 25MB upload cap even for long audio. */
    void extractAudioForTranscription(Path inputVideoPath, Path audioPath);
    void burnSubtitles(Path inputVideoPath, Path assPath, Path outputVideoPath);
    double probeDurationSeconds(Path videoPath);
}
