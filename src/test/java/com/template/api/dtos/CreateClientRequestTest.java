package com.template.api.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.template.api.dtos.client.CreateClientRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateClientRequestTest {

    public static final String JOHN = "John";
    public static final String DOE = "Doe";
    public static final String PHONE = "+12345678901";
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void validRequestHasNoViolations() {
        CreateClientRequest request = new CreateClientRequest(
                JOHN,
                DOE,
                PHONE
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankFirstNameIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                " ",
                DOE,
                PHONE
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("firstName");
                    assertThat(violation.getMessage()).isEqualTo("{validation.firstName.required}");
                });
    }

    @Test
    void blankLastNameIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                JOHN,
                "",
                PHONE
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("lastName");
                    assertThat(violation.getMessage()).isEqualTo("{validation.lastName.required}");
                });
    }

    @Test
    void blankPhoneIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                JOHN,
                DOE,
                ""
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("phone");
                    assertThat(violation.getMessage()).isEqualTo("{validation.phone.required}");
                });
    }

    @Test
    void firstNameOverMaxLengthIsRejected() {
        String tooLong = "a".repeat(101);
        CreateClientRequest request = new CreateClientRequest(
                tooLong,
                DOE,
                PHONE
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath()).hasToString("firstName"));
    }

    @Test
    void lastNameOverMaxLengthIsRejected() {
        String tooLong = "b".repeat(101);
        CreateClientRequest request = new CreateClientRequest(
                JOHN,
                tooLong,
                PHONE
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath()).hasToString("lastName"));
    }

    @Test
    void phonePatternIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                JOHN,
                DOE,
                "12-345"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("phone");
                    assertThat(violation.getMessage()).isEqualTo("{validation.phone.invalid}");
                });
    }

    @Test
    void phonePatternAcceptsMinAndMaxLengths() {
        CreateClientRequest min = new CreateClientRequest(
                JOHN,
                DOE,
                "1234567"
        );
        CreateClientRequest max = new CreateClientRequest(
                JOHN,
                DOE,
                "+123456789012345"
        );

        assertThat(validator.validate(min)).isEmpty();
        assertThat(validator.validate(max)).isEmpty();
    }
}

