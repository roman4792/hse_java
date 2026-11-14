package pizzadb;

import java.time.LocalDate;

/**
 * Типы данных, которые поддерживает база.
 */
public enum DataType {
    INT, DOUBLE, STRING, DATE, BOOLEAN;

    /**
     * Находит тип по названию из запроса (INT, VARCHAR и т.д.).
     */
    public static DataType fromString(String text) {
        String t = text.toUpperCase();
        if (t.equals("INT") || t.equals("INTEGER")) {
            return INT;
        }
        if (t.equals("DOUBLE") || t.equals("FLOAT")) {
            return DOUBLE;
        }
        if (t.equals("STRING") || t.equals("VARCHAR") || t.equals("TEXT")) {
            return STRING;
        }
        if (t.equals("DATE")) {
            return DATE;
        }
        if (t.equals("BOOLEAN") || t.equals("BOOL")) {
            return BOOLEAN;
        }
        throw new DbException("Неизвестный тип данных: " + text);
    }

    /**
     * Превращает кусочек запроса (токен) в значение нужного типа.
     * Строки и даты в запросе должны быть в одинарных кавычках.
     * Слово NULL даёт пустое значение.
     */
    public Object parseValue(String token) {
        if (token.equalsIgnoreCase("NULL")) {
            return null;
        }
        boolean inQuotes = token.startsWith("'");

        if (this == STRING || this == DATE) {
            if (!inQuotes) {
                throw new DbException("Значение " + token + " должно быть в одинарных кавычках");
            }
            String text = token.substring(1, token.length() - 1);
            if (this == STRING) {
                return text;
            }
            try {
                return LocalDate.parse(text);
            } catch (Exception e) {
                throw new DbException("Неверная дата '" + text + "', нужен формат ГГГГ-ММ-ДД");
            }
        }

        if (inQuotes) {
            throw new DbException("Значение " + token + " не подходит для типа " + this);
        }

        if (this == INT) {
            try {
                return Integer.parseInt(token);
            } catch (NumberFormatException e) {
                throw new DbException("Значение " + token + " не подходит для типа INT");
            }
        }
        if (this == DOUBLE) {
            try {
                return Double.parseDouble(token);
            } catch (NumberFormatException e) {
                throw new DbException("Значение " + token + " не подходит для типа DOUBLE");
            }
        }
        // остался BOOLEAN
        if (token.equalsIgnoreCase("TRUE")) {
            return Boolean.TRUE;
        }
        if (token.equalsIgnoreCase("FALSE")) {
            return Boolean.FALSE;
        }
        throw new DbException("Значение " + token + " не подходит для типа BOOLEAN");
    }
}
