package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.ai.web.dto.ChatRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AiRequestValidationTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsBlankMessages() {
        assertTrue(validator.validate(new ChatRequest("  ")).stream()
                .anyMatch(error -> "message".equals(error.getPropertyPath().toString())));
    }

    @Test
    void accepts500CharactersAndRejectsLongerMessages() {
        assertTrue(validator.validate(new ChatRequest("a".repeat(500))).isEmpty());
        assertEquals(1, validator.validate(new ChatRequest("a".repeat(501))).size());
    }
}
