package pizzadb;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Консольный интерфейс. Запросы вводятся по одному в строке.
 * Команды: help, tables, run файл.sql, exit
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        String folder = "data";
        if (args.length > 0) {
            folder = args[0];
        }

        Database db = new Database(folder);
        SqlExecutor executor = new SqlExecutor(db);
        Scanner scanner = new Scanner(System.in, "UTF-8");

        System.out.println("PizzaDB - простая СУБД для пиццерии. Введите help для справки.");
        System.out.println("Папка с данными: " + folder);

        while (true) {
            System.out.print("pizzadb> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            if (line.equalsIgnoreCase("help")) {
                printHelp();
            } else if (line.equalsIgnoreCase("tables")) {
                printTables(db);
            } else if (line.toLowerCase().startsWith("run ")) {
                runFile(executor, line.substring(4).trim());
            } else {
                runQuery(executor, line);
            }
        }
        System.out.println("До свидания!");
    }

    private static void runQuery(SqlExecutor executor, String sql) {
        try {
            QueryResult result = executor.execute(sql);
            TablePrinter.print(result);
        } catch (DbException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void runFile(SqlExecutor executor, String fileName) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("Не удалось прочитать файл " + fileName);
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            System.out.println("> " + line);
            runQuery(executor, line);
        }
    }

    private static void printTables(Database db) {
        if (db.getTables().isEmpty()) {
            System.out.println("Таблиц пока нет");
            return;
        }
        for (Table table : db.getTables()) {
            System.out.println(table.getName() + " (записей: " + table.getRows().size() + ")");
            for (int i = 0; i < table.getColumns().size(); i++) {
                Column c = table.getColumns().get(i);
                String text = "  " + c.getName() + " " + c.getType();
                if (c.isPrimaryKey()) {
                    text = text + " PRIMARY KEY";
                }
                if (c.getRefTable() != null) {
                    text = text + " REFERENCES " + c.getRefTable();
                }
                System.out.println(text);
            }
            for (int i = 0; i < table.getIndexes().size(); i++) {
                Index index = table.getIndexes().get(i);
                System.out.println("  индекс " + index.getName() + " по столбцу " + index.getColumnName());
            }
        }
    }

    private static void printHelp() {
        System.out.println("Команды:");
        System.out.println("  tables            - показать таблицы и их столбцы");
        System.out.println("  run файл.sql      - выполнить запросы из файла (по одному в строке)");
        System.out.println("  exit              - выход");
        System.out.println("Примеры запросов:");
        System.out.println("  CREATE TABLE customers (id INT PRIMARY KEY, name STRING)");
        System.out.println("  CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT REFERENCES customers, price DOUBLE)");
        System.out.println("  INSERT INTO customers VALUES (1, 'Иван')");
        System.out.println("  INSERT INTO customers (id) VALUES (2), (3)");
        System.out.println("  SELECT * FROM orders");
        System.out.println("  SELECT pizza, price FROM orders WHERE price > 500 AND size_cm = 35");
        System.out.println("  UPDATE orders SET delivered = TRUE WHERE id = 2");
        System.out.println("  DELETE FROM orders WHERE pizza = 'Маргарита'");
        System.out.println("  CREATE INDEX idx_orders_pizza ON orders (pizza)");
        System.out.println("  DROP TABLE orders");
    }
}
