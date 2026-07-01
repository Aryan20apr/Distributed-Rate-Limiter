package com.ratelimiter.core.controller;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ratelimiter.core.dtos.CreateRuleRequest;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.dtos.UpdateRuleRequest;
import com.ratelimiter.core.service.RuleConfigService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/rules")
@ConditionalOnBean(RuleConfigService.class)
public class AdminController {

    private final RuleConfigService configService;

    public AdminController(RuleConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public List<RateLimitRule> list() {
        return configService.listRules();
    }

    @GetMapping("/{name}")
    public RateLimitRule get(@PathVariable String name) {
        return configService.getRule(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RateLimitRule create(@Valid @RequestBody CreateRuleRequest request) {
        return configService.createRule(request.toRule());
    }

    @PutMapping("/{name}")
    public RateLimitRule update(
            @PathVariable String name,
            @Valid @RequestBody UpdateRuleRequest request) {
        return configService.updateRule(name, request.toRule(name));
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        configService.deleteRule(name);
    }
}