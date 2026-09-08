package org.scribereel.controllers;

import org.scribereel.config.AppPropertiesConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
public class DownloadController {

    private final AppPropertiesConfig appProperties;

    public DownloadController(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/api/download/{jobId}")
    public ResponseEntity<Resource> download(@PathVariable String jobId) {
        Path jobDir = appProperties.tempDirPath().resolve(jobId);
        if (!Files.isDirectory(jobDir)) {
            return ResponseEntity.notFound().build();
        }

        Optional<Path> resultFile = findResultFile(jobDir);
        if (resultFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Path file = resultFile.get();
        Resource resource = new FileSystemResource(file);
        MediaType contentType = resolveContentType(file);

        return ResponseEntity.ok()
                .contentType(contentType)
                .header("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    /** Every job's final deliverable is named "result.<ext>", regardless of which feature produced it. */
    private Optional<Path> findResultFile(Path jobDir) {
        try (Stream<Path> files = Files.list(jobDir)) {
            return files.filter(p -> p.getFileName().toString().startsWith("result."))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private MediaType resolveContentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".mp4")) return MediaType.valueOf("video/mp4");
        if (name.endsWith(".mp3")) return MediaType.valueOf("audio/mpeg");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}