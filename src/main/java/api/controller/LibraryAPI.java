    package api.controller;

    import api.exceptions.BookNotFoundException;
    import api.models.Book;
    import api.models.BookDTO;
    import api.responses.ApiResponse;
    import api.manager.LibraryManager;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;


    @RestController
    @RequestMapping("/api")
    public class LibraryAPI {

        private final LibraryManager manager;

        @Autowired
        public LibraryAPI(LibraryManager manager) {
            this.manager = manager;
        }

        // ==================== ROUTES ====================

        @GetMapping("/health")
        public ResponseEntity<ApiResponse<String>> healthCheck() {
            return ResponseEntity.ok(new ApiResponse<>(true, "API is running"));
        }

        @GetMapping("/books/stats")
        public ResponseEntity<Map<String,Object>> getStats() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBooks", manager.getAllBooks().size());
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
        public ResponseEntity<List<Book>> budgetBooks(@RequestParam String maxPrice) {
            try {
                double maxedPrice = Double.parseDouble(maxPrice);
                List<Book> affordableBooks = manager.getBooksWithinBudget(maxedPrice);
                return ResponseEntity.ok(affordableBooks);
            } catch (NumberFormatException e) {
                throw new  IllegalArgumentException("Invalid Number: MaxPrice must be a valid number");
            }
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
            if (book == null) {
                throw new BookNotFoundException("Book with ID " + id + " not found");
            }
            return ResponseEntity.ok(book);
        }

        // Delete a book
        @DeleteMapping("/books/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
            boolean deleted = manager.deleteBookById(id);
            if (!deleted) {
                throw new BookNotFoundException("Book with ID " + id + " not found");
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Book deleted successfully"));
        }

        @GetMapping("/books")
        public ResponseEntity<List<Book>> getAllBooks() {
            return ResponseEntity.ok(manager.getAllBooks());
        }

        @GetMapping("/books/sorted")
        public ResponseEntity<List<Book>> getSortedBooks(
                @RequestParam(required = false, defaultValue = "title") String by) {
            return ResponseEntity.ok(manager.getBooksSortedBy(by));
        }

        // Patch (partial update)
        @PatchMapping("/books/{id}")
        public ResponseEntity<ApiResponse<Book>> patchBook(
                @PathVariable Long id,
                @RequestBody BookDTO updates) {

            Book updated = manager.patchBook(id, updates);
            if (updated == null) {
                throw new BookNotFoundException("Book with ID " + id + " not found");
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Book updated successfully", updated));
        }


    }

