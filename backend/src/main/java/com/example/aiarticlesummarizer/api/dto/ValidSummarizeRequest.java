package com.example.aiarticlesummarizer.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SummarizeRequestValidator.class)
@Documented
public @interface ValidSummarizeRequest {
    String message() default "Either 'content' or 'url' must be provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
