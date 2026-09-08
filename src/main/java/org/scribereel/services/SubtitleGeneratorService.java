package org.scribereel.services;

import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.enums.CaptionStyle;

import java.nio.file.Path;
import java.util.List;

public interface SubtitleGeneratorService {
    void generate(List<TranscriptWord> words, Path outputAssPath);
    void generate(List<TranscriptWord> words, Path outputAssPath, CaptionStyle style);
}
