package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.scribereel.config.AppPropertiesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DownloadController.class)
class DownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppPropertiesConfig appProperties;

    @Test
    void download_returns404_whenJobDirDoesNotExist() throws Exception {
        Path tempRoot = Files.createTempDirectory("scribereel-download-test");
        when(appProperties.tempDirPath()).thenReturn(tempRoot);

        mockMvc.perform(get("/api/download/nonexistent-job"))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_returnsVideoWithCorrectContentType() throws Exception {
        Path tempRoot = Files.createTempDirectory("scribereel-download-test");
        Path jobDir = tempRoot.resolve("real-job");
        Files.createDirectories(jobDir);
        Files.writeString(jobDir.resolve("result.mp4"), "fake video bytes");

        when(appProperties.tempDirPath()).thenReturn(tempRoot);

        mockMvc.perform(get("/api/download/real-job"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "video/mp4"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"result.mp4\""));
    }

    @Test
    void download_returnsMp3WithCorrectContentType() throws Exception {
        Path tempRoot = Files.createTempDirectory("scribereel-download-test");
        Path jobDir = tempRoot.resolve("audio-job");
        Files.createDirectories(jobDir);
        Files.writeString(jobDir.resolve("result.mp3"), "fake audio bytes");

        when(appProperties.tempDirPath()).thenReturn(tempRoot);

        mockMvc.perform(get("/api/download/audio-job"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"));
    }
}