package org.scribereel.services;

import org.junit.jupiter.api.Test;
import org.scribereel.config.AppPropertiesConfig;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JobFileServiceImplTest {

    @Test
    void createJob_generatesUniqueDirUnderConfiguredTempDir() throws Exception {
        Path tempRoot = Files.createTempDirectory("scribereel-test");
        AppPropertiesConfig props = new AppPropertiesConfig();
        props.setTempDir(tempRoot.toString());

        JobFileServiceImpl service = new JobFileServiceImpl(props);

        JobFileService.JobContext job1 = service.createJob();
        JobFileService.JobContext job2 = service.createJob();

        assertThat(job1.jobId()).isNotEqualTo(job2.jobId());
        assertThat(Files.isDirectory(job1.jobDir())).isTrue();
        assertThat(job1.jobDir().getParent()).isEqualTo(tempRoot);
    }

    @Test
    void saveUpload_writesFileWithCorrectExtension() throws Exception {
        Path tempRoot = Files.createTempDirectory("scribereel-test");
        AppPropertiesConfig props = new AppPropertiesConfig();
        props.setTempDir(tempRoot.toString());

        JobFileServiceImpl service = new JobFileServiceImpl(props);
        JobFileService.JobContext job = service.createJob();

        MockMultipartFile upload = new MockMultipartFile(
                "video", "clip.mp4", "video/mp4", "fake-bytes".getBytes());

        Path saved = service.saveUpload(upload, job.jobDir());

        assertThat(saved.getFileName().toString()).isEqualTo("input.mp4");
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.readString(saved)).isEqualTo("fake-bytes");
    }

    @Test
    void saveUpload_handlesMissingExtensionGracefully() throws Exception {
        Path tempRoot = Files.createTempDirectory("scribereel-test");
        AppPropertiesConfig props = new AppPropertiesConfig();
        props.setTempDir(tempRoot.toString());

        JobFileServiceImpl service = new JobFileServiceImpl(props);
        JobFileService.JobContext job = service.createJob();

        MockMultipartFile upload = new MockMultipartFile(
                "video", "noextension", "application/octet-stream", "bytes".getBytes());

        Path saved = service.saveUpload(upload, job.jobDir());

        assertThat(saved.getFileName().toString()).isEqualTo("input"); // no extension appended
    }
}