    package api.controller;

    import api.models.Book;
    import api.models.BookRequestDTO;
    import api.responses.ApiResponse;
    import api.manager.BookService;
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
    @RequestMapping("/api/books")
    public class BookAPI {

        private final BookService manager;

        @Autowired
        public BookAPI(BookService manager) {
            this.manager = manager;
        }

        // ==================== ROUTES ====================

        @GetMapping("/health")
        public ResponseEntity<ApiResponse<String>> healthCheck() {
            return ResponseEntity.ok(new ApiResponse<>(true, "API is running"));
        }

        @GetMapping("/stats")
        public ResponseEntity<Map<String,Object>> getStats() {
            Map<String, Object> stats = new ConcurrentHashMap<>();
            stats.put("totalBooks", manager.countBooks());
            stats.put("totalValue", manager.getTotalLibraryValue());

            Book mostExpensive = manager.findMostExpensiveBook();
            stats.put("mostExpensiveBook", mostExpensive != null ? mostExpensive : "No books in library");

            return ResponseEntity.ok(stats);
        }

        // Search books
        @GetMapping("/search")
        public ResponseEntity<List<Book>> searchBooks(
                @RequestParam String type,
                @RequestParam String value) {

            List<Book> foundBooks = manager.searchBooks(type, value);
            return ResponseEntity.ok(foundBooks);
        }

        @GetMapping("/budget")
        public ResponseEntity<List<Book>> budgetBooks(@RequestParam BigDecimal maxPrice) {
                List<Book> affordableBooks = manager.getBooksWithinBudget(maxPrice);
                return ResponseEntity.ok(affordableBooks);
        }

        // Add a new book (validated)
        @PostMapping("/addBook")
        public ResponseEntity<ApiResponse<Book>> addBook(@Valid @RequestBody BookRequestDTO input) {
            Book newBook = manager.addBook(input);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Book Added Successfully", newBook));
        }

        // Get book by ID
        @GetMapping("/{id}")
        public ResponseEntity<Book> getBookById(@PathVariable Long id) {
            Book book = manager.findBookById(id);
            return ResponseEntity.ok(book);
        }

        // Delete a book
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
            manager.deleteBookById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Book deleted successfully"));
        }

        @GetMapping("/all") // User sets page, if omit then default will be used
        public ResponseEntity<Page<Book>> getAllBooks(
               @PageableDefault(size = 12, sort = "id") Pageable pageable) {
            Page<Book> page = manager.getBooks(pageable);
            return ResponseEntity.ok(page);
        }

        @GetMapping("/sorted")
        public ResponseEntity<List<Book>> getSortedBooks(
                @RequestParam(required = false, defaultValue = "title") String category) {
            return ResponseEntity.ok(manager.getBooksSortedBy(category));
        }

        @GetMapping("/genre")
        public ResponseEntity<Map<String, Long>> getGenre() {
            return ResponseEntity.ok(manager.getGenreDistribution());
        }

        @GetMapping("/price")
        public ResponseEntity<List<Book>> getPriceRangedBooks(
                @RequestParam String minPrice,
                @RequestParam String maxPrice
        ){
            return  ResponseEntity.ok(manager.getBooksInPriceRange(minPrice,maxPrice));
        }



        // Patch (partial update)
        @PatchMapping("/{id}")
        public ResponseEntity<ApiResponse<Book>> patchBook(
                @PathVariable Long id,
                @RequestBody BookRequestDTO updates) { // No @Valid to allow null values

            Book updated = manager.patchBook(id, updates);
            return ResponseEntity.ok(new ApiResponse<>(true, "Book updated successfully", updated));
        }

        // Put (complete update)
        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<Book>> replaceBook(
                @PathVariable Long id,
                @Valid @RequestBody BookRequestDTO updates){

             Book updated = manager.replaceBook(id,updates);
            return ResponseEntity.ok(new ApiResponse<>(true, "Book updated successfully", updated));
        }
    }

