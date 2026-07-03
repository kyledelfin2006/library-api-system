package api.repository;

import java.util.List;

public interface BaseRepository<T> {
    public void add(T type);
    public void remove(T type);
    List<T> getAll();
    public void clear();
}
