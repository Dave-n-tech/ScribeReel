package org.scribereel.services;

import org.junit.jupiter.api.Test;
import org.scribereel.dtos.internal.TranscriptWord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SubtitleGeneratorServiceImplTest {

    private final SubtitleGeneratorServiceImpl service = new SubtitleGeneratorServiceImpl();

    @Test
    void oneWordPerChunk_producesOneDialogueLinePerWord() throws IOException {
        List<TranscriptWord> words = List.of(
                new TranscriptWord("Hello", 0.0, 0.4),
                new TranscriptWord("world", 0.4, 0.8),
                new TranscriptWord("today", 0.8, 1.2)
        );

        Path tempAssFile = Files.createTempFile("test-1word", ".ass");
        service.generate(words, tempAssFile); // default overload = 1 word per chunk

        long dialogueLines = Files.readString(tempAssFile).lines()
                .filter(line -> line.startsWith("Dialogue:"))
                .count();

        assertThat(dialogueLines).isEqualTo(3);
        Files.deleteIfExists(tempAssFile);
    }

    @Test
    void threeWordsPerChunk_groupsConsecutiveWordsWithNoSilenceGap() throws IOException {
        // No silence gaps between any of these words - should group into a single
        // 3-word chunk, since maxWordsPerChunk=3 and nothing exceeds it.
        List<TranscriptWord> words = List.of(
                new TranscriptWord("this", 0.0, 0.3),
                new TranscriptWord("is", 0.3, 0.5),
                new TranscriptWord("ScribeReel", 0.5, 1.0)
        );

        Path tempAssFile = Files.createTempFile("test-3word", ".ass");
        service.generate(words, tempAssFile, 3);

        String content = Files.readString(tempAssFile);
        long dialogueLines = content.lines().filter(line -> line.startsWith("Dialogue:")).count();

        assertThat(dialogueLines).isEqualTo(1);
        assertThat(content).contains("this is ScribeReel");
        Files.deleteIfExists(tempAssFile);
    }

    @Test
    void silenceGap_forcesNewChunkEvenBelowWordCountTarget() throws IOException {
        // "this is" then a long pause, then "ScribeReel" - even with
        // maxWordsPerChunk=3, the pause should force a chunk break after "is",
        // so "ScribeReel" never appears on screen during the silence.
        List<TranscriptWord> words = List.of(
                new TranscriptWord("this", 0.0, 0.3),
                new TranscriptWord("is", 0.3, 0.5),
                new TranscriptWord("ScribeReel", 2.0, 2.5) // 1.5s gap - well above the 0.3s threshold
        );

        Path tempAssFile = Files.createTempFile("test-silence-split", ".ass");
        service.generate(words, tempAssFile, 3);

        String content = Files.readString(tempAssFile);
        long dialogueLines = content.lines().filter(line -> line.startsWith("Dialogue:")).count();

        // Should split into 2 chunks: "this is" and "ScribeReel", not 1 chunk of all 3
        assertThat(dialogueLines).isEqualTo(2);
        assertThat(content).contains("this is");
        assertThat(content).contains("ScribeReel");
        // Confirm "ScribeReel" chunk's start time matches its own word start (2.0s),
        // not the earlier "this"/"is" chunk's start - i.e. it doesn't appear early.
        assertThat(content).contains("0:00:02.00"); // ScribeReel's actual start timestamp
        Files.deleteIfExists(tempAssFile);
    }

    @Test
    void chunkDisplayDuration_isCappedRegardlessOfGapToNextChunk() throws IOException {
        // A single word with a long natural duration should still be capped at
        // MAX_CHUNK_DISPLAY_SECONDS (1.2s) rather than displaying for its full span.
        List<TranscriptWord> words = List.of(
                new TranscriptWord("looong", 0.0, 5.0) // unusually long single-word duration
        );

        Path tempAssFile = Files.createTempFile("test-cap", ".ass");
        service.generate(words, tempAssFile, 1);

        String content = Files.readString(tempAssFile);
        // End time should be capped near 1.2s, not the full 5.0s natural duration.
        assertThat(content).contains("0:00:01.20");
        Files.deleteIfExists(tempAssFile);
    }

    @Test
    void invalidWordsPerChunk_throwsIllegalArgumentException() {
        Path tempPath = Path.of("does-not-matter.ass");
        assertThatCode(() -> service.generate(List.of(new TranscriptWord("x", 0, 1)), tempPath, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyWordList_producesNoDialogueLines() throws IOException {
        Path tempAssFile = Files.createTempFile("test-empty", ".ass");

        assertThatCode(() -> service.generate(List.of(), tempAssFile, 1))
                .doesNotThrowAnyException();

        long dialogueLines = Files.readString(tempAssFile).lines()
                .filter(line -> line.startsWith("Dialogue:"))
                .count();
        assertThat(dialogueLines).isEqualTo(0);

        Files.deleteIfExists(tempAssFile);
    }


    @Test
    void formatsTimestampCorrectly() {
        // Testing via a chunk whose start/end we can predict exactly
        List<TranscriptWord> words = List.of(
                new TranscriptWord("word", 3725.5, 3726.0) // 1h 2m 5.5s
        );

        try {
            Path tempAssFile = Files.createTempFile("test-timestamp", ".ass");
            service.generate(words, tempAssFile);
            String content = Files.readString(tempAssFile);

            assertThat(content).contains("1:02:05.50");

            Files.deleteIfExists(tempAssFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}