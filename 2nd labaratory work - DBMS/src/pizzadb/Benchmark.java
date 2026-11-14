package pizzadb;

import java.io.File;
import java.io.PrintStream;

/**
 * Замер скорости SELECT без индекса и с индексом.
 * Запуск: java -cp out pizzadb.Benchmark
 */
public class Benchmark {

    private static final int CUSTOMERS = 1000;
    private static final int ORDERS = 200000;
    private static final int REPEATS = 100;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        // чистая папка для теста, чтобы не трогать обычные данные
        File folder = new File("bench_data");
        File[] old = folder.listFiles();
        if (old != null) {
            for (int i = 0; i < old.length; i++) {
                old[i].delete();
            }
        }

        Database db = new Database("bench_data");
        db.setAutoSave(false); // пишем на диск один раз в конце
        SqlExecutor executor = new SqlExecutor(db);

        executor.execute("CREATE TABLE customers (id INT PRIMARY KEY, name STRING)");
        executor.execute("CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT REFERENCES customers, "
                + "pizza STRING, size_cm INT, dough STRING, price DOUBLE, order_date DATE, delivered BOOLEAN)");

        System.out.println("Заполняю таблицы: клиентов " + CUSTOMERS + ", заказов " + ORDERS);
        fillCustomers(executor);
        fillOrders(executor);

        String[] queries = new String[3];
        queries[0] = "SELECT * FROM orders WHERE id = 150000";
        queries[1] = "SELECT * FROM orders WHERE customer_id = 777";
        queries[2] = "SELECT * FROM orders WHERE pizza = 'Margherita'";

        long[] withoutIndex = new long[3];
        long[] withIndex = new long[3];
        int[] foundRows = new int[3];

        for (int i = 0; i < queries.length; i++) {
            foundRows[i] = executor.execute(queries[i]).getRows().size();
            withoutIndex[i] = measure(executor, queries[i]);
        }

        executor.execute("CREATE INDEX idx_orders_id ON orders (id)");
        executor.execute("CREATE INDEX idx_orders_customer ON orders (customer_id)");
        executor.execute("CREATE INDEX idx_orders_pizza ON orders (pizza)");

        for (int i = 0; i < queries.length; i++) {
            withIndex[i] = measure(executor, queries[i]);
        }

        System.out.println();
        System.out.println("Среднее время одного запроса (мс), " + REPEATS + " повторов:");
        for (int i = 0; i < queries.length; i++) {
            double a = withoutIndex[i] / 1000000.0 / REPEATS;
            double b = withIndex[i] / 1000000.0 / REPEATS;
            System.out.println(queries[i]);
            System.out.println("  найдено строк: " + foundRows[i]);
            System.out.println("  без индекса: " + format(a) + " мс, с индексом: " + format(b)
                    + " мс, ускорение: " + format(a / b) + " раз");
        }

        // сохранение и повторная загрузка с диска
        long start = System.nanoTime();
        db.saveAll();
        long saveTime = System.nanoTime() - start;
        start = System.nanoTime();
        Database loaded = new Database("bench_data");
        long loadTime = System.nanoTime() - start;
        System.out.println();
        System.out.println("Запись всей базы на диск: " + format(saveTime / 1000000.0) + " мс");
        System.out.println("Загрузка с диска: " + format(loadTime / 1000000.0) + " мс, заказов: "
                + loaded.getTable("orders").getRows().size());
    }

    private static long measure(SqlExecutor executor, String sql) {
        executor.execute(sql); // прогрев
        long start = System.nanoTime();
        for (int i = 0; i < REPEATS; i++) {
            executor.execute(sql);
        }
        return System.nanoTime() - start;
    }

    private static void fillCustomers(SqlExecutor executor) {
        StringBuilder sb = new StringBuilder("INSERT INTO customers VALUES ");
        for (int i = 1; i <= CUSTOMERS; i++) {
            if (i > 1) {
                sb.append(", ");
            }
            sb.append("(").append(i).append(", 'Customer").append(i).append("')");
        }
        executor.execute(sb.toString());
    }

    private static void fillOrders(SqlExecutor executor) {
        String[] pizzas = {"Margherita", "Pepperoni", "Four Cheese", "Hawaiian",
                "Diavola", "Capricciosa", "Marinara", "Veggie"};
        int[] sizes = {25, 30, 35, 40};
        String[] doughs = {"thin", "thick"};

        int id = 1;
        while (id <= ORDERS) {
            // вставляем пачками по 1000 строк
            StringBuilder sb = new StringBuilder("INSERT INTO orders VALUES ");
            for (int k = 0; k < 1000 && id <= ORDERS; k++) {
                if (k > 0) {
                    sb.append(", ");
                }
                int day = id % 28 + 1;
                String date = "2026-09-" + (day < 10 ? "0" + day : "" + day);
                sb.append("(").append(id).append(", ")
                        .append(id % CUSTOMERS + 1).append(", '")
                        .append(pizzas[id % pizzas.length]).append("', ")
                        .append(sizes[id % sizes.length]).append(", '")
                        .append(doughs[id % 2]).append("', ")
                        .append(300 + id % 500).append(".0, '")
                        .append(date).append("', ")
                        .append(id % 3 == 0 ? "TRUE" : "FALSE").append(")");
                id++;
            }
            executor.execute(sb.toString());
        }
    }

    private static String format(double value) {
        return String.format("%.3f", value);
    }
}
