package app.book.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDTO {
    private Long id;
    private String title;
    private String author;
    private String genre;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
