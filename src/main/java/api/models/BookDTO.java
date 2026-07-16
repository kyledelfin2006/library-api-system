package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignores unknown fields
public class BookDTO {
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @NotBlank(message = "Author cannot be empty")
    @Size(max = 50, message = "Author cannot exceed 50 characters")
    private String author;

    @NotBlank(message = "Genre cannot be empty")
    @Size(max = 50, message = "Genre cannot exceed 50 characters")
    private String genre;

    @NotNull(message = "Price cannot be empty")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    public BookDTO() {}

    public BookDTO(String title, String author, String genre, Double price) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.price = price;
    }

    public String getTitle() {
        return title;
    }
    public String getGenre() {
        return genre;
    }
    public String getAuthor() {
        return author;
    }
    public Double getPrice() {
        return price;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public void setGenre(String genre) {
        this.genre = genre;
    }
    public void setPrice(double price) {
        this.price = price;
    }
}
