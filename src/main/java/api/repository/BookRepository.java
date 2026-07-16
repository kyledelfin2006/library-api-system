package api.repository;

import api.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByGenreContainingIgnoreCase(String genre);
    List<Book> findByPriceLessThanEqual(double price);

    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    List<Book> findBooksByPriceBetween(@Param("minPrice") double minPrice,
                                       @Param("maxPrice") double maxPrice);

    // Finds the most expensive book
    Book findTopByOrderByPriceDesc();


   @Query("SELECT SUM(b.price) FROM Book b")
   Optional<Double> sumTotalOfPrice();
}
