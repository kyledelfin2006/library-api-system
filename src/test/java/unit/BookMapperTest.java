package unit;

import app.book.dto.BookResponseDTO;
import app.book.entity.Book;
import app.book.mapper.BookMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookMapperTest {

    @Test
    void mapsCreatedAtToResponseDto() {
        Book book = new Book("The Hobbit", "J.R.R. Tolkien", "Fantasy", new BigDecimal("12.99"));
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 16, 30);
        book.setId(1L);
        book.setCreatedAt(createdAt);

        BookResponseDTO response = new BookMapper().toResponseDTO(book);

        assertEquals(createdAt, response.getCreatedAt());
    }
}
