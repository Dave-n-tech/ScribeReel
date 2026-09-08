package org.scribereel.services;

import org.scribereel.config.AppPropertiesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class CleanupServiceImpl implements CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupServiceImpl.class);

    private final AppPropertiesConfig appProperties;

    public CleanupServiceImpl(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    @Scheduled(fixedRate = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void cleanupExpiredJobs() {
        Path tempDir = appProperties.tempDirPath();
        if (!Files.exists(tempDir)) return;

        Instant cutoff = Instant.now().minus(appProperties.getTempFileTtlMinutes(), ChronoUnit.MINUTES);

        try (Stream<Path> jobDirs = Files.list(tempDir)) {
            jobDirs.filter(Files::isDirectory)
                    .filter(dir -> isOlderThan(dir, cutoff))
                    .forEach(this::deleteRecursively);
        } catch (IOException e) {
            log.warn("Cleanup sweep failed to list temp dir", e);
        }
    }

    private boolean isOlderThan(Path dir, Instant cutoff) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(dir, BasicFileAttributes.class);
            return attrs.creationTime().toInstant().isBefore(cutoff);
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteRecursively(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("Failed to delete {}", p, e);
                }
            });
            log.info("Cleaned up expired job dir: {}", dir);
        } catch (IOException e) {
            log.warn("Failed to walk {} for deletion", dir, e);
        }
    }

}
