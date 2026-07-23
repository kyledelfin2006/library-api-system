package app.book.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Entity
@Table(name = "books")
@JsonPropertyOrder({"id", "title", "author", "genre", "price"})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches Serial/BIGSERIAL in PostgreSQL
    private Long id;

    // Use @NotBlank for Strings to prevent empty spaces and null values.
    @NotBlank(message = "Title cannot be blank")
    @Column(nullable = false, length = 100)
    private String title;

    @NotBlank(message = "Author cannot be blank")
    @Column(nullable = false, length = 50)
    private String author;

    @NotBlank(message = "Genre cannot be blank")
    @Column(nullable = false, length = 50)
    private String genre;

    // Added @NotNull because @Positive allows null values by default unlike @NotBlank
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
