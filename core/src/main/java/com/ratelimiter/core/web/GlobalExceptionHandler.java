package com.ratelimiter.core.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ratelimiter.core.dtos.AdminErrorResponse;
import com.ratelimiter.core.utils.RuleConflictException;
import com.ratelimiter.core.utils.RuleNotFoundException;
import com.ratelimiter.core.utils.RuleValidationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuleValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AdminErrorResponse handleRuleValidation(RuleValidationException ex) {
        return AdminErrorResponse.validation(ex.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AdminErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return AdminErrorResponse.validation(fieldErrors);
    }

    @ExceptionHandler(RuleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public AdminErrorResponse handleRuleNotFound(RuleNotFoundException ex) {
        return AdminErrorResponse.of("rule_not_found", ex.getMessage());
    }

    @ExceptionHandler(RuleConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AdminErrorResponse handleRuleConflict(RuleConflictException ex) {
        return AdminErrorResponse.of("rule_conflict", ex.getMessage());
    }
}
