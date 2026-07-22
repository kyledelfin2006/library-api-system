package testAPI;

import api.book.controller.exceptions.BookNotFoundException;
import api.manager.BookService;
import api.models.Book;
import api.models.BookRequestDTO;
import api.repository.BookRepository;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
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

    private Book sampleBook;
    private BookRequestDTO sampleBookRequestDTO;
    private final Long BOOK_ID = 1L;
    private final String TITLE = "Effective Java";
    private final String AUTHOR = "Joshua Bloch";
    private final String GENRE = "Programming";
    private final @Positive(message = "Price must be greater than 0") BigDecimal PRICE = new BigDecimal("45.0");

    @BeforeEach
    void setUp() {
        sampleBook = new Book(TITLE, AUTHOR, GENRE, PRICE);
        sampleBook.setId(BOOK_ID);

        sampleBookRequestDTO = new BookRequestDTO(TITLE, AUTHOR, GENRE, PRICE);
    }

    // ---------- addBook ----------
    @Test
    void addBook_shouldSaveAndReturnBook() {
        when(repository.save(any(Book.class))).thenReturn(sampleBook);

        Book result = bookService.addBook(sampleBookRequestDTO);

        assertNotNull(result);
        assertEquals(TITLE, result.getTitle());
        assertEquals(AUTHOR, result.getAuthor());
        assertEquals(GENRE, result.getGenre());
        assertEquals(0, PRICE.compareTo(result.getPrice()));
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
        Book existing = new Book("Old Title", "Old Author", "Old Genre", new BigDecimal("10.0"));
        existing.setId(BOOK_ID);
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(existing));

        // Update only title and price
        BookRequestDTO updates = new BookRequestDTO("New Title", null, null, new BigDecimal("30.0"));

        Book result = bookService.patchBook(BOOK_ID, updates);

        assertEquals("New Title", result.getTitle());
        assertEquals("Old Author", result.getAuthor());  // unchanged
        assertEquals("Old Genre", result.getGenre());    // unchanged
        assertEquals(0, new BigDecimal("30.0").compareTo(result.getPrice()));
        // No save() call expected
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_shouldUpdateOnlyAuthorWhenOnlyAuthorProvided() {
        Book existing = new Book("Old Title", "Old Author", "Old Genre", new BigDecimal("10.0"));
        existing.setId(BOOK_ID);
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(existing));

        BookRequestDTO updates = new BookRequestDTO(null, "New Author", null, null);

        Book result = bookService.patchBook(BOOK_ID, updates);

        assertEquals("Old Title", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("Old Genre", result.getGenre());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getPrice()));
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_shouldUpdateOnlyGenreWhenOnlyGenreProvided() {
        Book existing = new Book("Old Title", "Old Author", "Old Genre", new BigDecimal("10.0"));
        existing.setId(BOOK_ID);
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(existing));

        BookRequestDTO updates = new BookRequestDTO(null, null, "New Genre", null);

        Book result = bookService.patchBook(BOOK_ID, updates);

        assertEquals("Old Title", result.getTitle());
        assertEquals("Old Author", result.getAuthor());
        assertEquals("New Genre", result.getGenre());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getPrice()));
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_whenAllFieldsBlankOrNull_shouldLeaveBookUnchanged() {
        Book existing = new Book("Old Title", "Old Author", "Old Genre", new BigDecimal("10.0"));
        existing.setId(BOOK_ID);
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(existing));

        // Blank strings should be treated as no update
        BookRequestDTO updates = new BookRequestDTO("   ", "", "  ", null);

        Book result = bookService.patchBook(BOOK_ID, updates);

        assertEquals("Old Title", result.getTitle());
        assertEquals("Old Author", result.getAuthor());
        assertEquals("Old Genre", result.getGenre());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getPrice()));
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_whenPriceIsNull_shouldLeavePriceUnchanged() {
        Book existing = new Book("Old Title", "Old Author", "Old Genre", new BigDecimal("10.0"));
        existing.setId(BOOK_ID);
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(existing));

        BookRequestDTO updates = new BookRequestDTO("New Title", null, null, null);

        Book result = bookService.patchBook(BOOK_ID, updates);

        assertEquals("New Title", result.getTitle());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getPrice())); // unchanged
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_whenPriceIsZeroOrNegative_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO updates = new BookRequestDTO(null, null, null, new BigDecimal("-5.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.patchBook(BOOK_ID, updates));
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_whenPriceIsExactlyZero_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO updates = new BookRequestDTO(null, null, null, BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> bookService.patchBook(BOOK_ID, updates));
        verify(repository, never()).save(any());
    }

    @Test
    void patchBook_whenBookNotFound_shouldThrowBookNotFoundException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());
        BookRequestDTO updates = new BookRequestDTO("New", null, null, new BigDecimal("20.0"));

        assertThrows(BookNotFoundException.class, () -> bookService.patchBook(BOOK_ID, updates));
        verify(repository, never()).save(any());
    }

    // ---------- replaceBook ----------
    @Test
    void replaceBook_shouldReplaceAllFields() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO newData = new BookRequestDTO("New Title", "New Author", "New Genre", new BigDecimal("99.99"));
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.replaceBook(BOOK_ID, newData);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("New Genre", result.getGenre());
        assertEquals(0, new BigDecimal("99.99").compareTo(result.getPrice()));
        verify(repository, times(1)).save(sampleBook);
    }

    @Test
    void replaceBook_whenDtoIsNull_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, null));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasNullTitle_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO(null, "Author", "Genre", new BigDecimal("20.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasEmptyTitle_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("   ", "Author", "Genre", new BigDecimal("20.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasNullAuthor_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("Title", null, "Genre", new BigDecimal("20.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasEmptyAuthor_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("Title", "", "Genre", new BigDecimal("20.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasNullGenre_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("Title", "Author", null, new BigDecimal("20.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasEmptyGenre_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("Title", "Author", "  ", new BigDecimal("20.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasNullPrice_shouldSetPriceToNull() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO dto = new BookRequestDTO("Title", "Author", "Genre", null);
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.replaceBook(BOOK_ID, dto);

        assertNull(result.getPrice());
        verify(repository, times(1)).save(any(Book.class));
    }

    @Test
    void replaceBook_whenDtoHasPriceZero_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("Title", "Author", "Genre", BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenDtoHasNegativePrice_shouldThrowIllegalArgumentException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.of(sampleBook));
        BookRequestDTO invalidDto = new BookRequestDTO("Title", "Author", "Genre", new BigDecimal("-10.0"));

        assertThrows(IllegalArgumentException.class, () -> bookService.replaceBook(BOOK_ID, invalidDto));
        verify(repository, never()).save(any());
    }

    @Test
    void replaceBook_whenBookNotFound_shouldThrowBookNotFoundException() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());
        BookRequestDTO dto = new BookRequestDTO("Title", "Author", "Genre", new BigDecimal("20.0"));

        assertThrows(BookNotFoundException.class, () -> bookService.replaceBook(BOOK_ID, dto));
        verify(repository, never()).save(any());
    }

    // ---------- getBooksWithinBudget ----------
    @Test
    void getBooksWithinBudget_shouldReturnBooksWithPriceLessThanOrEqual() {
        List<Book> expected = Arrays.asList(sampleBook, new Book("Cheap", "Author", "Genre", new BigDecimal("10.0")));
        when(repository.findByPriceLessThanEqual(new BigDecimal("30.0"))).thenReturn(expected);

        List<Book> result = bookService.getBooksWithinBudget(new BigDecimal("30.0"));

        assertEquals(2, result.size());
        verify(repository, times(1)).findByPriceLessThanEqual(new BigDecimal("30.0"));
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
    void searchBooks_byAuthor_isCaseAndWhitespaceInsensitiveForType() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findByAuthorContainingIgnoreCase("Bloch")).thenReturn(expected);

        List<Book> result = bookService.searchBooks("  AUTHOR  ", "Bloch");

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
    void searchBooks_byPrice_shouldReturnBooksWithMatchingPrice() {
        List<Book> expected = List.of(sampleBook);
        // The service uses Long.parseLong, so pass an integer string
        when(repository.findByPrice(new BigDecimal("45"))).thenReturn(expected);

        List<Book> result = bookService.searchBooks("price", "45");

        assertEquals(expected, result);
        verify(repository, times(1)).findByPrice(new BigDecimal("45"));
    }

    @Test
    void searchBooks_byPrice_withInvalidNumber_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bookService.searchBooks("price", "not-a-number"));
        verify(repository, never()).findByPrice(any(BigDecimal.class));
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
    void getBooksSortedBy_validFieldMixedCaseWithWhitespace_shouldReturnSortedList() {
        List<Book> expected = List.of(sampleBook);
        when(repository.findAll(Sort.by("price").ascending())).thenReturn(expected);

        List<Book> result = bookService.getBooksSortedBy("  PRICE  ");

        assertEquals(expected, result);
        verify(repository, times(1)).findAll(Sort.by("price").ascending());
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
        when(repository.sumTotalOfPrice()).thenReturn(Optional.of(new BigDecimal("150.0")));

        BigDecimal total = bookService.getTotalLibraryValue();

        assertEquals(0, new BigDecimal("150.0").compareTo(total));
        verify(repository, times(1)).sumTotalOfPrice();
    }

    @Test
    void getTotalLibraryValue_whenNoBooks_shouldReturnZero() {
        when(repository.sumTotalOfPrice()).thenReturn(Optional.empty());

        BigDecimal total = bookService.getTotalLibraryValue();

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
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