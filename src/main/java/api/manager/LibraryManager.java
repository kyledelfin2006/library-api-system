package api.manager;

import api.exceptions.BookNotFoundException;
import api.models.Book;
import api.models.BookDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LibraryManager{
    private final BaseRepository<Book> repository;


    public LibraryManager(BaseRepository<Book> repository) {
        this.repository = repository;
    }

    public List<Book> getAllBooks(){
        return new ArrayList<Book>(repository.getAll());
    }

    public Book findBookById(Long id){
        return repository.getAll().stream().filter(book -> book.getId().equals(id)).findFirst().orElse(null);
    }

    public Book addBook(BookDTO input){
        validateBookInput(input); // Validate before Adding book
        Book newBook = new Book(
                input.getTitle(),
                input.getAuthor(),
                input.getGenre(),
                input.getPrice());

        repository.add(newBook);
        return newBook;
    }

    public Book patchBook(Long id, BookDTO updates) {
        Book existingBook = findBookById(id);
        if (existingBook == null){
            throw new BookNotFoundException("Couldn't find book of that ID");
        }

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
        repository.update(existingBook);
        return existingBook;
    }

    public Book replaceBook(Long id, BookDTO updates) {
        Book existingBook = findBookById(id);
        if (existingBook == null){
            throw new BookNotFoundException("Couldn't find book of that ID");
        }

        validateBookInput(updates);

        existingBook.setTitle(updates.getTitle());
        existingBook.setAuthor(updates.getAuthor());
        existingBook.setGenre(updates.getGenre());
        existingBook.setPrice(updates.getPrice());

        repository.update(existingBook);
        return existingBook;
    }

    public boolean deleteBookById(Long id) {
        Book bookToRemove = findBookById(id);
        if (bookToRemove != null) {
            repository.remove(bookToRemove);
            return true;
        }
        return false;
    }

    public List<Book> getBooksWithinBudget(double maxPrice) {
        return repository.getAll()
                .stream()
                .filter(b -> b.getPrice() <= maxPrice)
                .toList();
    }


    public List<Book> searchBooks(String type, String value) {
        List<Book> results = new ArrayList<>();
        for (Book book : repository.getAll()) {
            if (bookMatches(book, type, value)) {
                results.add(book);
            }
        }
        return results;
    }

    public List<Book> getBooksSortedBy(String field) {
        if (field == null || field.trim().isEmpty()) {
            return List.copyOf(repository.getAll());
        }

        List<Book> sortedCopy = new ArrayList<>(repository.getAll());
        field = field.trim().toLowerCase();

        switch (field) {
            case "title":
                sortedCopy.sort(Comparator.comparing(Book::getTitle));
                break;
            case "author":
                sortedCopy.sort(Comparator.comparing(Book::getAuthor));
                break;
            case "id":
                sortedCopy.sort(Comparator.comparing(Book::getId));
                break;
            case "price":
                sortedCopy.sort(Comparator.comparing(Book::getPrice));
                break;
            case "genre":
                sortedCopy.sort(Comparator.comparing(Book::getGenre));
                break;
        }
        return sortedCopy;
    }

    public double getTotalLibraryValue() {
        double total = 0;
        for (Book book : repository.getAll()) {
            total += book.getPrice();
        }
        return total;
    }

    public Book findMostExpensiveBook(){
        List<Book> allBooks = repository.getAll();

        if (allBooks.isEmpty()) {
            return null;
        }

        Book mostExpensive = allBooks.getFirst();
        for (Book book : allBooks) {
            if (book.getPrice() > mostExpensive.getPrice()) {
                mostExpensive = book;
            }
        }
        return mostExpensive;
    }

    // ===== HELPER METHODS =====
    private void validateBookInput(BookDTO input) {
        if (input.getTitle() == null || input.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty or null");
        }
        if (input.getAuthor() == null || input.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be empty or null");
        }
        if (input.getGenre() == null || input.getGenre().trim().isEmpty()) {
            throw new IllegalArgumentException("Genre cannot be empty or null");
        }
        if (input.getPrice() == null || input.getPrice() <= 0) {
            throw new IllegalArgumentException("Price cannot be empty or null and must be greater than 0");
        }
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


