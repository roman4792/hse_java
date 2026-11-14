package pizzadb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Таблица: список столбцов и список строк.
 * Следит за уникальностью первичного ключа и использует индексы при поиске.
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private List<Column> columns;
    private List<Row> rows = new ArrayList<Row>();
    private int primaryKeyIndex = -1;

    // Эти поля не сохраняются вместе с таблицей:
    // индексы лежат в своих файлах, а множество ключей собирается при запуске.
    private transient List<Index> indexes = new ArrayList<Index>();
    private transient Set<Object> primaryKeys = new HashSet<Object>();
    private transient String lastUsedIndex;

    public Table(String name, List<Column> columns) {
        this.name = name;
        this.columns = columns;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).isPrimaryKey() && primaryKeyIndex == -1) {
                primaryKeyIndex = i;
            }
        }
    }

    /**
     * Вызывается после загрузки таблицы из файла:
     * создаёт пустые служебные поля и заполняет множество ключей.
     */
    public void restoreAfterLoad() {
        indexes = new ArrayList<Index>();
        primaryKeys = new HashSet<Object>();
        for (int i = 0; i < rows.size(); i++) {
            primaryKeys.add(rows.get(i).get(primaryKeyIndex));
        }
    }

    public String getName() {
        return name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public int getPrimaryKeyIndex() {
        return primaryKeyIndex;
    }

    /**
     * Имя индекса, который использовался в последнем поиске (или null).
     */
    public String getLastUsedIndex() {
        return lastUsedIndex;
    }

    /**
     * Ищет столбец по имени. Если такого нет, вернёт -1.
     */
    public int findColumn(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasKey(Object key) {
        return primaryKeys.contains(key);
    }

    /**
     * Добавляет строку. Если такой первичный ключ уже есть, будет ошибка.
     */
    public void insertRow(Row row) {
        Object key = row.get(primaryKeyIndex);
        if (key == null) {
            throw new DbException("Первичный ключ не может быть NULL");
        }
        if (primaryKeys.contains(key)) {
            throw new DbException("Запись с ключом " + key + " уже есть в таблице " + name);
        }
        rows.add(row);
        primaryKeys.add(key);

        int rowNumber = rows.size() - 1;
        for (int i = 0; i < indexes.size(); i++) {
            Index index = indexes.get(i);
            index.add(row.get(index.getColumnPosition()), rowNumber);
        }
    }

    /**
     * Подходит ли строка под все условия сразу.
     */
    private boolean matchesAll(Row row, List<Condition> conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            if (!conditions.get(i).matches(row)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Находит строки по условиям. Если есть условие "=" по столбцу с индексом,
     * то берём кандидатов из индекса, иначе просматриваем всю таблицу.
     */
    public List<Row> findRows(List<Condition> conditions) {
        lastUsedIndex = null;
        List<Row> result = new ArrayList<Row>();

        for (int c = 0; c < conditions.size(); c++) {
            Condition cond = conditions.get(c);
            if (!cond.getOperator().equals("=")) {
                continue;
            }
            for (int i = 0; i < indexes.size(); i++) {
                Index index = indexes.get(i);
                if (index.getColumnPosition() == cond.getColumnIndex()) {
                    lastUsedIndex = index.getName();
                    List<Integer> numbers = index.find(cond.getValue());
                    for (int k = 0; k < numbers.size(); k++) {
                        Row row = rows.get(numbers.get(k));
                        if (matchesAll(row, conditions)) {
                            result.add(row);
                        }
                    }
                    return result;
                }
            }
        }

        // индекса нет - полный перебор
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (matchesAll(row, conditions)) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Удаляет переданные строки из таблицы.
     */
    public void removeRows(List<Row> toDelete) {
        rows.removeAll(new HashSet<Row>(toDelete));
        rebuildHelpers();
    }

    /**
     * Меняет значения в переданных строках.
     * columnIndexes[i] - номер столбца, newValues[i] - новое значение.
     */
    public void updateRows(List<Row> targets, int[] columnIndexes, Object[] newValues) {
        // сначала проверяем первичный ключ, чтобы не оставить таблицу наполовину изменённой
        for (int i = 0; i < columnIndexes.length; i++) {
            if (columnIndexes[i] != primaryKeyIndex) {
                continue;
            }
            Object newKey = newValues[i];
            if (newKey == null) {
                throw new DbException("Первичный ключ не может быть NULL");
            }
            if (targets.size() > 1) {
                throw new DbException("Нельзя присвоить один и тот же ключ нескольким записям");
            }
            if (targets.size() == 1) {
                Object oldKey = targets.get(0).get(primaryKeyIndex);
                if (!newKey.equals(oldKey) && primaryKeys.contains(newKey)) {
                    throw new DbException("Запись с ключом " + newKey + " уже есть в таблице " + name);
                }
            }
        }

        for (int r = 0; r < targets.size(); r++) {
            Row row = targets.get(r);
            for (int i = 0; i < columnIndexes.length; i++) {
                row.set(columnIndexes[i], newValues[i]);
            }
        }
        rebuildHelpers();
    }

    /**
     * Добавляет индекс в таблицу и сразу заполняет его.
     */
    public void addIndex(Index index) {
        index.rebuild(rows);
        indexes.add(index);
    }

    /**
     * Пересобирает множество ключей и все индексы
     * (после удаления или обновления номера строк могли поменяться).
     */
    private void rebuildHelpers() {
        primaryKeys = new HashSet<Object>();
        for (int i = 0; i < rows.size(); i++) {
            primaryKeys.add(rows.get(i).get(primaryKeyIndex));
        }
        for (int i = 0; i < indexes.size(); i++) {
            indexes.get(i).rebuild(rows);
        }
    }
}
