package com.jorge.playlistconverter.controller;

import com.jorge.playlistconverter.dto.response.ApiStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiStatusControllerTest {

    @Test
    void shouldReturnUpStatusAsJson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ApiStatusController()).build();

        var result = mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON)).andReturn();

        ApiStatusResponse response = JsonMapper.builder()
                .build()
                .readValue(
                        result.getResponse().getContentAsString(),
                        ApiStatusResponse.class);

        assertEquals("UP", response.status());
    }
}
