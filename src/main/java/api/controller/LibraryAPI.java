    package api.controller;

    import api.repository.BookRepository;
    import api.storage.BookStorage;
    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.fasterxml.jackson.databind.SerializationFeature;
    import com.fasterxml.jackson.databind.DeserializationFeature;
    import api.models.Book;
    import api.models.BookInput;
    import api.responses.ApiResponse;
    import api.responses.ErrorResponse;
    import api.manager.LibraryManager;

    import java.util.ArrayList;
    import java.util.List;

    import static spark.Spark.*;

    public class LibraryAPI {
        private static final BookStorage storage = new BookStorage("Books.json");
        private static final BookRepository repository = new BookRepository();
        private static final LibraryManager manager = new LibraryManager(repository,storage);
        private static final ObjectMapper mapper = new ObjectMapper();

        public static void main(String[] args) {

            // Configure Jackson
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // Configure Spark server
            port(8080);

            // ==================== ROUTES ====================

            // GET /api/health -  checker for API health
            get("/api/health", (req, res) -> {
                res.type("application/json");
                return mapper.writeValueAsString(new ApiResponse<>(true, "API is running"));
            });

            // GET /api/books/stats -
            get("/api/books/stats", (req, res) -> {
                res.type("application/json");

                java.util.Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("totalBooks", manager.getAllBooks().size());
                stats.put("totalValue", manager.getTotalLibraryValue());
                stats.put("mostExpensiveBook", manager.findMostExpensiveBook());

                return mapper.writeValueAsString(stats);
            });

            // GET /api/books/search?type=author&value=Orwell - Search books
            get("/api/books/search", (req, res) -> {
                String searchType = req.queryParams("type");
                String searchValue = req.queryParams("value");


                if (searchType == null || searchValue == null) {
                    res.status(400);
                    return mapper.writeValueAsString(
                            new ErrorResponse("Missing parameters", "Required: type and value", 400)
                    );
                }

                // Validate search type
                if (!searchType.matches("(?i)author|title|genre|price")) {
                    res.status(400);
                    return mapper.writeValueAsString(
                            new ErrorResponse("Invalid search type", "Use: author, title, genre, or price", 400)
                    );
                }

                List<Book> foundBooks = manager.searchBooks(searchType, searchValue);

                res.type("application/json");
                return mapper.writeValueAsString(foundBooks);

            });

            // GET /api/books/budget?maxPrice=20 - Find books within budget
            get("/api/books/budget", (req, res) -> {
                String maxPriceParam = req.queryParams("maxPrice");

                if (maxPriceParam == null) {
                    res.status(400);
                    return mapper.writeValueAsString(new ErrorResponse("Missing parameter", "Required: maxPrice", 400));
                }

                try {
                    double maxPrice = Double.parseDouble(maxPriceParam);

                    List<Book> affordableBooks = manager.getBooksWithinBudget(maxPrice);

                    res.type("application/json");
                    return mapper.writeValueAsString(affordableBooks);

                } catch (NumberFormatException e) {
                    res.status(400);
                    return mapper.writeValueAsString(new ErrorResponse("Invalid Number", "MaxPrice must be a valid number", 400));
                }
            });

            // GET /api/books/sorted?by=title - Get sorted books
            get("/api/books/sorted", (req, res) -> {
                String sortField = req.queryParams("by");

                if (sortField == null || sortField.trim().isEmpty()) {
                    sortField = "title";
                }

                java.util.List<Book> sortedBooks = manager.getBooksSortedBy(sortField);
                res.type("application/json");
                return mapper.writeValueAsString(sortedBooks);
            });


            // GET /api/books - Get all books
            get("/api/books", (req, res) -> {
                res.header("Content-Type", "application/json");
                return mapper.writeValueAsString(manager.getAllBooks());
            });

            // POST /api/books - add book
            post("/api/books", (req, res) -> {
                try {
                    BookInput inputBook = mapper.readValue(req.body(), BookInput.class);

                    Book newBook = manager.addBook(inputBook); // BookInput turns into Book.

                    res.status(201);
                    res.type("application/json");

                    // return successful api response
                    return mapper.writeValueAsString(new ApiResponse<>(true, "Book Added Successfully",newBook));

                } catch (IllegalArgumentException e) {
                    res.status(400); //
                    return mapper.writeValueAsString(new ErrorResponse("Validation Error", e.getMessage(), 400));
                } catch (JsonProcessingException e){
                    res.status(400);
                    return mapper.writeValueAsString(new ErrorResponse("Json Processing Error", e.getMessage(),400));
                } catch (Exception e) {
                    res.status(500);
                    return mapper.writeValueAsString(new ErrorResponse("Unknown Error", e.getMessage(), 500));
                }
            });

            // GET /api/books/:id - Get book by ID
            get("/api/books/:id", (req, res) -> {
                String id = req.params(":id"); // parameters ID from User Request
                Book book = manager.findBookById(id);

                if (book == null) {
                    res.status(404); // Not Found
                    return mapper.writeValueAsString(new ErrorResponse("Book not found", 404));
                }

                res.type("application/json");
                return mapper.writeValueAsString(book);
            });

            // DELETE /api/books/:id - Delete a book
            delete("/api/books/:id", (req, res) -> {
                String id = req.params(":id");
                boolean deleted = manager.deleteBookById(id);

                res.type("application/json");

                if (deleted) {
                    return mapper.writeValueAsString(new ApiResponse<>(true, "Book deleted successfully"));
                } else {
                    res.status(404);
                    return mapper.writeValueAsString(new ErrorResponse("Book not found", 404));
                }

            });

            patch("/api/books/:id", (req,res) -> {
                String id = req.params(":id");

                try {

                    BookInput updates = mapper.readValue(req.body(), BookInput.class); // Ask for Update
                    Book updated = manager.patchBook(id, updates); // Set Update

                    if (updated == null){
                        res.status(404);
                        return mapper.writeValueAsString(new ErrorResponse("Book not found", 404));
                    }

                    res.type("application/json");
                    return mapper.writeValueAsString(new ApiResponse<>(true, "Book updated successfully", updated));

                } catch (IllegalArgumentException e){
                    res.status(400);
                    return mapper.writeValueAsString(new ErrorResponse("Validation Error", e.getMessage(), 400));
                } catch (JsonProcessingException e){
                    res.status(400);
                    return mapper.writeValueAsString(new ErrorResponse(" Json Processing Error",e.getMessage(),400));
                }
                catch (Exception e) {
                    res.status(500);
                    return mapper.writeValueAsString(new ErrorResponse(" Unexpected Error", e.getMessage(), 500));
                }


            });


            // global exception handler (add before the closing brace of main)
            exception(Exception.class, (e, req, res) -> {
                res.status(500);
                res.type("application/json");
                e.printStackTrace(); // For debugging
                try {
                    res.body(mapper.writeValueAsString(new ErrorResponse("Internal server error", e.getMessage(), 500)));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });

           // 404 handler for undefined routes
            notFound((req, res) -> {
                res.type("application/json");
                res.status(404);
                return mapper.writeValueAsString(new ErrorResponse("Endpoint not found", 404));
            });


            // Print startup message
            System.out.println("\n========================================");
            System.out.println(" LIBRARY API IS RUNNING");
            System.out.println("========================================");
            System.out.println(" URL: http://localhost:8080");
            System.out.println(" AVAILABLE ENDPOINTS:");
            System.out.println("   GET    /api/health                - Health check");
            System.out.println("   GET    /api/books                 - Get all books");
            System.out.println("   GET    /api/books/:id             - Get book by ID");
            System.out.println("   POST   /api/books                 - Add new book");
            System.out.println("   DELETE /api/books/:id             - Delete book");
            System.out.println("   PATCH /api/books/:id              - Update book");
            System.out.println("   GET    /api/books/search          - Search books");
            System.out.println("   GET    /api/books/budget          - Books within budget");
            System.out.println("   GET    /api/books/sorted          - Get sorted books");
            System.out.println("   GET    /api/books/stats           - Library statistics");
            System.out.println("\n TIP: Use Postman or curl to test the API");
            System.out.println("========================================\n");


    }
}

