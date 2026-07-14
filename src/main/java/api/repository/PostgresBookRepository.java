package api.repository;

import java.sql.PreparedStatement;
import api.models.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class PostgresBookRepository implements BaseRepository<Book> {

    private final JdbcTemplate jdbcTemplate;

    public PostgresBookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Book> rowMapper = (rs, rowNum) -> {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setGenre(rs.getString("genre"));
        book.setPrice(rs.getDouble("price"));
        return book;
    };

    @Override
    public void add(Book book) {
        String sql = "INSERT INTO books (title, author, genre, price) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getGenre());
            ps.setDouble(4, book.getPrice());
            return ps;
        }, keyHolder);

        // Extract the generated ID from the map of returned columns
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            Number generatedId = (Number) keys.get("id");
            book.setId(generatedId.longValue());
        }
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
        String sql = "INSERT INTO books (title, author, genre, price) VALUES (?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, books, books.size(), (ps, book) -> {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getGenre());
            ps.setDouble(4, book.getPrice());
        });
    }

    @Override
    public void update(Book book) {
        String sql = "UPDATE books SET title=?, author=?, genre=?, price=? WHERE id=?";
        jdbcTemplate.update(sql,
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPrice(),
                book.getId()
        );
    }


}