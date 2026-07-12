package api.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "title", "author", "genre", "price"})
public class Book {

    private Long id;
    private String title;
    private String author;
    private String genre;
    private double price;

    // DEFAULT CONSTRUCTOR - REQUIRED for Jackson
    public Book() {}

    // CONSTRUCTOR 1: For NEW books (user POST request)
    public Book(String title, String author, String genre, double price) {
        validate(title, "Title");
        validate(author, "Author");
        validate(genre, "Genre");
        validatePrice(price);

        this.title = title;
        this.author = author;
        this.genre = genre;
        this.price = price;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public double getPrice() { return price; }

    // Setters - Jackson uss these for deserialization
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setPrice(double price) { this.price = price; }

    // Validators
    private void validate(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty or null");
        }
    }

    private void validatePrice(double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Error! Price must be greater than 0!");
        }
    }

    @Override
    public String toString() {
        return String.format("%-10s %-25s %-20s %-15s %10.2f", id, title, author, genre, price);
    }
}