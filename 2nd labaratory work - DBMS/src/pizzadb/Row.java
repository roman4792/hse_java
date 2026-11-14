package pizzadb;

import java.io.Serializable;

/**
 * Одна запись (строка) таблицы. Значения лежат в массиве
 * в том же порядке, что и столбцы таблицы.
 */
public class Row implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object[] values;

    public Row(Object[] values) {
        this.values = values;
    }

    public Object get(int index) {
        return values[index];
    }

    public void set(int index, Object value) {
        values[index] = value;
    }
}
