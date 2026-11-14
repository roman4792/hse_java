package pizzadb;

/**
 * Ошибка, которую выбрасывает наша СУБД (неверный запрос, дубликат ключа и т.д.).
 */
public class DbException extends RuntimeException {

    public DbException(String message) {
        super(message);
    }
}
