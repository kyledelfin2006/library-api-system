package app.book.dto;

import java.math.BigDecimal;

//This DTO is only used as a response – serialized to JSON, never deserialize from JSON.
//
//It has exactly three fields – all mandatory, no optional/nullable values.
//
//You never need to modify it – it's a read‑only snapshot of library statistics.
//
//No JPA or Spring‑specific behavior – it's a pure data object.


public record LibraryStatisticsDTO(long totalBooks, BigDecimal totalValue, BookResponseDTO mostExpensiveBook) {
}