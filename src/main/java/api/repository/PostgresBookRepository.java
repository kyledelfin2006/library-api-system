package api.repository;

import api.models.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class PostgresBookRepository implements BaseRepository<Book> {

    private final JdbcTemplate jdbcTemplate;

    public PostgresBookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Book> rowMapper = (rs, rowNum) -> {
        Book book = new Book();
        book.setId(rs.getString("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setGenre(rs.getString("genre"));
        book.setPrice(rs.getDouble("price"));
        return book;
    };

    @Override
    public void add(Book book) {
        String sql = "INSERT INTO books (id, title, author, genre, price) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPrice());
    }

    @Override
    public void remove(Book book) {
        jdbcTemplate.update("DELETE FROM books WHERE id = ?", book.getId());
    }

    @Override
    public List<Book> getAll() {
        return jdbcTemplate.query("SELECT * FROM books", rowMapper);
    }

    @Override
    public void clear() {
        jdbcTemplate.update("DELETE FROM books");
    }

    @Override
    public void addAll(List<Book> books) {
        for (Book b : books) {
            add(b);
        }
    }


}