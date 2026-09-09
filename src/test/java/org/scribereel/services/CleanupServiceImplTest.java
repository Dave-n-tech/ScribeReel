package org.scribereel.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scribereel.config.AppPropertiesConfig;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.file.Files;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CleanupServiceImplTest {
    private AppPropertiesConfig appProperties;

    @BeforeEach
    void setUp() throws Exception {
        appProperties = new AppPropertiesConfig();
        appProperties.setTempDir(Files.createTempDirectory("scribereel-cleanup-test").toString());
    }

    @Test
    void cleanupExpiredJobs_alsoPurgesJobRegistry() {
        // Assuming CleanupServiceImpl now takes JobRegistryService as a constructor dependency
        JobRegistryService mockRegistry = mock(JobRegistryService.class);
        CleanupServiceImpl service = new CleanupServiceImpl(appProperties, mockRegistry);

        service.cleanupExpiredJobs();

        verify(mockRegistry, times(1)).purgeOlderThan(any(Duration.class));
    }
}