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

    /**
     * Finds all books whose title contains the given substring (case-insensitive).
     *
     * @param title the substring to search for in the title (non-null)
     * @return a list of books with a title containing {@code title} (ignoring case),
     *         or an empty list if none found
     */
    List<Book> findByTitleContainingIgnoreCase(String title);

    /**
     * Finds all books whose author contains the given substring (case-insensitive).
     *
     * @param author the substring to search for in the title (non-null)
     * @return a list of books with a title containing {@code author} (ignoring case),
     *         or an empty list if none found
     */
    List<Book> findByAuthorContainingIgnoreCase(String author);

    /**
     * Finds all books whose genre contains the given substring (case-insensitive).
     *
     * @param genre the substring to search for in the title (non-null)
     * @return a list of books with a title containing {@code title} (ignoring case),
     *         or an empty list if none found
     */
    List<Book> findByGenreContainingIgnoreCase(String genre);

    /**
     * Finds all books whose price is less than or equal of the given BigDecimal (case-insensitive).
     *
     * @param price the decimal to compare for in the query (non-null)
     * @return a list of books with a price less than or equal to {@code price} (ignoring case),
     *         or an empty list if none found
     */
    List<Book> findByPriceLessThanEqual(BigDecimal price);

    /**
     * Finds all books whose price contains the exact given decimal (case-insensitive).
     *
     * @param price the price to find for a book (non-null)
     * @return a list of books with an exact price of {@code price} (ignoring case),
     *         or an empty list if none found
     */
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
    Optional<BigDecimal> getAveragePrice();




}
