package org.scribereel.controllers;

import org.scribereel.config.AppPropertiesConfig;
import org.scribereel.dtos.response.FeatureLimits;
import org.scribereel.dtos.response.LimitsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LimitsController {

    private final AppPropertiesConfig appProperties;

    public LimitsController(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/api/limits")
    public LimitsResponse getLimits() {
        return new LimitsResponse(
                appProperties.getMaxFileSize(),
                new FeatureLimits(appProperties.getMaxDurationSecondsCaption()),
                new FeatureLimits(appProperties.getMaxDurationSecondsConvert()),
                new FeatureLimits(appProperties.getMaxDurationSecondsTranscribe())
        );
    }
}