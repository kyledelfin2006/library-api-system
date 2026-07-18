    package api.controller;

    import api.models.Book;
    import api.models.BookDTO;
    import api.responses.ApiResponse;
    import api.manager.BookService;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;


    @RestController
    @RequestMapping("/api")
    public class BookAPI {

        private final BookService manager;

        @Autowired
        public BookAPI(BookService manager) {
            this.manager = manager;
        }

        // ==================== ROUTES ====================

        @GetMapping("/books/health")
        public ResponseEntity<ApiResponse<String>> healthCheck() {
            return ResponseEntity.ok(new ApiResponse<>(true, "API is running"));
        }

        @GetMapping("/books/stats")
        public ResponseEntity<Map<String,Object>> getStats() {
            Map<String, Object> stats = new ConcurrentHashMap<>();
            stats.put("totalBooks", manager.countBooks());
            stats.put("totalValue", manager.getTotalLibraryValue());

            Book mostExpensive = manager.findMostExpensiveBook();
            stats.put("mostExpensiveBook", mostExpensive != null ? mostExpensive : "No books in library");

            return ResponseEntity.ok(stats);
        }

        // Search books
        @GetMapping("/books/search")
        public ResponseEntity<List<Book>> searchBooks(
                @RequestParam String type,
                @RequestParam String value) {

            if (!type.matches("(?i)author|title|genre|price")) {
                throw new IllegalArgumentException("Invalid search type. Use: author, title, genre, or price");
            }

            List<Book> foundBooks = manager.searchBooks(type, value);
            return ResponseEntity.ok(foundBooks);
        }

        @GetMapping("/books/budget")
        public ResponseEntity<List<Book>> budgetBooks(@RequestParam double maxPrice) {
                List<Book> affordableBooks = manager.getBooksWithinBudget(maxPrice);
                return ResponseEntity.ok(affordableBooks);
        }


        // Add a new book (validated)
        @PostMapping("/books")
        public ResponseEntity<ApiResponse<Book>> addBook(@Valid @RequestBody BookDTO input) {
            Book newBook = manager.addBook(input);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Book Added Successfully", newBook));
        }

        // Get book by ID
        @GetMapping("/books/{id}")
        public ResponseEntity<Book> getBookById(@PathVariable Long id) {
            Book book = manager.findBookById(id);
            return ResponseEntity.ok(book);
        }

        // Delete a book
        @DeleteMapping("/books/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
            manager.deleteBookById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Book deleted successfully"));
        }

        @GetMapping("/books")
        public ResponseEntity<List<Book>> getAllBooks() {
            return ResponseEntity.ok(manager.getAllBooks());
        }

        @GetMapping("/books/sorted")
        public ResponseEntity<List<Book>> getSortedBooks(
                @RequestParam(required = false, defaultValue = "title") String category) {
            return ResponseEntity.ok(manager.getBooksSortedBy(category));
        }

        // Patch (partial update)
        @PatchMapping("/books/{id}")
        public ResponseEntity<ApiResponse<Book>> patchBook(
                @PathVariable Long id,
                @RequestBody BookDTO updates) { // No @Valid to allow null values

            Book updated = manager.patchBook(id, updates);
            return ResponseEntity.ok(new ApiResponse<>(true, "Book updated successfully", updated));
        }

        // Put (complete update)
        @PutMapping("/books/{id}")
        public ResponseEntity<ApiResponse<Book>> replaceBook(
                @PathVariable Long id,
                @Valid @RequestBody BookDTO updates){

             Book updated = manager.replaceBook(id,updates);
            return ResponseEntity.ok(new ApiResponse<>(true, "Book updated successfully", updated));
        }
    }

