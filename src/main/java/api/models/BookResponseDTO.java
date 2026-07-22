package api.models;

import java.math.BigDecimal;

public class BookResponseDTO {
    private Long id;
    private String title;
    private String author;
    private String genre;
    private BigDecimal price;

    public BookResponseDTO(){}
    public BookResponseDTO(Long id, String title, String author, String genre, BigDecimal price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }

    public BigDecimal getPrice() {
        return price;
    }
}