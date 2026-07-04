package api.config;

import api.manager.LibraryManager;
import api.models.BookDTO;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedDataLoader implements CommandLineRunner {

    private final LibraryManager libraryManager;

    public SeedDataLoader(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    @Override
    public void run(String @NonNull ... args) throws Exception {
        // Only seed if the library is empty
        if (libraryManager.getAllBooks().isEmpty()) {
            System.out.println(" Seeding initial books...");

            libraryManager.addBook(new BookDTO("Dune", "Frank Herbert", "Sci-Fi", 15.99));
            libraryManager.addBook(new BookDTO("Pride and Prejudice", "Jane Austen", "Romance", 9.99));
            libraryManager.addBook(new BookDTO("The Catcher in the Rye", "J.D. Salinger", "Fiction", 11.50));
            libraryManager.addBook(new BookDTO("To Kill a Mockingbird", "Harper Lee", "Fiction", 13.25));
            libraryManager.addBook(new BookDTO("The Great Gatsby", "F. Scott Fitzgerald", "Fiction", 10.00));
            libraryManager.addBook(new BookDTO("Moby-Dick", "Herman Melville", "Adventure", 8.75));

            System.out.println(" Seed data loaded. ");
        }
    }
}