package com.ratelimiter.core.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ratelimiter.core.dtos.RateLimitRule;

public final class RuleOrdering {

    private static final int UNSET_PRIORITY = 0;
    private static final int LEGACY_UNKNOWN_PRIORITY = 1000;
    private static final int PRIORITY_STEP = 10;

    private RuleOrdering() {}

    public static Comparator<RateLimitRule> comparator() {
        return Comparator
                .comparingInt(RateLimitRule::getPriority)
                .thenComparing(RateLimitRule::getName);
    }

    public static List<RateLimitRule> sorted(List<RateLimitRule> rules) {
        List<RateLimitRule> copy = new ArrayList<>(rules);
        copy.sort(comparator());
        return List.copyOf(copy);
    }

    /** Assign (index + 1) * 10 when priority is unset (0). */
    public static void stampFromListOrderIfUnset(List<RateLimitRule> rules) {
        for (int i = 0; i < rules.size(); i++) {
            RateLimitRule rule = rules.get(i);
            if (rule.getPriority() == UNSET_PRIORITY) {
                rule.setPriority((i + 1) * PRIORITY_STEP);
            }
        }
    }

    public static int nextPriority(List<RateLimitRule> existingRules) {
        if (existingRules.isEmpty()) {
            return PRIORITY_STEP;
        }
        int max = existingRules.stream()
                .mapToInt(RateLimitRule::getPriority)
                .max()
                .orElse(0);
        return max + PRIORITY_STEP;
    }

    public static int legacyUnknownPriority() {
        return LEGACY_UNKNOWN_PRIORITY;
    }

    public static boolean isUnset(int priority) {
        return priority == UNSET_PRIORITY;
    }
}
