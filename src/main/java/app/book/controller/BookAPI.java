    package app.book.controller;

    import app.LibraryApplication;
    import app.book.dto.BookResponseDTO;
    import app.book.dto.LibraryStatisticsDTO;
    import app.book.entity.Book;
    import app.book.dto.BookRequestDTO;
    import app.book.mapper.BookMapper;
    import app.global.responses.ApiResponse;
    import app.book.service.BookService;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import java.math.BigDecimal;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.web.PageableDefault;


    @RestController
    @RequestMapping("/app/books")
    public class BookAPI {

        private final BookService service;
        private final BookMapper mapper;

        @Autowired
        public BookAPI(BookService service, BookMapper mapper) {
            this.service = service;
            this.mapper = mapper;
        }

        // ==================== ROUTES ====================

        @GetMapping("/health")
        public ResponseEntity<ApiResponse<String>> healthCheck() {
            return ResponseEntity.ok(new ApiResponse<>(true, "API is running"));
        }

        @GetMapping("/stats")
        public ResponseEntity<LibraryStatisticsDTO> getStats() {

            // 1. Create libraryStatsDTO pre-built dto from service
            LibraryStatisticsDTO dto = service.getLibraryStatistics();

            // 2. Return object
            return ResponseEntity.ok(dto);
        }

        // Search books
        @GetMapping("/search")
        public ResponseEntity<List<BookResponseDTO>> searchBooks(
                @RequestParam String type,
                @RequestParam String value) {

            List<Book> foundBooks = service.searchBooks(type, value);
            return ResponseEntity.ok(mapper.toResponseDTOList(foundBooks));
        }

        @GetMapping("/budget")
        public ResponseEntity<List<BookResponseDTO>> budgetBooks(@RequestParam BigDecimal maxPrice) {
                List<Book> affordableBooks = service.getBooksWithinBudget(maxPrice);
                return ResponseEntity.ok(mapper.toResponseDTOList(affordableBooks));
        }

        // Add a new book (validated)
        @PostMapping("/add")
        public ResponseEntity<ApiResponse<BookResponseDTO>> addBook(@Valid @RequestBody BookRequestDTO input) {
            Book newBook = service.addBook(input);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Book Added Successfully", mapper.toResponseDTO(newBook)));
        }

        // Get book by ID
        @GetMapping("/{id}")
        public ResponseEntity<BookResponseDTO> getBookById(@PathVariable Long id) {
            Book book = service.findBookById(id);
            return ResponseEntity.ok(mapper.toResponseDTO(book));
        }

        // Delete a book
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
            service.deleteBookById(id); // Throws Exception in case
            return ResponseEntity.ok(new ApiResponse<>(true, "Book deleted successfully"));
        }

        @GetMapping("/all") // User sets page, if omit then default will be used
        public ResponseEntity<Page<BookResponseDTO>> getAllBooks(
               @PageableDefault(size = 12, sort = "id") Pageable pageable) {

            // 1. Get page as book entity
            Page<Book> page = service.getBooks(pageable);

            // 2. Map from book entity to response dto
            Page<BookResponseDTO> dtopage = page.map(mapper::toResponseDTO);

            return ResponseEntity.ok(dtopage);
        }

        @GetMapping("/sorted")
        public ResponseEntity<List<BookResponseDTO>> getSortedBooks(
                @RequestParam(required = false, defaultValue = "title") String category) {

            // 1. Get book list in book entity type
            List<Book> bookList = service.getBooksSortedBy(category);

            // 2. Map book entity list to DTO list
            List<BookResponseDTO> dtoList = mapper.toResponseDTOList(bookList);

            return ResponseEntity.ok(dtoList);
        }

        @GetMapping("/genre")
        public ResponseEntity<Map<String, Long>> getGenre() {
            return ResponseEntity.ok(service.getGenreDistribution());
        }

        @GetMapping("/price")
        public ResponseEntity<List<BookResponseDTO>> getPriceRangedBooks(
                @RequestParam BigDecimal minPrice,
                @RequestParam BigDecimal maxPrice
        ){
            List<Book> bookList = service.getBooksInPriceRange(minPrice,maxPrice);
            List<BookResponseDTO> dtoList = mapper.toResponseDTOList(bookList);
            return ResponseEntity.ok(dtoList);
        }

        // Patch (partial update)
        @PatchMapping("/{id}")
        public ResponseEntity<ApiResponse<BookResponseDTO>> patchBook(
                @PathVariable Long id,
                @RequestBody BookRequestDTO updates) { // No @Valid to allow null values

            Book updated = service.patchBook(id, updates);
            return ResponseEntity.ok(
                    new ApiResponse<>(true,"Book updated successfully", mapper.toResponseDTO(updated))
            );
        }

        // Put (complete update)
        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<BookResponseDTO>> replaceBook(
                @PathVariable Long id,
                @Valid @RequestBody BookRequestDTO updates){

            // Update book
             Book updated = service.replaceBook(id,updates);
            return ResponseEntity.ok(
                    new ApiResponse<>(true,
                            "Book updated successfully",
                            mapper.toResponseDTO(updated)));
        }
    }

