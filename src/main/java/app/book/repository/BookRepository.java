package app.book.repository;

import app.book.entity.Book;
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
    List<Book> findByPrice(BigDecimal price);

    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    List<Book> findBooksByPriceBetween(@Param("minPrice") BigDecimal minPrice,
                                       @Param("maxPrice") BigDecimal maxPrice);

    // Finds the most expensive book
    Book findTopByOrderByPriceDesc();


   @Query("SELECT SUM(b.price) FROM Book b")
   Optional<BigDecimal> sumTotalOfPrice();

   @Query("SELECT b.genre, COUNT(b) FROM Book b GROUP BY b.genre")
   List<Object[]> getGenres(); // Used in Genre Distribution

    @Query("SELECT COUNT(b), COALESCE(SUM(b.price), 0) FROM Book b")
    Object[] getCountAndTotalValue(); // Used in Library Statistics

    @Modifying(clearAutomatically = true) // Clears the persistence context to avoid stale data
    @Query("DELETE FROM Book b WHERE b.id = :id")
    int deleteBookById(@Param("id") Long id);

    @Query("SELECT AVG(b.price) FROM Book b")
    BigDecimal getAveragePrice();




}
