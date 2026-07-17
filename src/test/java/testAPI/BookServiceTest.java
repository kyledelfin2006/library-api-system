package testAPI;

import api.exceptions.BookNotFoundException;
import api.manager.BookService;
import api.models.Book;
import api.models.BookDTO;
import api.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookService bookService;

    // Mock setups
    private Book sampleBook;
    private BookDTO sampleBookDTO;
    private final Long BOOK_ID = 1L;
    private final String TITLE = "Effective Java";
    private final String AUTHOR = "Joshua Bloch";
    private final String GENRE = "Programming";
    private final Double PRICE = 45.0;

    @BeforeEach
    void setUp() {
        sampleBook = new Book(TITLE, AUTHOR, GENRE, PRICE);
        sampleBook.setId(BOOK_ID);

        sampleBookDTO = new BookDTO(TITLE, AUTHOR, GENRE, PRICE);
    }

    // ---------- getAllBooks ----------
    @Test
    void getAllBooks_shouldReturnAllBooks() {
        List<Book> books = Arrays.asList(sampleBook, new Book("Book2", "Author2", "Genre2", 20.0));
        when(repository.findAll()).thenReturn(books);

        List<Book> result = bookService.getAllBooks();

        assertEquals(2, result.size());
        assertTrue(result.contains(sampleBook));
        verify(repository, times(1)).findAll();
    }

    // ---------- addBook ----------
    @Test
    void addBook_shouldSaveAndReturnBook() {
        when(repository.save(any(Book.class))).thenReturn(sampleBook);

        Book result = bookService.addBook(sampleBookDTO);

        assertNotNull(result);
        assertEquals(TITLE, result.getTitle());
        assertEquals(AUTHOR, result.getAuthor());
        assertEquals(GENRE, result.getGenre());
        assertEquals(PRICE, result.getPrice());
        verify(repository, times(1)).save(any(Book.class));
    }

    // ---------- findBookById ----------
    @Test
    void findBookById_whenBookExists_shouldReturnBook() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));

        Book result = bookService.findBookById(BOOK_ID);

        assertEquals(sampleBook, result);
        verify(repository, times(1)).findById(BOOK_ID);
    }

    @Test
    void findBookById_whenBookNotFound_shouldThrowBookNotFoundException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.findBookById(BOOK_ID));
        verify(repository, times(1)).findById(BOOK_ID);
    }

    // ---------- patchBook ----------
    @Test
    void patchBook_shouldUpdateOnlyProvidedFields() {
        // Given an existing book
        Book existing = new Book("Old Title", "Old Author", "Old Genre", 10.0);
        existing.setId(BOOK_ID);
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(existing));

        // Update only title and price
        BookDTO updates = new BookDTO("New Title", null, null, 30.0);
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.patchBook(BOOK_ID, updates);

        assertEquals("New Title", result.getTitle());
        assertEquals("Old Author", result.getAuthor());  // unchanged
        assertEquals("Old Genre", result.getGenre());    // unchanged
        assertEquals(30.0, result.getPrice());
        verify(repository, times(1)).save(existing);
    }

    @Test
    void patchBook_whenPriceIsZeroOrNegative_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookDTO updates = new BookDTO(null, null, null, -5.0);

        assertThrows(IllegalArgumentException.class, () -> bookService.patchBook(BOOK_ID, updates));
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_whenBookNotFound_shouldThrowBookNotFoundException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());
        BookDTO updates = new BookDTO("New", null, null, 20.0);

        assertThrows(BookNotFoundException.class, () -> bookService.patchBook(BOOK_ID, updates));
        verify(repository, never()).save(any());
    }

    // ---------- replaceBook ----------
    @Test
    void replaceBook_shouldReplaceAllFields() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookDTO newData = new BookDTO("New Title", "New Author", "New Genre", 99.99);
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.replaceBook(BOOK_ID, newData);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("New Genre", result.getGenre());
        assertEquals(99.99, result.getPrice());
        verify(repository, times(1)).save(sampleBook);
    }

    @Test
    void replaceBook_whenDtoHasNullTitle_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookDTO invalidDto = new BookDTO(null, "Author", "Genre", 20.0);

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasEmptyAuthor_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookDTO invalidDto = new BookDTO("Title", "", "Genre", 20.0);

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasPriceZero_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookDTO invalidDto = new BookDTO("Title", "Author", "Genre", 0.0);

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenBookNotFound_shouldThrowBookNotFoundException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());
        BookDTO dto = new BookDTO("Title", "Author", "Genre", 20.0);

        assertThrows(BookNotFoundException.class, () -> bookService.replaceBook(BOOK_ID, dto));
        verify(repository, never()).save(any());
    }

    // ---------- deleteBookById ----------
    @Test
    void deleteBookById_shouldDeleteExistingBook() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        doNothing().when(repository).delete(any(Book.class));

        bookService.deleteBookById(BOOK_ID);

        verify(repository, times(1)).delete(sampleBook);
    }

    @Test
    void deleteBookById_whenBookNotFound_shouldThrowBookNotFoundException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.deleteBookById(BOOK_ID));
        verify(repository, never()).delete(any());
    }

    // ---------- getBooksWithinBudget ----------
    @Test
    void getBooksWithinBudget_shouldReturnBooksWithPriceLessThanOrEqual() {
        List<Book> expected = Arrays.asList(sampleBook, new Book("Cheap", "Author", "Genre", 10.0));
        when(repository.findByPriceLessThanEqual(30.0)).thenReturn(expected);

        List<Book> result = bookService.getBooksWithinBudget(30.0);

        assertEquals(2, result.size());
        verify(repository, times(1)).findByPriceLessThanEqual(30.0);
    }

    // ---------- searchBooks ----------
    @Test
    void searchBooks_byAuthor_shouldReturnMatchingBooks() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findByAuthorContainingIgnoreCase("Bloch")).thenReturn(expected);

        List<Book> result = bookService.searchBooks("author", "Bloch");

        assertEquals(expected, result);
        verify(repository, times(1)).findByAuthorContainingIgnoreCase("Bloch");
    }

    @Test
    void searchBooks_byTitle_shouldReturnMatchingBooks() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findByTitleContainingIgnoreCase("Effective")).thenReturn(expected);

        List<Book> result = bookService.searchBooks("title", "Effective");

        assertEquals(expected, result);
        verify(repository, times(1)).findByTitleContainingIgnoreCase("Effective");
    }

    @Test
    void searchBooks_byGenre_shouldReturnMatchingBooks() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findByGenreContainingIgnoreCase("Program")).thenReturn(expected);

        List<Book> result = bookService.searchBooks("genre", "Program");

        assertEquals(expected, result);
        verify(repository, times(1)).findByGenreContainingIgnoreCase("Program");
    }

    @Test
    void searchBooks_byPrice_shouldReturnBooksWithMatchingPriceWithinTolerance() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findBooksByPriceBetween(44.9999, 45.0001)).thenReturn(expected);

        List<Book> result = bookService.searchBooks("price", "45.0");

        assertEquals(expected, result);
        verify(repository, times(1)).findBooksByPriceBetween(44.9999, 45.0001);
    }

    @Test
    void searchBooks_byPrice_withInvalidNumber_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bookService.searchBooks("price", "not-a-number"));
        verify(repository, never()).findBooksByPriceBetween(anyDouble(), anyDouble());
    }

    @Test
    void searchBooks_withInvalidType_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bookService.searchBooks("invalid", "value"));
        verify(repository, never()).findByAuthorContainingIgnoreCase(anyString());
    }

    // ---------- getBooksSortedBy ----------
    @Test
    void getBooksSortedBy_validField_shouldReturnSortedList() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findAll(Sort.by("title").ascending())).thenReturn(expected);

        List<Book> result = bookService.getBooksSortedBy("title");

        assertEquals(expected, result);
        verify(repository, times(1)).findAll(Sort.by("title").ascending());
    }

    @Test
    void getBooksSortedBy_invalidField_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bookService.getBooksSortedBy("invalidField"));
        verify(repository, never()).findAll(any(Sort.class));
    }

    @Test
    void getBooksSortedBy_nullField_shouldReturnAllUnsorted() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findAll()).thenReturn(expected);

        List<Book> result = bookService.getBooksSortedBy(null);

        assertEquals(expected, result);
        verify(repository, times(1)).findAll();
        verify(repository, never()).findAll(any(Sort.class));
    }

    @Test
    void getBooksSortedBy_emptyField_shouldReturnAllUnsorted() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findAll()).thenReturn(expected);

        List<Book> result = bookService.getBooksSortedBy("   ");

        assertEquals(expected, result);
        verify(repository, times(1)).findAll();
        verify(repository, never()).findAll(any(Sort.class));
    }

    // ---------- getTotalLibraryValue ----------
    @Test
    void getTotalLibraryValue_whenBooksExist_shouldReturnSum() {
        when(repository.sumTotalOfPrice()).thenReturn(Optional.of(150.0));

        Double total = bookService.getTotalLibraryValue();

        assertEquals(150.0, total);
        verify(repository, times(1)).sumTotalOfPrice();
    }

    @Test
    void getTotalLibraryValue_whenNoBooks_shouldReturnZero() {
        when(repository.sumTotalOfPrice()).thenReturn(Optional.empty());

        Double total = bookService.getTotalLibraryValue();

        assertEquals(0.0, total);
        verify(repository, times(1)).sumTotalOfPrice();
    }

    // ---------- findMostExpensiveBook ----------
    @Test
    void findMostExpensiveBook_shouldReturnBookWithHighestPrice() {
        when(repository.findTopByOrderByPriceDesc()).thenReturn(sampleBook);

        Book result = bookService.findMostExpensiveBook();

        assertEquals(sampleBook, result);
        verify(repository, times(1)).findTopByOrderByPriceDesc();
    }

    @Test
    void findMostExpensiveBook_whenNoBooks_shouldReturnNull() {
        when(repository.findTopByOrderByPriceDesc()).thenReturn(null);

        Book result = bookService.findMostExpensiveBook();

        assertNull(result);
        verify(repository, times(1)).findTopByOrderByPriceDesc();
    }
}