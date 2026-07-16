package testAPI;

import api.models.Book;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    void testCreateValidBook() {
        Book book = new Book("1984", "Orwell", "Fiction", 15.99);

        assertEquals("1984", book.getTitle());
        assertEquals("Orwell", book.getAuthor());
        assertEquals("Fiction", book.getGenre());
        assertEquals(15.99, book.getPrice());
        // ID is null until the repository saves it and sets it via KeyHolder.
        // We do NOT test it here.
        assertNull(book.getId()); // This is the correct state after construction.
    }

    @Test
    void testEmptyTitleThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Book("", "Orwell", "Fiction", 15.99);
        });
        assertTrue(exception.getMessage().contains("Title cannot be empty"));
    }

    @Test
    void testNullAuthorThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Book("1984", null, "Fiction", 15.99);
        });
    }

    @Test
    void testNegativePriceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Book("1984", "Orwell", "Fiction", -5.00);
        });
    }

    @Test
    void testZeroPriceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Book("1984", "Orwell", "Fiction", 0);
        });
    }
    // ID generation is now handled by PostgreSQL, not the constructor.
}