package org.scribereel.services;

import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.exceptions.VideoValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Set;

@Service
public class VideoValidationServiceImpl implements VideoValidationService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".mov");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(".mp3", ".wav", ".m4a", ".aac");

    private final AppPropertiesConfig appProperties;
    private final FfmpegService ffmpegService;

    public VideoValidationServiceImpl(AppPropertiesConfig appProperties, FfmpegService ffmpegService) {
        this.appProperties = appProperties;
        this.ffmpegService = ffmpegService;
    }

    @Override
    public void validate(MultipartFile file) {
        validateExtension(file, VIDEO_EXTENSIONS, "Only .mp4 and .mov files are supported.");
    }

    @Override
    public void validateMedia(MultipartFile file) {
        Set<String> allowed = union(VIDEO_EXTENSIONS, AUDIO_EXTENSIONS);
        validateExtension(file, allowed, "Only video (.mp4, .mov) or audio (.mp3, .wav, .m4a, .aac) files are supported.");
    }

    @Override
    public void validateDuration(Path mediaPath) {
        double durationSeconds = ffmpegService.probeDurationSeconds(mediaPath);
        if (durationSeconds > appProperties.getMaxDurationSeconds()) {
            throw new VideoValidationException(
                    "Media is " + Math.round(durationSeconds) + "s - max allowed is "
                            + appProperties.getMaxDurationSeconds() + "s.");
        }
    }

    @Override
    public boolean isAudioFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return AUDIO_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private void validateExtension(MultipartFile file, Set<String> allowedExtensions, String errorMessage) {
        if (file == null || file.isEmpty()) {
            throw new VideoValidationException("No file was uploaded.");
        }

        long maxBytes = appProperties.getMaxFileSize() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new VideoValidationException("File exceeds the " + appProperties.getMaxFileSize() + "MB limit.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || allowedExtensions.stream().noneMatch(ext -> filename.toLowerCase().endsWith(ext))) {
            throw new VideoValidationException(errorMessage);
        }
    }

    private Set<String> union(Set<String> a, Set<String> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}