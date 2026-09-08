package org.scribereel.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.scribereel.config.AppPropertiesConfig;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;

abstract class BaseControllerTest {
    @MockBean
    protected AppPropertiesConfig appProperties;

    @BeforeEach
    void setUpRateLimiting() {
        when(appProperties.getRateLimitMaxRequests()).thenReturn(1000);
        when(appProperties.getRateLimitMaxRequestsConvert()).thenReturn(1000);
        when(appProperties.getRateLimitWindowMinutes()).thenReturn(2);
    }
}