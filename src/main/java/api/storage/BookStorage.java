package api.storage;

import api.exceptions.StorageException;
import api.models.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class BookStorage implements Storage<Book> {

    private final String filename;

    // The filename can be overridden in application.properties,
    // default is "books.txt".
    public BookStorage(@Value("${books.storage.file:books.txt}") String filename) {
        this.filename = filename;
    }

    @Override
    public void save(List<Book> books) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Book book : books) {
                // Format: id|title|author|genre|price
                writer.write(String.format("%s|%s|%s|%s|%.2f",
                        book.getId(),
                        escape(book.getTitle()),
                        escape(book.getAuthor()),
                        escape(book.getGenre()),
                        book.getPrice()));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new StorageException("Failed to save books to file: " + filename, e);
        }
    }

    @Override
    public List<Book> load() {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        List<Book> books = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length != 5) {
                    // Log a warning but skip malformed lines
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                Book book = new Book(); // default constructor
                book.setId(parts[0].trim());
                book.setTitle(unescape(parts[1].trim()));
                book.setAuthor(unescape(parts[2].trim()));
                book.setGenre(unescape(parts[3].trim()));
                try {
                    book.setPrice(Double.parseDouble(parts[4].trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid price in line: " + line);
                    continue;
                }
                books.add(book);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to load books from file: " + filename, e);
        }
        return books;
    }

    // Simple escape to avoid breaking the delimiter (if fields contain '|')
    private String escape(String s) {
        return s.replace("|", "\\|").replace("\n", "\\n");
    }

    private String unescape(String s) {
        return s.replace("\\|", "|").replace("\\n", "\n");
    }
}