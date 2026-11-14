package pizzadb;

import java.time.LocalDate;

/**
 * Одно условие из WHERE, например: price > 20000
 */
public class Condition {

    private int columnIndex;
    private DataType type;
    private String operator;
    private Object value;

    public Condition(int columnIndex, DataType type, String operator, Object value) {
        this.columnIndex = columnIndex;
        this.type = type;
        this.operator = operator;
        this.value = value;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Проверяет, подходит ли строка под условие.
     * Если в ячейке NULL, то условие не выполняется.
     */
    public boolean matches(Row row) {
        Object cell = row.get(columnIndex);
        if (cell == null || value == null) {
            return false;
        }

        int cmp;
        if (type == DataType.INT) {
            cmp = Integer.compare((Integer) cell, (Integer) value);
        } else if (type == DataType.DOUBLE) {
            cmp = Double.compare((Double) cell, (Double) value);
        } else if (type == DataType.STRING) {
            cmp = ((String) cell).compareTo((String) value);
        } else if (type == DataType.DATE) {
            cmp = ((LocalDate) cell).compareTo((LocalDate) value);
        } else {
            cmp = Boolean.compare((Boolean) cell, (Boolean) value);
        }

        if (operator.equals("=")) {
            return cmp == 0;
        }
        if (operator.equals("!=")) {
            return cmp != 0;
        }
        if (operator.equals("<")) {
            return cmp < 0;
        }
        if (operator.equals(">")) {
            return cmp > 0;
        }
        if (operator.equals("<=")) {
            return cmp <= 0;
        }
        return cmp >= 0; // остался ">="
    }
}
