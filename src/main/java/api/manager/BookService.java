package api.manager;

import api.exceptions.BookNotFoundException;
import api.models.Book;
import api.models.BookDTO;
import api.repository.BookRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BookService {
    private final BookRepository repository;


    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public Page<Book> getBooks(Pageable pageable){
        return repository.findAll(pageable);
    }

    @Transactional
    public Book addBook(BookDTO input){
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

    @Transactional
    public Book patchBook(Long id, BookDTO updates) {
        Book existingBook = findBookById(id);

        // Update only provided fields, If Null/Not given ignore.
        if (updates.getTitle() != null && !updates.getTitle().trim().isEmpty()) {
            existingBook.setTitle(updates.getTitle());
        }
        if (updates.getAuthor() != null && !updates.getAuthor().trim().isEmpty()) {
            existingBook.setAuthor(updates.getAuthor());
        }
        if (updates.getGenre() != null && !updates.getGenre().trim().isEmpty()) {
            existingBook.setGenre(updates.getGenre());
        }

        if (updates.getPrice() != null) {
            if (updates.getPrice() <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            existingBook.setPrice(updates.getPrice());
        }
        repository.save(existingBook);
        return existingBook;
    }

    @Transactional
    public Book replaceBook(Long id, BookDTO updates) {
        // 1. Fetch the existing book (throws 404 if not found)
        Book existingBook = findBookById(id); // Loaded from DB

        // 2. Validate that the DTO contains all required fields
        //    (Even though @Valid in the controller ensures this, we keep a defensive check.)
        if (updates == null) {
            throw new IllegalArgumentException("Book data must not be null");
        }
        if (updates.getTitle() == null || updates.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (updates.getAuthor() == null || updates.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be empty");
        }
        if (updates.getGenre() == null || updates.getGenre().trim().isEmpty()) {
            throw new IllegalArgumentException("Genre cannot be empty");
        }
        if (updates.getPrice() == null || updates.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        // 3. Apply all updates
        existingBook.setTitle(updates.getTitle());
        existingBook.setAuthor(updates.getAuthor());
        existingBook.setGenre(updates.getGenre());
        existingBook.setPrice(updates.getPrice());

        // 4. Persist and return the managed entity
        return repository.save(existingBook);
    }

    @Transactional
    public void deleteBookById(Long id) {
        Book bookToRemove = findBookById(id);
        repository.delete(bookToRemove);
    }

    public List<Book> getBooksWithinBudget(double maxPrice) {
        return repository.findByPriceLessThanEqual(maxPrice);
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
                   double price = Double.parseDouble(value);
                   double tolerance = 0.0001;
                   return repository.findBooksByPriceBetween(price - tolerance, price + tolerance);
               } catch (NumberFormatException e) {
               throw new IllegalArgumentException("Invalid price format value:" + value);
               }
           default: // Handler for invalid types
               throw new IllegalArgumentException("Invalid search type: " + type + ". Valid types: author, title, genre, price");
       }
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

    public Double getTotalLibraryValue() {
        return repository.sumTotalOfPrice().orElse(0.0);
    }

    public Long countBooks(){
        return repository.count();
    }

    public Book findMostExpensiveBook(){
        return repository.findTopByOrderByPriceDesc();
    }
}


