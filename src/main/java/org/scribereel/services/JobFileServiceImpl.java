package org.scribereel.services;

import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.exceptions.VideoProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class JobFileServiceImpl implements JobFileService {

    private final AppPropertiesConfig appProperties;

    public JobFileServiceImpl(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public JobContext createJob() {
        String jobId = UUID.randomUUID().toString();
        Path jobDir = appProperties.tempDirPath().resolve(jobId);
        try {
            Files.createDirectories(jobDir);
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to create job directory.", e);
        }
        return new JobContext(jobId, jobDir);
    }

    @Override
    public Path saveUpload(MultipartFile file, Path jobDir) {
        Path inputPath = jobDir.resolve("input" + extensionOf(file.getOriginalFilename()));
        try {
            file.transferTo(inputPath);
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to save the uploaded file.", e);
        }
        return inputPath;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ""; // no extension available - caller decides how to handle
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}