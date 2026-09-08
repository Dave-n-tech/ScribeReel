package org.scribereel.client;

import org.scribereel.config.FeignMultipartConfig;
import org.scribereel.dtos.response.GroqTranscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;

import java.io.File;

@FeignClient(
        name = "groq-client",
        url = "${groq.api.base-url}",
        configuration = FeignMultipartConfig.class
)
public interface GroqClient {

    @PostMapping(value = "/openai/v1/audio/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    GroqTranscriptionResponse transcribe(
            @RequestHeader("Authorization") String bearerToken,
            @RequestPart("file") File audioFile,
            @RequestPart("model") String model,
            @RequestPart("response_format") String responseFormat,
            @RequestPart("timestamp_granularities[]") String timestampGranularities
    );
}
