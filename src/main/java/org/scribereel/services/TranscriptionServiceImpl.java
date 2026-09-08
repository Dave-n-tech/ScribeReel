package org.scribereel.services;

import org.scribereel.config.GroqApiProperties;
import org.scribereel.dtos.internal.TranscriptWord;
import org.scribereel.dtos.response.GroqTranscriptionResponse;
import org.scribereel.exceptions.VideoProcessingException;
import org.scribereel.client.GroqClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Service
public class TranscriptionServiceImpl implements TranscriptionService {

    private final Logger log = LoggerFactory.getLogger(TranscriptionServiceImpl.class);

    private final GroqClient groqClient;
    private final GroqApiProperties groqApiProperties;

    public TranscriptionServiceImpl(GroqClient groqClient,
                                    GroqApiProperties groqApiProperties) {
        this.groqClient = groqClient;
        this.groqApiProperties = groqApiProperties;
    }

    @Override
    public List<TranscriptWord> transcribe(Path audioPath) {
        File audioFile = audioPath.toFile();

        try {
            GroqTranscriptionResponse response = groqClient.transcribe(
                    "Bearer " + groqApiProperties.getKey(),
                    audioFile,
                    groqApiProperties.getModel(),
                    "verbose_json",
                    "word"
            );

            if (response.words() == null || response.words().isEmpty()) {
                throw new VideoProcessingException("Transcription returned no words - is the audio silent?");
            }

            return response.words();
        } catch (VideoProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoProcessingException("Transcription failed", e);
        }
    }

    @Override
    public String transcribeText(Path audioFilePath) {
        File audioFile = audioFilePath.toFile();
        try {
            GroqTranscriptionResponse response = groqClient.transcribe(
                    "Bearer " + groqApiProperties.getKey(),
                    audioFile,
                    groqApiProperties.getModel(),
                    "verbose_json",
                    "word"
            );

            if (response.text() == null || response.text().isBlank()) {
                throw new VideoProcessingException("Transcription returned no text - is the audio silent?");
            }

            return response.text();
        } catch (VideoProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoProcessingException("Transcription failed.", e);
        }
    }
}
