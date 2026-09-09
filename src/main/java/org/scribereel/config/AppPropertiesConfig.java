package org.scribereel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scribereel")
public class AppPropertiesConfig {
    private int maxDurationSecondsCaption = 180;
    private int maxDurationSecondsConvert = 180;
    private int maxDurationSecondsTranscribe = 1800;
    private int maxFileSize = 100;
    private int rateLimitWindowMinutes = 2;
    private int tempFileTtlMinutes = 15;
    private int ffmpegTimeoutMinutes = 5;
    private int rateLimitMaxRequests = 1;
    private int rateLimitMaxRequestsConvert = 5;

    private String tempDir = System.getProperty("java.io.tmpdir") + "/scribereel";

    public Path tempDirPath() {
        return Path.of(tempDir);
    }
}
