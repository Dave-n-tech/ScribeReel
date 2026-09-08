package org.scribereel.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.exceptions.VideoValidationException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoValidationServiceImplTest {

    @Mock
    private FfmpegService ffmpegService;

    private VideoValidationServiceImpl service;
    private AppPropertiesConfig props;

    private void setUp() {
        props = new AppPropertiesConfig();
        props.setMaxFileSize(100);
        props.setMaxDurationSeconds(60);
        service = new VideoValidationServiceImpl(props, ffmpegService);
    }

    @Test
    void validateBasic_acceptsMp4() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());
        assertThatCode(() -> service.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validateBasic_rejectsUnsupportedExtension() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("video", "clip.avi", "video/avi", "bytes".getBytes());
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(VideoValidationException.class)
                .hasMessageContaining(".mp4");
    }

    @Test
    void validateBasic_rejectsOversizedFile() {
        setUp();
        props.setMaxFileSize(1); // 1MB cap
        byte[] tooBig = new byte[2 * 1024 * 1024]; // 2MB
        MockMultipartFile file = new MockMultipartFile("video", "clip.mp4", "video/mp4", tooBig);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(VideoValidationException.class)
                .hasMessageContaining("1MB limit");
    }

    @Test
    void validateBasicMedia_acceptsAudioExtensions() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp3", "audio/mpeg", "bytes".getBytes());
        assertThatCode(() -> service.validateMedia(file)).doesNotThrowAnyException();
    }

    @Test
    void validateBasicMedia_rejectsUnsupportedExtension() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("file", "clip.ogg", "audio/ogg", "bytes".getBytes());
        assertThatThrownBy(() -> service.validateMedia(file))
                .isInstanceOf(VideoValidationException.class);
    }

    @Test
    void validateDuration_rejectsTooLong() {
        setUp();
        Path fakePath = Path.of("fake.mp4");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(120.0);

        assertThatThrownBy(() -> service.validateDuration(fakePath))
                .isInstanceOf(VideoValidationException.class)
                .hasMessageContaining("max allowed is 60s");
    }

    @Test
    void validateDuration_acceptsWithinLimit() {
        setUp();
        Path fakePath = Path.of("fake.mp4");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(45.0);

        assertThatCode(() -> service.validateDuration(fakePath)).doesNotThrowAnyException();
    }

    @Test
    void isAudioFile_detectsKnownAudioExtensions() {
        setUp();
        assertThat(service.isAudioFile("clip.mp3")).isTrue();
        assertThat(service.isAudioFile("clip.wav")).isTrue();
        assertThat(service.isAudioFile("clip.mp4")).isFalse();
        assertThat(service.isAudioFile(null)).isFalse();
    }
}