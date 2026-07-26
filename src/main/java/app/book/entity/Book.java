package app.book.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Represents a book entity in the bookstore application.
 * This class is mapped to the "books" table in the database and includes
 * validation constraints for each field.
 * <p>
 * The JSON serialization order is defined by {@link JsonPropertyOrder}.
 * </p>
 */
@Entity
@Table(name = "books")
@JsonPropertyOrder({"id", "title", "author", "genre", "price"})
public class Book {


    /**
     * The unique identifier of the book.
     * Generated automatically using the database identity column
     * (SERIAL / BIGSERIAL in PostgreSQL).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches Serial/BIGSERIAL in PostgreSQL
    private Long id;

    /**
     * <p>
     * The {@link NotBlank} validation annotation is used to ensure that the title, author, genre
     * is not {@code null}, not empty, and does not consist solely of whitespace.
     * This prevents empty or meaningless strings from being persisted.
     * </p>
     * The maximum length is 100 characters, enforced by the database column definition.
     */
    @NotBlank(message = "Title cannot be blank")
    @Column(nullable = false, length = 100)
    private String title;

    @NotBlank(message = "Author cannot be blank")
    @Column(nullable = false, length = 50)
    private String author;

    @NotBlank(message = "Genre cannot be blank")
    @Column(nullable = false, length = 50)
    private String genre;

    /**
     * The price of the book.
     * <p>
     * The {@link Positive} validation constraint ensures that the price must be
     * strictly greater than zero. However, by default {@code @Positive} allows
     * {@code null} values (unlike {@code @NotBlank}, which only applies to strings).
     * Since the price is a required field, the {@link NotNull} constraint is
     * added explicitly to enforce that the price is always provided and cannot
     * be {@code null}.
     * </p>
     * <p>
     * At the database level, the column is defined as {@code NOT NULL} with
     * a precision of 10 digits and a scale of 2 decimal places, suitable for
     * storing monetary values.
     * </p>
     */
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // REQUIRED for JPA and Jackson deserialization
    public Book() {}

    public Book(String title, String author, String genre, BigDecimal price) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    @Override
    public String toString() {
        return String.format("%-10s %-25s %-20s %-15s %10.2f",
                id != null ? id : "NEW", title, author, genre, price);
    }
}
