package testAPI;

import api.models.Book;
import api.models.BookDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    void testCreateValidBook() {
        Book book = new Book("1984", "Orwell", "Fiction", new BigDecimal("15.99"));

        assertEquals("1984", book.getTitle());
        assertEquals("Orwell", book.getAuthor());
        assertEquals("Fiction", book.getGenre());
        assertEquals(0, new BigDecimal("15.99").compareTo(book.getPrice()));
        // ID is null until the repository saves it and sets it via KeyHolder.
        // We do NOT test it here.
        assertNull(book.getId()); // This is the correct state after construction.
    }

    @Test
    void testBookDtoValidationConstraints() {
        // 1. Set up the Jakarta Validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // 2. Create an INVALID DTO (empty title, null author, negative price)
        BookDTO invalidDto = new BookDTO("", null, "Fiction", new BigDecimal("-5.0"));

        // 3. Run the validation
        Set<ConstraintViolation<BookDTO>> violations = validator.validate(invalidDto);

        // 4. Assert that validation failed
        assertFalse(violations.isEmpty(), "Expected validation errors but got none");

        // 5. (Optional) Check that the specific error messages are present
        // This is good for ensuring the right rules fired.
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title cannot be empty")),
                "Missing validation error for empty title");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Author cannot be empty")),
                "Missing validation error for null author");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Price must be greater than 0")),
                "Missing validation error for price less than or equal to 0");
    }

    @Test
    void testValidBookDtoPassesValidation() {
        // 1. Set up Validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // 2. Create a fully valid DTO
        BookDTO validDto = new BookDTO("The Hobbit", "J.R.R. Tolkien", "Fantasy", new BigDecimal("12.99"));

        // 3. Validate
        Set<ConstraintViolation<BookDTO>> violations = validator.validate(validDto);

        // 4. Assert that NO violations exist
        assertTrue(violations.isEmpty(), "Expected zero validation errors for a valid DTO, but got: " + violations);
    }

}