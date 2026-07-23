package app.book.dto;

import java.math.BigDecimal;

public class LibraryStatisticsDTO {
    private final long totalBooks;
    private final BigDecimal totalValue;
    private final BookResponseDTO mostExpensiveBook;

    public LibraryStatisticsDTO(long totalBooks, BigDecimal totalValue, BookResponseDTO mostExpensiveBook) {
        this.totalBooks = totalBooks;
        this.totalValue = totalValue;
        this.mostExpensiveBook = mostExpensiveBook;
    }

    // Getters
    public long getTotalBooks() { return totalBooks; }
    public BigDecimal getTotalValue() { return totalValue; }
    public BookResponseDTO getMostExpensiveBook() { return mostExpensiveBook; }
}