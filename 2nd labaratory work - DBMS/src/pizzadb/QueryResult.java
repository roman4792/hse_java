package pizzadb;

import java.util.List;

/**
 * Результат выполнения запроса: либо таблица (для SELECT), либо просто сообщение.
 */
public class QueryResult {

    private List<String> columnNames; // null, если это просто сообщение
    private List<Object[]> rows;
    private String message;

    public QueryResult(String message) {
        this.message = message;
    }

    public QueryResult(List<String> columnNames, List<Object[]> rows, String message) {
        this.columnNames = columnNames;
        this.rows = rows;
        this.message = message;
    }

    public boolean hasTable() {
        return columnNames != null;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Object[]> getRows() {
        return rows;
    }

    public String getMessage() {
        return message;
    }
}
