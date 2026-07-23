package app.book.mapper;

import app.book.dto.BookRequestDTO;
import app.book.dto.BookResponseDTO;
import app.book.entity.Book;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookMapper {


    /**
     * Converts a Book entity to a BookResponseDTO.
     *
     * @param book the entity to convert (maybe null)
     * @return the response DTO, or null if input is null
     */
    public BookResponseDTO toResponseDTO(Book book) {
        if (book == null) {
            return null;
        }

        return new BookResponseDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPrice()
        );
    }

    /**
     * Converts a BookRequestDTO to a new Book entity.
     * Useful for create operations. (i.e. POST)
     */
    public Book toEntity(BookRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Book(
                dto.getTitle(),
                dto.getAuthor(),
                dto.getGenre(),
                dto.getPrice()
        );
    }

    /**
     * Updates an existing BookEntity from a BookRequestDTO.
     * Useful for changing entire entities. (i.e. PUT)
     */
    public void updateBookFromDto(BookRequestDTO dto, Book existingBook) {
        if (dto == null) return;

        existingBook.setTitle(dto.getTitle());
        existingBook.setAuthor(dto.getAuthor());
        existingBook.setGenre(dto.getGenre());
        existingBook.setPrice(dto.getPrice());
    }




}
