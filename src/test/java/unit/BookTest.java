package unit;

import app.book.entity.Book;
import app.book.dto.BookRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Book} constructor and the Jakarta Validation contract exposed by
 * {@link BookRequestDTO}.
 *
 * <p>This suite has no mocked dependencies. Entity construction is tested as a plain Java
 * operation, while DTO constraints are exercised with a real Jakarta {@link Validator}. No
 * Spring application context, repository, or database is started.</p>
 */
class BookTest {

    /**
     * Verifies that the convenience constructor copies every supplied value and leaves the
     * generated identifier {@code null} until persistence assigns it.
     */
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

    /**
     * Verifies that blank titles, missing authors, and non-positive prices each produce their
     * documented validation message on an invalid request DTO.
     */
    @Test
    void testBookDtoValidationConstraints() {
        // 1. Set up the Jakarta Validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // 2. Create an INVALID DTO (empty title, null author, negative price)
        BookRequestDTO invalidDto = new BookRequestDTO("", null, "Fiction", new BigDecimal("-5.0"));

        // 3. Run the validation
        Set<ConstraintViolation<BookRequestDTO>> violations = validator.validate(invalidDto);

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

    /**
     * Verifies that a complete DTO satisfying every constraint produces no violations.
     */
    @Test
    void testValidBookDtoPassesValidation() {
        // 1. Set up Validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // 2. Create a fully valid DTO
        BookRequestDTO validDto = new BookRequestDTO("The Hobbit", "J.R.R. Tolkien", "Fantasy", new BigDecimal("12.99"));

        // 3. Validate
        Set<ConstraintViolation<BookRequestDTO>> violations = validator.validate(validDto);

        // 4. Assert that NO violations exist
        assertTrue(violations.isEmpty(), "Expected zero validation errors for a valid DTO, but got: " + violations);
    }

}
