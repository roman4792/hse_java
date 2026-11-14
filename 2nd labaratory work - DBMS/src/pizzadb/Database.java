package pizzadb;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * База данных: хранит все таблицы, проверяет связи между ними (внешние ключи)
 * и сохраняет изменения на диск. Это что-то вроде Repository.
 */
public class Database {

    private Map<String, Table> tables = new LinkedHashMap<String, Table>();
    private FileStorage storage;
    private boolean autoSave = true;

    /**
     * Открывает базу из папки: читает таблицы и индексы с диска.
     */
    public Database(String folder) {
        storage = new FileStorage(folder);

        List<Table> loadedTables = storage.loadTables();
        for (int i = 0; i < loadedTables.size(); i++) {
            Table table = loadedTables.get(i);
            table.restoreAfterLoad();
            tables.put(table.getName().toLowerCase(), table);
        }

        List<Index> loadedIndexes = storage.loadIndexes();
        for (int i = 0; i < loadedIndexes.size(); i++) {
            Index index = loadedIndexes.get(i);
            Table table = tables.get(index.getTableName().toLowerCase());
            if (table == null) {
                continue; // индекс без таблицы - просто мусор
            }
            // если программа упала между записью таблицы и индекса, индекс устарел
            if (index.getIndexedRows() != table.getRows().size()) {
                System.out.println("Индекс " + index.getName() + " не совпал с таблицей, перестраиваю");
                index.rebuild(table.getRows());
            }
            table.getIndexes().add(index);
        }
    }

    /**
     * Включает/выключает запись на диск после каждого запроса.
     * Выключать нужно только для больших загрузок (например, в тесте скорости).
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public boolean hasTable(String name) {
        return tables.containsKey(name.toLowerCase());
    }

    public Table getTable(String name) {
        Table table = tables.get(name.toLowerCase());
        if (table == null) {
            throw new DbException("Таблицы " + name + " нет");
        }
        return table;
    }

    public Collection<Table> getTables() {
        return tables.values();
    }

    public void createTable(Table table) {
        if (hasTable(table.getName())) {
            throw new DbException("Таблица " + table.getName() + " уже существует");
        }

        List<Column> columns = table.getColumns();
        int keyCount = 0;
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if (column.isPrimaryKey()) {
                keyCount++;
            }
            for (int j = 0; j < i; j++) {
                if (columns.get(j).getName().equalsIgnoreCase(column.getName())) {
                    throw new DbException("Столбец " + column.getName() + " указан дважды");
                }
            }
            if (column.getRefTable() != null) {
                if (!hasTable(column.getRefTable())) {
                    throw new DbException("Таблицы " + column.getRefTable() + " нет, на неё нельзя ссылаться");
                }
                Table parent = getTable(column.getRefTable());
                Column parentKey = parent.getColumns().get(parent.getPrimaryKeyIndex());
                if (parentKey.getType() != column.getType()) {
                    throw new DbException("Тип столбца " + column.getName()
                            + " не совпадает с типом ключа таблицы " + parent.getName());
                }
            }
        }
        if (keyCount != 1) {
            throw new DbException("В таблице должен быть ровно один столбец PRIMARY KEY");
        }

        tables.put(table.getName().toLowerCase(), table);
        saveTable(table);
    }

    public void dropTable(String name) {
        Table table = getTable(name);
        // нельзя удалять таблицу, на которую кто-то ссылается
        for (Table other : tables.values()) {
            if (other == table) {
                continue;
            }
            for (int i = 0; i < other.getColumns().size(); i++) {
                String ref = other.getColumns().get(i).getRefTable();
                if (ref != null && ref.equalsIgnoreCase(table.getName())) {
                    throw new DbException("На таблицу " + table.getName()
                            + " ссылается таблица " + other.getName() + ", удалять нельзя");
                }
            }
        }
        tables.remove(table.getName().toLowerCase());
        storage.deleteTable(table);
    }

    public void createIndex(String indexName, String tableName, String columnName) {
        Table table = getTable(tableName);
        int position = table.findColumn(columnName);
        if (position == -1) {
            throw new DbException("В таблице " + table.getName() + " нет столбца " + columnName);
        }

        for (Table t : tables.values()) {
            for (int i = 0; i < t.getIndexes().size(); i++) {
                Index old = t.getIndexes().get(i);
                if (old.getName().equalsIgnoreCase(indexName)) {
                    throw new DbException("Индекс " + indexName + " уже существует");
                }
            }
        }
        for (int i = 0; i < table.getIndexes().size(); i++) {
            if (table.getIndexes().get(i).getColumnPosition() == position) {
                throw new DbException("На этот столбец уже есть индекс");
            }
        }

        String realColumn = table.getColumns().get(position).getName();
        Index index = new Index(indexName.toLowerCase(), table.getName(), realColumn, position);
        table.addIndex(index);
        saveTable(table);
    }

    /**
     * Вставляет сразу несколько строк. Если хоть одна не подходит,
     * не вставляется ни одна.
     */
    public void insertAll(String tableName, List<Row> newRows) {
        Table table = getTable(tableName);
        int keyIndex = table.getPrimaryKeyIndex();

        Set<Object> keysInBatch = new HashSet<Object>();
        for (int i = 0; i < newRows.size(); i++) {
            Row row = newRows.get(i);
            Object key = row.get(keyIndex);
            if (key == null) {
                throw new DbException("Первичный ключ не может быть NULL");
            }
            if (table.hasKey(key) || !keysInBatch.add(key)) {
                throw new DbException("Запись с ключом " + key + " уже есть в таблице " + table.getName());
            }
            checkForeignKeys(table, row);
        }

        for (int i = 0; i < newRows.size(); i++) {
            table.insertRow(newRows.get(i));
        }
        saveTable(table);
    }

    public int update(String tableName, List<Condition> conditions, int[] columnIndexes, Object[] newValues) {
        Table table = getTable(tableName);
        List<Row> targets = table.findRows(conditions);

        for (int i = 0; i < columnIndexes.length; i++) {
            Column column = table.getColumns().get(columnIndexes[i]);

            // новое значение внешнего ключа должно существовать в родительской таблице
            if (column.getRefTable() != null && newValues[i] != null) {
                Table parent = getTable(column.getRefTable());
                if (!parent.hasKey(newValues[i])) {
                    throw new DbException("В таблице " + parent.getName() + " нет записи с ключом " + newValues[i]);
                }
            }

            // если меняем ключ, на него не должны ссылаться другие таблицы
            if (columnIndexes[i] == table.getPrimaryKeyIndex()) {
                if (newValues[i] == null) {
                    throw new DbException("Первичный ключ не может быть NULL");
                }
                for (int r = 0; r < targets.size(); r++) {
                    Object oldKey = targets.get(r).get(columnIndexes[i]);
                    if (!newValues[i].equals(oldKey) && isReferenced(table, oldKey)) {
                        throw new DbException("На запись с ключом " + oldKey + " ссылаются другие таблицы");
                    }
                }
            }
        }

        table.updateRows(targets, columnIndexes, newValues);
        saveTable(table);
        return targets.size();
    }

    public int delete(String tableName, List<Condition> conditions) {
        Table table = getTable(tableName);
        List<Row> targets = table.findRows(conditions);

        for (int i = 0; i < targets.size(); i++) {
            Object key = targets.get(i).get(table.getPrimaryKeyIndex());
            if (isReferenced(table, key)) {
                throw new DbException("Нельзя удалить запись с ключом " + key
                        + ": на неё ссылаются записи из другой таблицы");
            }
        }

        table.removeRows(targets);
        saveTable(table);
        return targets.size();
    }

    /**
     * Записывает все таблицы и индексы на диск.
     */
    public void saveAll() {
        for (Table table : tables.values()) {
            storage.saveTable(table);
            for (int i = 0; i < table.getIndexes().size(); i++) {
                storage.saveIndex(table.getIndexes().get(i));
            }
        }
    }

    private void saveTable(Table table) {
        if (!autoSave) {
            return;
        }
        // сначала таблица, потом её индексы
        storage.saveTable(table);
        for (int i = 0; i < table.getIndexes().size(); i++) {
            storage.saveIndex(table.getIndexes().get(i));
        }
    }

    /**
     * Проверяет, что все внешние ключи строки указывают на существующие записи.
     */
    private void checkForeignKeys(Table table, Row row) {
        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            Object value = row.get(i);
            if (column.getRefTable() != null && value != null) {
                Table parent = getTable(column.getRefTable());
                if (!parent.hasKey(value)) {
                    throw new DbException("В таблице " + parent.getName() + " нет записи с ключом " + value
                            + " (столбец " + column.getName() + ")");
                }
            }
        }
    }

    /**
     * Есть ли в других таблицах строки, которые ссылаются на этот ключ.
     */
    private boolean isReferenced(Table parent, Object key) {
        for (Table table : tables.values()) {
            for (int c = 0; c < table.getColumns().size(); c++) {
                String ref = table.getColumns().get(c).getRefTable();
                if (ref == null || !ref.equalsIgnoreCase(parent.getName())) {
                    continue;
                }
                List<Row> rows = table.getRows();
                for (int r = 0; r < rows.size(); r++) {
                    if (key.equals(rows.get(r).get(c))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
