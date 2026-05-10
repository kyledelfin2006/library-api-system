package api.repository;

import api.models.Book;
import api.repository.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookRepository implements Repository<Book> {
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
