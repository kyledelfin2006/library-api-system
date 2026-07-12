package testAPI;

import api.exceptions.BookNotFoundException;
import api.manager.LibraryManager;
import api.models.Book;
import api.models.BookDTO;
import api.repository.BaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

    @Mock
    private BaseRepository<Book> repository;

    private LibraryManager manager;

    // We manually assign IDs now because the Book constructor doesn't generate them.
    private final Book book1 = new Book("1984", "George Orwell", "Dystopian", 12.99);
    private final Book book2 = new Book("Brave New World", "Aldous Huxley", "Dystopian", 14.99);
    private final Book book3 = new Book("The Hobbit", "J.R.R. Tolkien", "Fantasy", 10.50);

    @BeforeEach
    void setUp() {
        // Manually set IDs because the database (PostgreSQL) would generate them,
        // but in our unit tests, we're mocking the repository, so we must provide fake IDs.
        book1.setId(1L);
        book2.setId(2L);
        book3.setId(3L);

        // Create manager with ONLY the repository mock
        manager = new LibraryManager(repository);
    }

    // Helper to initialize repository with a list of books
    private void initRepositoryWithBooks(List<Book> books) {
        reset(repository);
        when(repository.getAll()).thenReturn(books);
    }

    // ===================== ADD BOOK TESTS =====================

    @Test
    void addBook_ConcurrentAdds_AllSucceed() throws InterruptedException {
        // given: empty repository mock
        reset(repository);

        int threadCount = 10;
        List<BookDTO> inputs = IntStream.range(0, threadCount)
                .mapToObj(i -> new BookDTO("Book" + i, "Author" + i, "Genre" + i, 10.0 + i))
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Book>> futures = new ArrayList<>();

        // when: all threads submit a book concurrently
        for (BookDTO input : inputs) {
            futures.add(executor.submit(() -> manager.addBook(input)));
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(finished, "Tasks did not complete in time");

        // then: no exceptions were thrown
        for (Future<Book> future : futures) {
            assertDoesNotThrow(() -> future.get());
        }

        // verify: repository.add was called exactly threadCount times
        verify(repository, times(threadCount)).add(any(Book.class));

        // optional: capture all added books and check their data
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(repository, times(threadCount)).add(captor.capture());
        List<Book> capturedBooks = captor.getAllValues();

        Set<String> expectedTitles = inputs.stream()
                .map(BookDTO::getTitle)
                .collect(Collectors.toSet());
        Set<String> actualTitles = capturedBooks.stream()
                .map(Book::getTitle)
                .collect(Collectors.toSet());

        assertEquals(expectedTitles, actualTitles, "All book titles should be present");
    }


    @Test
    void addBook_InvalidInput_ThrowsIllegalArgumentException() {
        List<BookDTO> invalidInputs = List.of(
                new BookDTO("", "Author", "Genre", 10.0),
                new BookDTO("Title", null, "Genre", 10.0),
                new BookDTO("Title", "Author", "Genre", 0.0),
                new BookDTO("Title", "Author", "Genre", -5.0)
        );

        for (BookDTO input : invalidInputs) {
            assertThrows(IllegalArgumentException.class, () -> manager.addBook(input));
        }

        verify(repository, never()).add(any());
    }

    // ===================== GET / FIND TESTS =====================

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

        // book1.getId() is now 1L (Long)
        Book found = manager.findBookById(book1.getId()); // 1L
        assertNotNull(found);
        assertEquals(book1, found);

        found = manager.findBookById(book2.getId()); // 2L
        assertNotNull(found);
        assertEquals(book2, found);
    }

    @Test
    void findBookById_NonExisting_ReturnsNull() {
        initRepositoryWithBooks(List.of(book1, book2));
        // Changed from "9999" to 9999L (Long)
        Book found = manager.findBookById(9999L);
        assertNull(found);
    }

    // ===================== DELETE TESTS =====================

    @Test
    void deleteBookById_Existing_DeletesAndSaves() {
        List<Book> books = new ArrayList<>(List.of(book1, book2));
        initRepositoryWithBooks(books);

        // book1.getId() is 1L
        boolean deleted = manager.deleteBookById(book1.getId());

        assertTrue(deleted);
        verify(repository).remove(book1);
        // REMOVED: verify(storage).save(anyList());  -- BookStorage is GONE!
    }

    @Test
    void deleteBookById_NonExisting_ReturnsFalseAndDoesNotSave() {
        initRepositoryWithBooks(List.of(book1));

        // Changed from "9999" to 9999L
        boolean deleted = manager.deleteBookById(9999L);

        assertFalse(deleted);
        verify(repository, never()).remove(any());
        // REMOVED: verify(storage, never()).save(anyList());
    }

    // ===================== PATCH TESTS =====================

    @Test
    void patchBook_ExistingBook_UpdatesFieldsAndSaves() {
        List<Book> books = new ArrayList<>(List.of(book1));
        initRepositoryWithBooks(books);

        BookDTO updates = new BookDTO("Nineteen Eighty-Four", null, null, 13.99);
        // book1.getId() is 1L
        Book updated = manager.patchBook(book1.getId(), updates);

        assertNotNull(updated);
        assertEquals("Nineteen Eighty-Four", updated.getTitle());
        assertEquals(13.99, updated.getPrice());
        assertEquals(book1.getAuthor(), updated.getAuthor());
        assertEquals(book1.getGenre(), updated.getGenre());

        // REMOVED: verify(storage).save(anyList());
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

        // REMOVED: verify(storage, never()).save(anyList());
    }

    @Test
    void patchBook_NonExisting_ThrowsBookNotFoundException() {
        initRepositoryWithBooks(List.of(book1));
        BookDTO updates = new BookDTO("New Title", null, null, 10.0);
        assertThrows(BookNotFoundException.class, () -> manager.patchBook(9999L, updates));
    }


    // ===================== BUDGET & SEARCH TESTS =====================
    // (These are unchanged because they don't use IDs)

    @Test
    void getBooksWithinBudget_ReturnsBooksUnderOrEqual() {
        List<Book> books = List.of(book1, book2, book3);
        initRepositoryWithBooks(books);

        List<Book> budgetBooks = manager.getBooksWithinBudget(12.99);
        assertEquals(2, budgetBooks.size());
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

        result = manager.searchBooks("price", "15.00");
        assertTrue(result.isEmpty());
    }

    // ===================== SORTING TESTS =====================

    @Test
    void getBooksSortedBy_DefaultTitle_ReturnsSorted() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("title");
        assertEquals(3, sorted.size());
        assertEquals(book1, sorted.get(0));
        assertEquals(book2, sorted.get(1));
        assertEquals(book3, sorted.get(2));
    }

    @Test
    void getBooksSortedBy_Author_ReturnsSorted() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("author");
        assertEquals(3, sorted.size());
        assertEquals(book2, sorted.get(0));
        assertEquals(book1, sorted.get(1));
        assertEquals(book3, sorted.get(2));
    }

    @Test
    void getBooksSortedBy_Price_ReturnsSorted() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("price");
        assertEquals(3, sorted.size());
        assertEquals(book3, sorted.get(0));
        assertEquals(book1, sorted.get(1));
        assertEquals(book2, sorted.get(2));
    }

    @Test
    void getBooksSortedBy_Id_ReturnsSortedById() {
        // Since we manually assign IDs in @BeforeEach, we need to create books with explicit IDs.
        // We'll bypass the auto-id logic entirely.
        Book b1 = new Book("A", "A", "A", 1.0);
        Book b2 = new Book("B", "B", "B", 2.0);
        Book b3 = new Book("C", "C", "C", 3.0);

        // Manually assign IDs in the order we want
        b1.setId(1L);
        b2.setId(2L);
        b3.setId(3L);

        List<Book> books = List.of(b2, b1, b3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("id");
        assertEquals(3, sorted.size());
        assertEquals(b1, sorted.get(0)); // ID 1
        assertEquals(b2, sorted.get(1)); // ID 2
        assertEquals(b3, sorted.get(2)); // ID 3
    }

    @Test
    void getBooksSortedBy_InvalidField_ReturnsUnsortedCopy() {
        List<Book> books = List.of(book2, book1, book3);
        initRepositoryWithBooks(books);

        List<Book> sorted = manager.getBooksSortedBy("invalid");
        assertEquals(books, sorted);
        assertNotSame(books, sorted);
    }

    // ===================== UTILITY TESTS =====================

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
        assertEquals(book2, mostExpensive);
    }

    @Test
    void findMostExpensiveBook_Empty_ReturnsNull() {
        initRepositoryWithBooks(List.of());
        assertNull(manager.findMostExpensiveBook());
    }

    @Test
    void bookMatches_TestDirectlyIfNeeded() {
        Book book = new Book("The Lord of the Rings", "J.R.R. Tolkien", "Fantasy", 25.0);
        // We must manually set the ID if needed, but this test doesn't use it.
        book.setId(100L);

        assertTrue(manager.bookMatches(book, "title", "Lord"));
        assertTrue(manager.bookMatches(book, "title", "rings"));
        assertTrue(manager.bookMatches(book, "author", "Tolkien"));
        assertTrue(manager.bookMatches(book, "genre", "Fantasy"));
        assertTrue(manager.bookMatches(book, "price", "25"));
        assertFalse(manager.bookMatches(book, "price", "30"));
        assertTrue(manager.bookMatches(book, "author", "Tolki"));
    }
}