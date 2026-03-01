package com.example.api.common.validation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@WebMvcTest
@Import(CrossFieldValidationIntegrationTest.TestController.class)
class CrossFieldValidationIntegrationTest {

    @ValidDateRange(from = "startDate", to = "endDate")
    @RequiredWhen(field = "endDate", dependsOn = "startDate")
    static class TestRequest {

        public String startDate;
        public String endDate;
    }

    @RestController
    @RequestMapping("/test-validation")
    static class TestController {

        @PostMapping
        public Object create(@Valid @RequestBody TestRequest request) {
            return Map.of("status", "ok");
        }
    }

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void post_shouldReturn400_whenDateRangeIsInvalid() throws Exception {
        TestRequest request = new TestRequest();
        request.startDate = "2024/12/31";
        request.endDate = "2024/01/01";

        mockMvc.perform(
                        post("/test-validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn400_whenRequiredFieldIsMissing() throws Exception {
        TestRequest request = new TestRequest();
        request.startDate = "2024/01/01";
        request.endDate = null;

        mockMvc.perform(
                        post("/test-validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn200_whenRequestIsValid() throws Exception {
        TestRequest request = new TestRequest();
        request.startDate = "2024/01/01";
        request.endDate = "2024/12/31";

        mockMvc.perform(
                        post("/test-validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
