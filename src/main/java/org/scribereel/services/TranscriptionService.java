package org.scribereel.services;

import org.scribereel.dtos.internal.TranscriptWord;

import java.nio.file.Path;
import java.util.List;

public interface TranscriptionService {
    List<TranscriptWord> transcribe(Path audioPath);
    String transcribeText(Path audioFilePath);
}
