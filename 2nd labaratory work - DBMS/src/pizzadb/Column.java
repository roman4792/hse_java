package pizzadb;

import java.io.Serializable;

/**
 * Описание одного столбца таблицы.
 */
public class Column implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private DataType type;
    private boolean primaryKey;
    private String refTable; // таблица, на которую ссылается столбец (или null)

    public Column(String name, DataType type, boolean primaryKey, String refTable) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
        this.refTable = refTable;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public String getRefTable() {
        return refTable;
    }
}
