package org.scribereel.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LimitsController.class)
class LimitsControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getLimits_returnsConfiguredValuesForEachFeature() throws Exception {
        when(appProperties.getMaxFileSize()).thenReturn(250);
        when(appProperties.getMaxDurationSecondsCaption()).thenReturn(180);
        when(appProperties.getMaxDurationSecondsConvert()).thenReturn(180);
        when(appProperties.getMaxDurationSecondsTranscribe()).thenReturn(1800);

        mockMvc.perform(get("/api/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxFileSizeMb").value(250))
                .andExpect(jsonPath("$.caption.maxDurationSeconds").value(180))
                .andExpect(jsonPath("$.convert.maxDurationSeconds").value(180))
                .andExpect(jsonPath("$.transcribe.maxDurationSeconds").value(1800));
    }
}