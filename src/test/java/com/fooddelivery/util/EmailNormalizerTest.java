package com.fooddelivery.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailNormalizerTest {

    @Test
    void trimsWhitespaceAndNormalizesCaseUsingLocaleIndependentRules() {
        assertEquals("user@example.com", EmailNormalizer.normalize("  User@Example.Com  "));
    }
}
