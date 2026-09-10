package org.scribereel.dtos.response;

public record LimitsResponse(
        int maxFileSizeMb,
        FeatureLimits caption,
        FeatureLimits convert,
        FeatureLimits transcribe
) {
}