package api.repository;

import api.models.Book;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository implements BaseRepository<Book> {
    private final List<Book> books = new ArrayList<>();

    public BookRepository() {}

    @Override
    public synchronized void add(Book book){
        books.add(book);
    }

    @Override
    public synchronized void remove(Book book){
        books.remove(book);
    }

    @Override
    public synchronized List<Book> getAll() {
        return List.copyOf(books);
    }

    @Override
    public synchronized void clear(){
        books.clear();
    }

    public synchronized void addAll(List<Book> Books) {
        books.addAll(Books);
    }

}
