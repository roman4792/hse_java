-- Пример данных для пиццерии. Запуск в консоли: run sample.sql
CREATE TABLE customers (id INT PRIMARY KEY, name STRING, phone STRING, address STRING)
CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT REFERENCES customers, pizza STRING, size_cm INT, dough STRING, price DOUBLE, order_date DATE, delivered BOOLEAN)
INSERT INTO customers VALUES (1, 'Иван Петров', '+7-900-111-22-33', 'ул. Ленина, 5'), (2, 'Мария Сидорова', '+7-900-222-33-44', 'пр. Мира, 12'), (3, 'Олег Громов', '+7-900-333-44-55', 'ул. Гагарина, 7')
INSERT INTO orders VALUES (1, 1, 'Маргарита', 35, 'тонкое', 650.0, '2026-10-01', TRUE)
INSERT INTO orders VALUES (2, 2, 'Пепперони', 30, 'толстое', 720.5, '2026-10-01', FALSE), (3, 1, 'Четыре сыра', 40, 'тонкое', 890.0, '2026-10-02', FALSE), (4, 3, 'Маргарита', 25, 'тонкое', 480.0, '2026-10-03', TRUE)
INSERT INTO orders (id, customer_id, pizza) VALUES (5, 2, 'Гавайская')
SELECT * FROM customers
SELECT * FROM orders
SELECT pizza, size_cm, price FROM orders WHERE price > 600
SELECT * FROM orders WHERE pizza = 'Маргарита' AND dough = 'тонкое'
UPDATE orders SET delivered = TRUE WHERE id = 2
SELECT id, pizza, delivered FROM orders WHERE delivered = TRUE
CREATE INDEX idx_orders_pizza ON orders (pizza)
SELECT * FROM orders WHERE pizza = 'Маргарита'
