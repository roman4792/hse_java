package pizzadb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Индекс по одному столбцу таблицы.
 * Внутри обычная HashMap: значение -> номера строк, где оно встречается.
 * Помогает быстро находить строки по условию "столбец = значение".
 */
public class Index implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String tableName;
    private String columnName;
    private int columnPosition;
    private int indexedRows; // сколько строк таблицы учтено (нужно для проверки при запуске)
    private HashMap<Object, List<Integer>> map = new HashMap<Object, List<Integer>>();

    public Index(String name, String tableName, String columnName, int columnPosition) {
        this.name = name;
        this.tableName = tableName;
        this.columnName = columnName;
        this.columnPosition = columnPosition;
    }

    public String getName() {
        return name;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnPosition() {
        return columnPosition;
    }

    public int getIndexedRows() {
        return indexedRows;
    }

    /**
     * Добавляет в индекс значение и номер строки, где оно лежит.
     */
    public void add(Object value, int rowNumber) {
        indexedRows++;
        if (value == null) {
            return; // NULL в индекс не кладём
        }
        List<Integer> list = map.get(value);
        if (list == null) {
            list = new ArrayList<Integer>();
            map.put(value, list);
        }
        list.add(rowNumber);
    }

    /**
     * Возвращает номера строк с таким значением (может быть пустой список).
     */
    public List<Integer> find(Object value) {
        List<Integer> list = map.get(value);
        if (list == null) {
            return new ArrayList<Integer>();
        }
        return list;
    }

    /**
     * Строит индекс заново по всем строкам таблицы.
     */
    public void rebuild(List<Row> rows) {
        map = new HashMap<Object, List<Integer>>();
        indexedRows = 0;
        for (int i = 0; i < rows.size(); i++) {
            add(rows.get(i).get(columnPosition), i);
        }
    }
}
