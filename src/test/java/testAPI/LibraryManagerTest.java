package testAPI;

import api.manager.LibraryManager;
import api.models.Book;
import api.models.BookDTO;
import api.repository.BookRepository;
import api.storage.BookStorage;
import api.util.BookIDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

    @Mock
    private BookRepository repository;

    @Mock
    private BookStorage storage;

    private LibraryManager manager;

    private final Book book1 = new Book("1984", "George Orwell", "Dystopian", 12.99);
    private final Book book2 = new Book("Brave New World", "Aldous Huxley", "Dystopian", 14.99);
    private final Book book3 = new Book("The Hobbit", "J.R.R. Tolkien", "Fantasy", 10.50);

    @BeforeEach
    void setUp() {
        // Reset ID generator to a known state for repeatable tests
        BookIDGenerator.setNextId(0);

        // Create manager with mocked dependencies – no need to stub storage.load()
        // because mocks return empty collections by default.
        manager = new LibraryManager(repository, storage);
    }

    // Helper to initialise repository with a list of books
    private void initRepositoryWithBooks(List<Book> books) {
        // Reset the mock to clear previous interactions and stubbings
        reset(repository);
        // When getAll is called, return the given list
        when(repository.getAll()).thenReturn(books);
    }

    @Test
    void addBook_ValidInput_AddsBookAndSaves() {
        // Arrange
        initRepositoryWithBooks(List.of()); // Empty repository
        BookDTO input = new BookDTO("Fahrenheit 451", "Ray Bradbury", "Dystopian", 15.99);

        // Act
        Book added = manager.addBook(input);

        // Assert – book added to repository
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(repository).add(bookCaptor.capture());
        Book captured = bookCaptor.getValue();
        assertNotNull(captured.getId());
        assertEquals(input.getTitle(), captured.getTitle());
        assertEquals(input.getAuthor(), captured.getAuthor());
        assertEquals(input.getGenre(), captured.getGenre());
        assertEquals(input.getPrice(), captured.getPrice());

        // Verify storage save was called once
        verify(storage).save(anyList());

        // Verify returned book matches
        assertEquals(captured, added);
    }

    @Test
    void addBook_InvalidInput_ThrowsIllegalArgumentException() {
        List<BookDTO> invalidInputs = List.of(
                new BookDTO("", "Author", "Genre", 10.0),       // empty title
                new BookDTO("Title", null, "Genre", 10.0),      // null author
                new BookDTO("Title", "Author", "Genre", 0.0),   // zero price
                new BookDTO("Title", "Author", "Genre", -5.0)   // negative price
        );

        for (BookDTO input : invalidInputs) {
            assertThrows(IllegalArgumentException.class, () -> manager.addBook(input));
        }

        // Verify no interaction with repository or storage
        verify(repository, never()).add(any());
        verify(storage, never()).save(anyList());
    }

    @Test
    void getAllBooks_ReturnsAllBooks() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        List<Book> result = manager.getAllBooks();

        assertEquals(3, result.size());
        assertTrue(result.containsAll(books));
        verify(repository).getAll();
    }

    @Test
    void findBookById_Existing_ReturnsBook() {
        List<Book> books = List.of(book1, book2);
        initRepositoryWithBooks(books);

        Book found = manager.findBookById(book1.getId());
        assertNotNull(found);
        assertEquals(book1, found);

        found = manager.findBookById(book2.getId());
        assertNotNull(found);
        assertEquals(book2, found);
    }

    @Test
    void findBookById_NonExisting_ReturnsNull() {
        initRepositoryWithBooks(List.of(book1, book2));
        Book found = manager.findBookById("9999");
        assertNull(found);
    }

    @Test
    void deleteBookById_Existing_DeletesAndSaves() {
        List<Book> books = new ArrayList<>(List.of(book1, book2));
        initRepositoryWithBooks(books);

        boolean deleted = manager.deleteBookById(book1.getId());

        assertTrue(deleted);
        verify(repository).remove(book1);
        verify(storage).save(anyList());
    }

    @Test
    void deleteBookById_NonExisting_ReturnsFalseAndDoesNotSave() {
        initRepositoryWithBooks(List.of(book1));

        boolean deleted = manager.deleteBookById("9999");

        assertFalse(deleted);
        verify(repository, never()).remove(any());
        verify(storage, never()).save(anyList());
    }

    @Test
    void patchBook_ExistingBook_UpdatesFieldsAndSaves() {
        List<Book> books = new ArrayList<>(List.of(book1));
        initRepositoryWithBooks(books);

        // Update title and price
        BookDTO updates = new BookDTO("Nineteen Eighty-Four", null, null, 13.99);
        Book updated = manager.patchBook(book1.getId(), updates);

        assertNotNull(updated);
        assertEquals("Nineteen Eighty-Four", updated.getTitle());
        assertEquals(13.99, updated.getPrice());
        assertEquals(book1.getAuthor(), updated.getAuthor()); // unchanged
        assertEquals(book1.getGenre(), updated.getGenre());   // unchanged

        verify(storage).save(anyList());
    }

    @Test
    void patchBook_ExistingBook_PriceValidationThrows() {
        List<Book> books = new ArrayList<>(List.of(book1));
        initRepositoryWithBooks(books);

        List<Double> invalidPrices = List.of(-5.0, 0.0);
        for (Double price : invalidPrices) {
            BookDTO updates = new BookDTO(null, null, null, price);
            assertThrows(IllegalArgumentException.class, () -> manager.patchBook(book1.getId(), updates));
        }

        verify(storage, never()).save(anyList());
    }

    @Test
    void patchBook_NonExisting_ReturnsNull() {
        initRepositoryWithBooks(List.of(book1));

        BookDTO updates = new BookDTO("New Title", null, null, 10.0);
        Book result = manager.patchBook("9999", updates);

        assertNull(result);
        verify(storage, never()).save(anyList());
    }

    @Test
    void getBooksWithinBudget_ReturnsBooksUnderOrEqual() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        List<Book> budgetBooks = manager.getBooksWithinBudget(12.99);
        assertEquals(2, budgetBooks.size()); // book1 and book3
        assertTrue(budgetBooks.contains(book1));
        assertTrue(budgetBooks.contains(book3));
        assertFalse(budgetBooks.contains(book2));
    }

    @Test
    void getBooksWithinBudget_EmptyBudget_ReturnsEmptyList() {
        initRepositoryWithBooks(List.of(book1, book2));
        List<Book> result = manager.getBooksWithinBudget(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchBooks_ByAuthor_ReturnsMatchingBooks() {
        List<Book> books = List.of(book1, book2);
        initRepositoryWithBooks(books);

        List<Book> result = manager.searchBooks("author", "Orwell");
        assertEquals(1, result.size());
        assertEquals(book1, result.getFirst());

        result = manager.searchBooks("author", "Huxley");
        assertEquals(1, result.size());
        assertEquals(book2, result.getFirst());

        result = manager.searchBooks("author", "Tolkien");
        assertTrue(result.isEmpty());
    }

    @Test
    void searchBooks_ByTitle_PartialMatch() {
        List<Book> books = List.of(book1, book2);
        initRepositoryWithBooks(books);

        List<Book> result = manager.searchBooks("title", "1984");
        assertEquals(1, result.size());
        assertEquals(book1, result.getFirst());

        result = manager.searchBooks("title", "Brave");
        assertEquals(1, result.size());
        assertEquals(book2, result.getFirst());

        result = manager.searchBooks("title", "World");
        assertEquals(1, result.size());
        assertEquals(book2, result.getFirst());
    }

    @Test
    void searchBooks_ByGenre_Matches() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        List<Book> result = manager.searchBooks("genre", "Dystopian");
        assertEquals(2, result.size());
        assertTrue(result.contains(book1));
        assertTrue(result.contains(book2));
        assertFalse(result.contains(book3));
    }

    @Test
    void searchBooks_ByPrice_ExactMatchWithTolerance() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        List<Book> result = manager.searchBooks("price", "12.99");
        assertEquals(1, result.size());
        assertEquals(book1, result.getFirst());

        result = manager.searchBooks("price", "14.99");
        assertEquals(1, result.size());
        assertEquals(book2, result.getFirst());

        result = manager.searchBooks("price", "15.00"); // No book with that price
        assertTrue(result.isEmpty());
    }

    @Test
    void getBooksSortedBy_DefaultTitle_ReturnsSorted() {
        List<Book> books = List.of(book2, book1, book3); // unsorted order
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("title");
        assertEquals(3, sorted.size());
        assertEquals(book1, sorted.get(0)); // "1984"
        assertEquals(book2, sorted.get(1)); // "Brave New World"
        assertEquals(book3, sorted.get(2)); // "The Hobbit"
    }

    @Test
    void getBooksSortedBy_Author_ReturnsSorted() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("author");
        assertEquals(3, sorted.size());
        assertEquals(book2, sorted.get(0)); // Aldous Huxley
        assertEquals(book1, sorted.get(1)); // George Orwell
        assertEquals(book3, sorted.get(2)); // J.R.R. Tolkien
    }

    @Test
    void getBooksSortedBy_Price_ReturnsSorted() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("price");
        assertEquals(3, sorted.size());
        assertEquals(book3, sorted.get(0)); // 10.50
        assertEquals(book1, sorted.get(1)); // 12.99
        assertEquals(book2, sorted.get(2)); // 14.99
    }

    @Test
    void getBooksSortedBy_Id_ReturnsSortedById() {
        // IDs are generated sequentially; we'll create books in a specific order
        Book b1 = new Book("A", "A", "A", 1.0);
        Book b2 = new Book("B", "B", "B", 2.0);
        Book b3 = new Book("C", "C", "C", 3.0);
        List<Book> books = List.of(b2, b1, b3); // shuffled
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("id");
        // IDs are assigned in creation order: b1->0000, b2->0001, b3->0002
        // So sorted by ID should be b1, b2, b3
        assertEquals(3, sorted.size());
        assertEquals(b1, sorted.get(0));
        assertEquals(b2, sorted.get(1));
        assertEquals(b3, sorted.get(2));
    }

    @Test
    void getBooksSortedBy_InvalidField_ReturnsUnsortedCopy() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("invalid");
        // Should return a copy in the same order as repository.getAll()
        assertEquals(books, sorted);
        assertNotSame(books, sorted); // Should be a copy
    }

    @Test
    void getTotalLibraryValue_ReturnsSum() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        double total = manager.getTotalLibraryValue();
        double expected = 12.99 + 14.99 + 10.50;
        assertEquals(expected, total, 0.001);
    }

    @Test
    void getTotalLibraryValue_Empty_ReturnsZero() {
        initRepositoryWithBooks(List.of());
        assertEquals(0.0, manager.getTotalLibraryValue());
    }

    @Test
    void findMostExpensiveBook_ReturnsHighestPrice() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        Book mostExpensive = manager.findMostExpensiveBook();
        assertEquals(book2, mostExpensive); // 14.99
    }

    @Test
    void findMostExpensiveBook_Empty_ReturnsNull() {
        initRepositoryWithBooks(List.of());
        assertNull(manager.findMostExpensiveBook());
    }

    @Test
    void bookMatches_TestDirectlyIfNeeded() {
        // This is a public method; we can test it independently.
        Book book = new Book("The Lord of the Rings", "J.R.R. Tolkien", "Fantasy", 25.0);
        assertTrue(manager.bookMatches(book, "title", "Lord"));
        assertTrue(manager.bookMatches(book, "title", "rings"));
        assertTrue(manager.bookMatches(book, "author", "Tolkien"));
        assertTrue(manager.bookMatches(book, "genre", "Fantasy"));
        assertTrue(manager.bookMatches(book, "price", "25"));
        assertFalse(manager.bookMatches(book, "price", "30"));
        // "Tolki" is a substring of "Tolkien", so it should return true.
        assertTrue(manager.bookMatches(book, "author", "Tolki"));
    }
}