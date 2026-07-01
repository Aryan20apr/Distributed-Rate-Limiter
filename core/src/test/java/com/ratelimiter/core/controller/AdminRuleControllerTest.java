package com.ratelimiter.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.config.RateLimitStoreType;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.service.RuleConfigService;
import com.ratelimiter.core.web.AdminAuthFilter;
import com.ratelimiter.core.web.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AdminRuleControllerTest {

    @Mock
    private RuleConfigService configService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setStore(RateLimitStoreType.REDIS);
        properties.getAdmin().setEnabled(true);
        properties.getAdmin().setToken("test-token");

        ObjectMapper objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(configService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new AdminAuthFilter(properties, objectMapper))
                .build();
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/admin/rules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void listWithTokenReturns200() throws Exception {
        when(configService.listRules()).thenReturn(List.of());

        mockMvc.perform(get("/admin/rules")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/admin/rules")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "USER",
                                  "algorithm": "token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createWithValidBodyReturns201() throws Exception {
        RateLimitRule created = new RateLimitRule();
        created.setName("new-rule");
        created.setScope(RateLimitScope.USER);
        created.setAlgorithm("token");
        created.setCapacity(10);
        created.setRefillPerSecond(1.0);
        when(configService.createRule(any())).thenReturn(created);

        mockMvc.perform(post("/admin/rules")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "new-rule",
                                  "scope": "USER",
                                  "algorithm": "token",
                                  "capacity": 10,
                                  "refillPerSecond": 1.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("new-rule"));
    }
}
