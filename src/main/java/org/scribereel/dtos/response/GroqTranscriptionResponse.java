package org.scribereel.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.scribereel.dtos.internal.TranscriptWord;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GroqTranscriptionResponse(String text, List<TranscriptWord> words) {
}
