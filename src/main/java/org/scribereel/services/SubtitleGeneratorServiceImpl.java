package org.scribereel.services;

import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.enums.CaptionStyle;
import org.scribereel.exceptions.VideoProcessingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Service
public class SubtitleGeneratorServiceImpl implements SubtitleGeneratorService {

    private static final double MAX_CHUNK_DISPLAY_SECONDS = 1.2;

    private static final double SILENCE_GAP_THRESHOLD_SECONDS = 0.5;

    private static final String ASS_HEADER_TEMPLATE = """
            [Script Info]
            ScriptType: v4.00+
            PlayResX: 1080
            PlayResY: 1920

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, OutlineColour, Bold, BorderStyle, Outline, Alignment, MarginV
            %s

            [Events]
            Format: Layer, Start, End, Style, Text
            """;

    @Override
    public void generate(List<TranscriptWord> words, Path outputAssPath) {
        generate(words, outputAssPath, CaptionStyle.PUNCH); // default: one word at a time
    }

    @Override
    public void generate(List<TranscriptWord> words, Path outputAssPath, CaptionStyle style) {
        List<List<TranscriptWord>> chunks = chunkBySilence(words, style.getWordsPerChunk());

        StringBuilder sb = new StringBuilder(ASS_HEADER_TEMPLATE.formatted(style.toAssStyleLine()));

        for (List<TranscriptWord> chunk : chunks) {
            appendDialogueLine(sb, chunk);
        }

        try {
            Files.writeString(outputAssPath, sb.toString());
        } catch (IOException e) {
            throw new VideoProcessingException("Failed to write subtitle file.", e);
        }
    }

    private List<List<TranscriptWord>> chunkBySilence(List<TranscriptWord> words, int maxWordsPerChunk) {
        List<List<TranscriptWord>> chunks = new ArrayList<>();
        if (words.isEmpty()) {
            return chunks;
        }

        List<TranscriptWord> currentChunk = new ArrayList<>();
        currentChunk.add(words.get(0));

        for (int i = 1; i < words.size(); i++) {
            TranscriptWord previous = words.get(i - 1);
            TranscriptWord current = words.get(i);
            double gap = current.start() - previous.end();

            boolean silenceDetected = gap > SILENCE_GAP_THRESHOLD_SECONDS;
            boolean chunkFull = currentChunk.size() >= maxWordsPerChunk;

            if (silenceDetected || chunkFull) {
                chunks.add(currentChunk);
                currentChunk = new ArrayList<>();
            }

            currentChunk.add(current);
        }

        chunks.add(currentChunk); // flush the last chunk
        return chunks;
    }

    private void appendDialogueLine(StringBuilder sb, List<TranscriptWord> chunk) {
        double start = chunk.get(0).start();
        double naturalEnd = chunk.get(chunk.size() - 1).end();
        double end = Math.min(naturalEnd, start + MAX_CHUNK_DISPLAY_SECONDS);

        String text = chunk.stream()
                .map(TranscriptWord::word)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        sb.append("Dialogue: 0,")
                .append(formatTimestamp(start)).append(",")
                .append(formatTimestamp(end)).append(",")
                .append("Default,").append(text).append("\n");
    }

    /** .ass timestamps are H:MM:SS.CC (centiseconds). */
    private String formatTimestamp(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        double secs = seconds % 60;
        return String.format("%d:%02d:%05.2f", hours, minutes, secs);
    }
}
