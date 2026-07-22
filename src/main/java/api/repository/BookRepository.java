package api.repository;

import api.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByGenreContainingIgnoreCase(String genre);
    List<Book> findByPriceLessThanEqual(BigDecimal price);
    List<Book> findPriceContaining(BigDecimal price);

    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    List<Book> findBooksByPriceBetween(@Param("minPrice") BigDecimal minPrice,
                                       @Param("maxPrice") BigDecimal maxPrice);

    // Finds the most expensive book
    Book findTopByOrderByPriceDesc();


   @Query("SELECT SUM(b.price) FROM Book b")
   Optional<BigDecimal> sumTotalOfPrice();

   @Query("SELECT b.genre, COUNT(b) FROM Book b GROUP BY b.genre")
   List<Object[]> getGenres();

    @Modifying(clearAutomatically = true) // Clears the persistence context to avoid stale data
    @Query("DELETE FROM Book b WHERE b.id = :id")
    int deleteBookById(@Param("id") Long id);




}
