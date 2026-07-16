package api.manager;

import api.exceptions.BookNotFoundException;
import api.models.Book;
import api.models.BookDTO;
import api.repository.BookRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LibraryManager{
    private final BookRepository repository;


    public LibraryManager(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> getAllBooks(){
        return new ArrayList<Book>(repository.findAll());
    }

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
        Book existingBook = repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Couldn't find book " + id + " ID"));


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
        Book existingBook = repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Couldn't find book " + id + " ID"));

        existingBook.setTitle(updates.getTitle());
        existingBook.setAuthor(updates.getAuthor());
        existingBook.setGenre(updates.getGenre());
        existingBook.setPrice(updates.getPrice());

        repository.save(existingBook);
        return existingBook;
    }

    @Transactional
    public void deleteBookById(Long id) {
        Book bookToRemove = repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Couldn't find book " + id + " ID"));

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
                   return List.of(); // or throw an IllegalArgumentException
               }
           default:
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
        return repository.sumTotalOfPrice();
    }

    public Book findMostExpensiveBook(){
        return repository.findTopByOrderByPriceDesc();
    }


    // Made public so API can use it for validation (works for partial due to .contains)
    public boolean bookMatches(Book book, String type, String value) {
        type = type.trim().toLowerCase();
        value = value.trim().toLowerCase();

        switch (type) {
            case "author":
                // Contains match (partial)
                return book.getAuthor().toLowerCase().contains(value);

            case "title":
                // Contains match (partial)
                return book.getTitle().toLowerCase().contains(value);

            case "genre":
                // Contains match (partial)
                return book.getGenre().toLowerCase().contains(value);

            case "price":
                try {
                    double priceValue = Double.parseDouble(value);
                    return Math.abs(book.getPrice() - priceValue) < 0.0001; // Instead of concrete we use .abs for near accuracy
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return false;
        }
    }
}


