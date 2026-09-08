package org.scribereel.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "groq.api")
public class GroqApiProperties {

    @NotBlank(message = "groq.api.key must be set - check your active profile or GROQ_API_KEY env var")
    private String key;

    private String baseUrl;
    private String model = "whisper-large-v3";

}
