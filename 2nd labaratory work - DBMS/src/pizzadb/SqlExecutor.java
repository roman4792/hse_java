package pizzadb;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбирает SQL-подобный запрос и выполняет его в базе.
 * Поддерживается: CREATE TABLE, DROP TABLE, CREATE INDEX,
 * INSERT INTO, SELECT, UPDATE, DELETE.
 */
public class SqlExecutor {

    private Database db;
    private List<String> tokens; // запрос, разрезанный на кусочки
    private int pos;             // номер кусочка, который читаем сейчас

    public SqlExecutor(Database db) {
        this.db = db;
    }

    /**
     * Выполняет один запрос и возвращает результат.
     */
    public QueryResult execute(String sql) {
        tokens = Tokenizer.split(sql);
        pos = 0;

        // точку с запятой в конце отбрасываем
        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).equals(";")) {
            tokens.remove(tokens.size() - 1);
        }
        if (tokens.isEmpty()) {
            throw new DbException("Пустой запрос");
        }

        String first = tokens.get(0).toUpperCase();
        QueryResult result;

        if (first.equals("INSERT")) {
            result = doInsert();
        } else if (first.equals("SELECT")) {
            result = doSelect();
        } else if (first.equals("UPDATE")) {
            result = doUpdate();
        } else if (first.equals("DELETE")) {
            result = doDelete();
        } else if (first.equals("CREATE")) {
            if (tokens.size() > 1 && tokens.get(1).equalsIgnoreCase("TABLE")) {
                result = doCreateTable();
            } else if (tokens.size() > 1 && tokens.get(1).equalsIgnoreCase("INDEX")) {
                result = doCreateIndex();
            } else {
                throw new DbException("После CREATE ожидалось TABLE или INDEX");
            }
        } else if (first.equals("DROP")) {
            result = doDropTable();
        } else {
            throw new DbException("Неизвестная команда: " + tokens.get(0));
        }

        if (pos < tokens.size()) {
            throw new DbException("Лишнее в конце запроса: " + tokens.get(pos));
        }
        return result;
    }

    // ---------- маленькие помощники для чтения токенов ----------

    private String next() {
        if (pos >= tokens.size()) {
            throw new DbException("Запрос оборван, ожидалось продолжение");
        }
        String token = tokens.get(pos);
        pos++;
        return token;
    }

    private String peek() {
        if (pos >= tokens.size()) {
            return null;
        }
        return tokens.get(pos);
    }

    private boolean peekIs(String word) {
        String token = peek();
        return token != null && token.equalsIgnoreCase(word);
    }

    private void expect(String word) {
        String token = next();
        if (!token.equalsIgnoreCase(word)) {
            throw new DbException("Ожидалось '" + word + "', а найдено '" + token + "'");
        }
    }

    private boolean isName(String token) {
        char first = token.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    private String nextName() {
        String token = next();
        if (!isName(token)) {
            throw new DbException("Ожидалось имя, а найдено '" + token + "'");
        }
        return token;
    }

    private int columnIndex(Table table, String columnName) {
        int index = table.findColumn(columnName);
        if (index == -1) {
            throw new DbException("В таблице " + table.getName() + " нет столбца " + columnName);
        }
        return index;
    }

    /**
     * Читает значение и приводит его к типу столбца.
     */
    private Object readValue(Column column) {
        String token = next();
        try {
            return column.getType().parseValue(token);
        } catch (DbException e) {
            throw new DbException("Столбец " + column.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Читает необязательную часть WHERE (условия через AND).
     */
    private List<Condition> parseWhere(Table table) {
        List<Condition> list = new ArrayList<Condition>();
        if (!peekIs("WHERE")) {
            return list;
        }
        pos++;

        while (true) {
            String columnName = nextName();
            int index = columnIndex(table, columnName);
            Column column = table.getColumns().get(index);

            String op = next();
            if (op.equals("<>")) {
                op = "!=";
            }
            if (!op.equals("=") && !op.equals("!=") && !op.equals("<") && !op.equals(">")
                    && !op.equals("<=") && !op.equals(">=")) {
                throw new DbException("Неизвестный оператор сравнения: " + op);
            }

            Object value = readValue(column);
            list.add(new Condition(index, column.getType(), op, value));

            if (peekIs("AND")) {
                pos++;
            } else {
                break;
            }
        }
        return list;
    }

    // ---------- сами команды ----------

    private QueryResult doCreateTable() {
        expect("CREATE");
        expect("TABLE");
        String tableName = nextName();
        expect("(");

        List<Column> columns = new ArrayList<Column>();
        while (true) {
            String columnName = nextName();
            DataType type = DataType.fromString(nextName());
            boolean primaryKey = false;
            String refTable = null;

            while (peekIs("PRIMARY") || peekIs("REFERENCES")) {
                if (peekIs("PRIMARY")) {
                    pos++;
                    expect("KEY");
                    primaryKey = true;
                } else {
                    pos++;
                    refTable = nextName();
                }
            }
            columns.add(new Column(columnName, type, primaryKey, refTable));

            String token = next();
            if (token.equals(")")) {
                break;
            }
            if (!token.equals(",")) {
                throw new DbException("Ожидалась ',' или ')', а найдено '" + token + "'");
            }
        }

        db.createTable(new Table(tableName, columns));
        return new QueryResult("Таблица " + tableName + " создана");
    }

    private QueryResult doDropTable() {
        expect("DROP");
        expect("TABLE");
        String tableName = nextName();
        db.dropTable(tableName);
        return new QueryResult("Таблица " + tableName + " удалена");
    }

    private QueryResult doCreateIndex() {
        expect("CREATE");
        expect("INDEX");
        String indexName = nextName();
        expect("ON");
        String tableName = nextName();
        expect("(");
        String columnName = nextName();
        expect(")");
        db.createIndex(indexName, tableName, columnName);
        return new QueryResult("Индекс " + indexName + " создан");
    }

    private QueryResult doInsert() {
        expect("INSERT");
        expect("INTO");
        String tableName = nextName();
        Table table = db.getTable(tableName);
        List<Column> columns = table.getColumns();

        // в какие столбцы кладём значения
        List<Integer> targets = new ArrayList<Integer>();
        if (peekIs("(")) {
            pos++;
            while (true) {
                String columnName = nextName();
                int index = columnIndex(table, columnName);
                if (targets.contains(index)) {
                    throw new DbException("Столбец " + columnName + " указан дважды");
                }
                targets.add(index);
                String token = next();
                if (token.equals(")")) {
                    break;
                }
                if (!token.equals(",")) {
                    throw new DbException("Ожидалась ',' или ')', а найдено '" + token + "'");
                }
            }
        } else {
            for (int i = 0; i < columns.size(); i++) {
                targets.add(i);
            }
        }

        expect("VALUES");

        List<Row> newRows = new ArrayList<Row>();
        while (true) {
            expect("(");
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < targets.size(); i++) {
                int columnNumber = targets.get(i);
                values[columnNumber] = readValue(columns.get(columnNumber));
                if (i < targets.size() - 1) {
                    expect(",");
                }
            }
            expect(")");
            newRows.add(new Row(values));

            if (peekIs(",")) {
                pos++;
            } else {
                break;
            }
        }

        db.insertAll(tableName, newRows);
        return new QueryResult("Добавлено записей: " + newRows.size());
    }

    private QueryResult doSelect() {
        expect("SELECT");

        List<String> wanted = new ArrayList<String>();
        boolean all = false;
        if (peekIs("*")) {
            pos++;
            all = true;
        } else {
            while (true) {
                wanted.add(nextName());
                if (peekIs(",")) {
                    pos++;
                } else {
                    break;
                }
            }
        }

        expect("FROM");
        String tableName = nextName();
        Table table = db.getTable(tableName);
        List<Condition> conditions = parseWhere(table);

        // номера и названия столбцов, которые нужно вывести
        List<Integer> columnNumbers = new ArrayList<Integer>();
        List<String> columnNames = new ArrayList<String>();
        if (all) {
            for (int i = 0; i < table.getColumns().size(); i++) {
                columnNumbers.add(i);
                columnNames.add(table.getColumns().get(i).getName());
            }
        } else {
            for (int i = 0; i < wanted.size(); i++) {
                int index = columnIndex(table, wanted.get(i));
                columnNumbers.add(index);
                columnNames.add(table.getColumns().get(index).getName());
            }
        }

        List<Row> found = table.findRows(conditions);

        List<Object[]> resultRows = new ArrayList<Object[]>();
        for (int r = 0; r < found.size(); r++) {
            Object[] line = new Object[columnNumbers.size()];
            for (int c = 0; c < columnNumbers.size(); c++) {
                line[c] = found.get(r).get(columnNumbers.get(c));
            }
            resultRows.add(line);
        }

        String message = "Строк: " + resultRows.size();
        if (table.getLastUsedIndex() != null) {
            message = message + " (использован индекс " + table.getLastUsedIndex() + ")";
        }
        return new QueryResult(columnNames, resultRows, message);
    }

    private QueryResult doUpdate() {
        expect("UPDATE");
        String tableName = nextName();
        Table table = db.getTable(tableName);
        expect("SET");

        List<Integer> columnNumbers = new ArrayList<Integer>();
        List<Object> values = new ArrayList<Object>();
        while (true) {
            String columnName = nextName();
            int index = columnIndex(table, columnName);
            expect("=");
            Object value = readValue(table.getColumns().get(index));
            columnNumbers.add(index);
            values.add(value);

            if (peekIs(",")) {
                pos++;
            } else {
                break;
            }
        }

        List<Condition> conditions = parseWhere(table);

        int[] columnArray = new int[columnNumbers.size()];
        Object[] valueArray = new Object[values.size()];
        for (int i = 0; i < columnArray.length; i++) {
            columnArray[i] = columnNumbers.get(i);
            valueArray[i] = values.get(i);
        }

        int count = db.update(tableName, conditions, columnArray, valueArray);
        return new QueryResult("Обновлено записей: " + count);
    }

    private QueryResult doDelete() {
        expect("DELETE");
        expect("FROM");
        String tableName = nextName();
        Table table = db.getTable(tableName);
        List<Condition> conditions = parseWhere(table);

        int count = db.delete(tableName, conditions);
        return new QueryResult("Удалено записей: " + count);
    }
}
