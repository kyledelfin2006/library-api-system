package app.book.service;

import app.book.exceptions.BookNotFoundException;
import app.book.entity.Book;
import app.book.dto.BookRequestDTO;
import app.book.mapper.BookMapper;
import app.book.repository.BookRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookService {
    private final BookRepository repository;
    private final BookMapper mapper;


    public BookService(BookRepository repository, BookMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<Book> getBooks(Pageable pageable){
        return repository.findAll(pageable);
    }

    @Transactional
    public Book addBook(BookRequestDTO input){
        Book newBook = new Book(
                input.getTitle(),
                input.getAuthor(),
                input.getGenre(),
                input.getPrice());

        repository.save(newBook);
        return newBook;
    }

    public Book findBookById(Long id){
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException("Couldn't find book of ID: " + id ));
    }

    // Partial updates
    @Transactional
    public Book patchBook(Long id, BookRequestDTO updates) {
        Book existingBook = findBookById(id);

        if (hasText(updates.getTitle())) {
            existingBook.setTitle(updates.getTitle().trim());
        }
        if (hasText(updates.getAuthor())) {
            existingBook.setAuthor(updates.getAuthor().trim());
        }
        if (hasText(updates.getGenre())) {
            existingBook.setGenre(updates.getGenre().trim());
        }
        if (updates.getPrice() != null) {
            if (updates.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            existingBook.setPrice(updates.getPrice());
        }

        return existingBook; // no repository.save() needed for Transactional methods.
    }

    // Checks whether a string has text and is not null
    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // Replaces entire book
    @Transactional
    public Book replaceBook(Long id, BookRequestDTO updates) {
        // 1. Fetch the existing book (throws 404 if not found)
        Book existingBook = findBookById(id); // Loaded from DB

        // 2. Update entire entity using BookMapper
        mapper.updateBookFromDto(updates, existingBook);

        // 3. Persist and return managed book entity through dirty checking
        return  existingBook;
        }

    @Transactional
    public void deleteBookById(Long id) {
        int deletedCount = repository.deleteBookById(id); // Returns the amount of rows deleted
        if (deletedCount == 0){ // If no row is deleted, it means no id matched that book to delete.
            throw new  BookNotFoundException("Couldn't find book of ID: " + id);
        }
    }

    public List<Book> getBooksWithinBudget(BigDecimal maxPrice) {
        return repository.findByPriceLessThanEqual(maxPrice);
    }

    public List<Book> getBooksInPriceRange(String minPrice, String maxPrice) {
        try{
            BigDecimal min = new BigDecimal(minPrice);
            BigDecimal max = new BigDecimal(maxPrice);
            return repository.findBooksByPriceBetween(min,max);
        } catch (NumberFormatException e){
            throw new NumberFormatException("Price must be a number.");
        }
    }

    public List<Book> searchBooks(String type, String value) {
       String formattedType = type.trim().toLowerCase();

       switch (formattedType) {
           case "author":
               return repository.findByAuthorContainingIgnoreCase(value);
           case "title":
               return repository.findByTitleContainingIgnoreCase(value);
           case "genre":
               return repository.findByGenreContainingIgnoreCase(value);
           case  "price":
               try {
                   BigDecimal price = new BigDecimal(value);
                   return repository.findByPrice(price);
               } catch (NumberFormatException e) {
               throw new IllegalArgumentException("Invalid price format value:" + value);
               }
           default: // Handler for invalid types
               throw new IllegalArgumentException("Invalid search type: " + type + ". Valid types: author, title, genre, price");
       }
    }

    /**
     * Retrieves a list of genre–count pairs from the repository and converts it into a map.
     *
     * <p>The repository method {@code getGenres()} returns a {@code List<Object[]>} where
     * each {@code Object[]} contains exactly two elements: the genre (as a {@code String})
     * and the count of books in that genre (as a {@code Long}).
     *
     * <p>The underlying query is:
     * {@code SELECT b.genre, COUNT(b) FROM Book b GROUP BY b.genre}.
     *
     * <p>This method uses {@link Collectors#toMap} to build a {@code Map<String, Long>}
     * where the key is the genre (converted to {@code String}) and the value is the count
     * (converted to {@code Long}).
     *
     * @return a map containing genre names as keys and their corresponding book counts as values
     * @throws NullPointerException if any genre is {@code null} (consider filtering before collecting)
     */
    public Map<String,Long> getGenreDistribution(){
        return repository.getGenres().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // typecasting to appropriate data type
                        row -> (Long) row[1] // Maps the row in the getGenres() array list
            ));
    }

    public List<Book> getBooksSortedBy(String field) {
        if (field == null || field.trim().isEmpty()) {
            return repository.findAll();
        }

        String fieldName = field.trim().toLowerCase();

        // Validate allowed fields to avoid SQL injection through Sort.by()
        Set<String> allowedFields = Set.of("title", "author", "id", "price", "genre");

        if (!allowedFields.contains(fieldName)) {
            throw new IllegalArgumentException("Invalid sort field: " + field);
        }

        Sort sort = Sort.by(fieldName).ascending();
        return repository.findAll(sort);
    }

    public BigDecimal getTotalLibraryValue() {
        return repository.sumTotalOfPrice().orElse(BigDecimal.ZERO);
    }

    public Long countBooks(){
        return repository.count();
    }

    public Book findMostExpensiveBook(){
        return repository.findTopByOrderByPriceDesc();
    }
}


