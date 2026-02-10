package com.example.aiarticlesummarizer.api.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SummarizeRequestValidator implements ConstraintValidator<ValidSummarizeRequest, SummarizeRequest> {

    @Override
    public void initialize(ValidSummarizeRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(SummarizeRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasUrl = request.getUrl() != null && !request.getUrl().isBlank();

        if (!hasContent && !hasUrl) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Either 'content' or 'url' must be provided")
                    .addPropertyNode("content")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
