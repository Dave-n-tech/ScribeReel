package org.scribereel.services;

import org.scribereel.dtos.internal.TranscriptWord;

import java.nio.file.Path;
import java.util.List;

public interface SubtitleGeneratorService {
    void generate(List<TranscriptWord> words, Path outputAssPath);
    void generate(List<TranscriptWord> words, Path outputAssPath, int wordsPerChunk);
}
