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
        service = new VideoValidationServiceImpl(props, ffmpegService);
    }

    @Test
    void appPropertiesConfig_hasExpectedProductionDefaults() {
        AppPropertiesConfig realProps = new AppPropertiesConfig();

        assertThat(realProps.getMaxDurationSecondsCaption()).isEqualTo(180);
        assertThat(realProps.getMaxDurationSecondsConvert()).isEqualTo(180);
        assertThat(realProps.getMaxDurationSecondsTranscribe()).isEqualTo(3600);
    }

    @Test
    void validateDuration_acceptsExactlyAtTheLimit() {
        setUp();
        Path fakePath = Path.of("fake.mp4");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(180.0);

        assertThatCode(() -> service.validateDuration(fakePath, 180)).doesNotThrowAnyException();
    }

    @Test
    void validateDuration_rejectsOneSecondOverTheLimit() {
        setUp();
        Path fakePath = Path.of("fake.mp4");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(181.0);

        assertThatThrownBy(() -> service.validateDuration(fakePath, 180))
                .isInstanceOf(VideoValidationException.class);
    }

    @Test
    void validate_acceptsFileExactlyAtSizeLimit() {
        setUp();
        props.setMaxFileSize(1); // 1MB
        byte[] exactlyOneMb = new byte[1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("video", "clip.mp4", "video/mp4", exactlyOneMb);

        assertThatCode(() -> service.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsFileOneByteOverSizeLimit() {
        setUp();
        props.setMaxFileSize(1); // 1MB
        byte[] oneByteOver = new byte[1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("video", "clip.mp4", "video/mp4", oneByteOver);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(VideoValidationException.class);
    }

    @Test
    void validate_acceptsMp4() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("video", "clip.mp4", "video/mp4", "bytes".getBytes());
        assertThatCode(() -> service.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUnsupportedExtension() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("video", "clip.avi", "video/avi", "bytes".getBytes());
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(VideoValidationException.class)
                .hasMessageContaining(".mp4");
    }

    @Test
    void validate_rejectsOversizedFile() {
        setUp();
        props.setMaxFileSize(1); // 1MB cap
        byte[] tooBig = new byte[2 * 1024 * 1024]; // 2MB
        MockMultipartFile file = new MockMultipartFile("video", "clip.mp4", "video/mp4", tooBig);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(VideoValidationException.class)
                .hasMessageContaining("1MB limit");
    }

    @Test
    void validateMedia_acceptsAudioExtensions() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp3", "audio/mpeg", "bytes".getBytes());
        assertThatCode(() -> service.validateMedia(file)).doesNotThrowAnyException();
    }

    @Test
    void validateMedia_rejectsUnsupportedExtension() {
        setUp();
        MockMultipartFile file = new MockMultipartFile("file", "clip.ogg", "audio/ogg", "bytes".getBytes());
        assertThatThrownBy(() -> service.validateMedia(file))
                .isInstanceOf(VideoValidationException.class);
    }

    @Test
    void validateDuration_rejectsWhenOverGivenLimit() {
        setUp();
        Path fakePath = Path.of("fake.mp4");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(200.0);

        assertThatThrownBy(() -> service.validateDuration(fakePath, 180))
                .isInstanceOf(VideoValidationException.class)
                .hasMessageContaining("max allowed is 180s");
    }

    @Test
    void validateDuration_acceptsWithinGivenLimit() {
        setUp();
        Path fakePath = Path.of("fake.mp4");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(150.0);

        assertThatCode(() -> service.validateDuration(fakePath, 180)).doesNotThrowAnyException();
    }

    @Test
    void validateDuration_appliesDifferentLimitsPerCaller() {
        setUp();
        // Same probed duration (1000s), but validated against transcription's much
        // larger allowance vs caption/convert's smaller one - confirms the limit
        // is genuinely caller-supplied, not read from a single fixed property anymore.
        Path fakePath = Path.of("fake.mp3");
        when(ffmpegService.probeDurationSeconds(fakePath)).thenReturn(1000.0);

        assertThatThrownBy(() -> service.validateDuration(fakePath, 180)) // caption/convert-sized limit
                .isInstanceOf(VideoValidationException.class);

        assertThatCode(() -> service.validateDuration(fakePath, 1800)) // transcription-sized limit
                .doesNotThrowAnyException();
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