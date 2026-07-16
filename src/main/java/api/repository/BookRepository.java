package api.repository;

import api.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByGenreContainingIgnoreCase(String genre);
    List<Book> findByPriceLessThanEqual(double price);

    // Custom sorting
    List<Book> findAllByOrderByTitleAsc();
    List<Book> findAllByOrderByAuthorAsc();
    List<Book> findAllByOrderByPriceAsc();
    List<Book> findAllByOrderByIdAsc();

    // Custom JPQL query for complex logic (if needed)
    @Query("SELECT b FROM Book b ORDER BY b.price DESC")
    List<Book> findAllOrderByPriceDesc();

    // Get most expensive book in one query
    Book findTopByOrderByPriceDesc();
}
