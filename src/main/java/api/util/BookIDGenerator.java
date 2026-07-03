package api.util;

import java.util.concurrent.atomic.AtomicInteger;

public class BookIDGenerator {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    public static String generateNextID() {
        int id = nextId.getAndIncrement();  // READ + WRITE as ONE operation
        return String.format("%04d", id);
    }


    // Setter for NextId
    public static void setNextId(int id) {
        BookIDGenerator.nextId.set(id);
    }
}
