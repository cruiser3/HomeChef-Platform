package com.fooddelivery.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AddShoppingItemRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void rejectsAmountsWithMoreThanTwoDecimalPlaces() {
        AddShoppingItemRequest request = new AddShoppingItemRequest();
        request.setName("Flour");
        request.setAmount(new BigDecimal("1.234"));
        request.setUnit("kg");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void acceptsAmountsWithTwoDecimalPlaces() {
        AddShoppingItemRequest request = new AddShoppingItemRequest();
        request.setName("Flour");
        request.setAmount(new BigDecimal("1.23"));
        request.setUnit("kg");

        assertTrue(validator.validate(request).isEmpty());
    }
}
