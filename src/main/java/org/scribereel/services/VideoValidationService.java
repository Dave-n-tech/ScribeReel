package org.scribereel.services;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface VideoValidationService {
    void validate(MultipartFile video);
    void validateMedia(MultipartFile file);
    void validateDuration(Path videoPath, int maxDurationSeconds);
    boolean isAudioFile(String filename);
}
