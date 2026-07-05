package api.manager;

import api.models.Book;
import api.models.BookDTO;
import api.repository.BookRepository;
import api.storage.BookStorage;
import api.util.BookIDGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LibraryManager{
    private final BookRepository repository;
    private final BookStorage storage;


    public LibraryManager(BookRepository repository,BookStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @PostConstruct
    public void init() {
        loadFromStorage(); // Load from storage -> repository
    }

    @PreDestroy
    public void destroy(){
        saveToStorage();
    }

    // Load from storage -> repository
    private void loadFromStorage() {
        List<Book> loaded = storage.load(); // Loads storage into loadedList

        // Update nextId based on loaded students
        int maxId = 0;
        for (Book b : loaded) {
            String id = b.getId();
            if (id != null && !id.isEmpty()){ // Null Checker
                int idNum = Integer.parseInt(b.getId()); // Parse Id into IdNum
                if (idNum > maxId) { // the largest loaded ID is the maxID
                    maxId = idNum;
                }
            }
        }
        BookIDGenerator.setNextId(maxId + 1);
        repository.addAll(loaded); // Add to the repository
    }

    // Save from repository -> storage
    private void saveToStorage() {
        List<Book> loaded = repository.getAll();
        storage.save(loaded); // Calls save method of storage
    }


    public List<Book> getAllBooks(){
        return new ArrayList<Book>(repository.getAll());
    }

    public Book findBookById(String id){
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
        saveToStorage();
        return newBook;
    }

    public Book patchBook(String id, BookDTO updates) {
        Book existing = findBookById(id);
        if (existing == null) return null;



        // Update only provided fields, If Null/Not given ignore.
        if (updates.getTitle() != null && !updates.getTitle().trim().isEmpty()) {
            existing.setTitle(updates.getTitle());
        }
        if (updates.getAuthor() != null && !updates.getAuthor().trim().isEmpty()) {
            existing.setAuthor(updates.getAuthor());
        }
        if (updates.getGenre() != null && !updates.getGenre().trim().isEmpty()) {
            existing.setGenre(updates.getGenre());
        }

        if (updates.getPrice() != null) {
            if (updates.getPrice() <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            existing.setPrice(updates.getPrice());
        }

        saveToStorage();
        return existing;
    }

    public boolean deleteBookById(String id) {
        Book bookToRemove = findBookById(id);
        if (bookToRemove != null) {
            repository.remove(bookToRemove);
            saveToStorage();
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
            return new ArrayList<>(repository.getAll());
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
            default:
                return new ArrayList<>(repository.getAll()); // Invalid field, return unsorted
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
        if (repository.getAll().isEmpty()) {
            return null;
        }

        Book mostExpensive = repository.getAll().getFirst();
        for (Book book : repository.getAll()){
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
        if (input.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
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
                    return Math.abs(book.getPrice() - priceValue) < 0.0001;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return false;
        }
    }
}


