import java.util.ArrayList;
import java.util.Scanner;

// Консольный интерфейс для работы с матрицами комплексных чисел
public class Main {
    static Scanner scanner = new Scanner(System.in);
    static ArrayList<ComplexMatrix> matrices = new ArrayList<ComplexMatrix>();

    public static void main(String[] args) {
        boolean work = true;
        while (work) {
            printMenu();
            int choice = readInt("Ваш выбор: ");

            if (choice == 1) {
                createMatrix();
            } else if (choice == 2) {
                showMatrices();
            } else if (choice == 3) {
                addMatrices();
            } else if (choice == 4) {
                multiplyMatrices();
            } else if (choice == 5) {
                divideMatrices();
            } else if (choice == 6) {
                transposeMatrix();
            } else if (choice == 7) {
                findDeterminant();
            } else if (choice == 0) {
                work = false;
                System.out.println("Пока!");
            } else {
                System.out.println("Нет такого пункта меню");
            }
        }
    }

    static void printMenu() {
        System.out.println();
        System.out.println("===== Матрицы комплексных чисел =====");
        System.out.println("1 - Создать матрицу");
        System.out.println("2 - Показать все матрицы");
        System.out.println("3 - Сложить две матрицы");
        System.out.println("4 - Перемножить две матрицы");
        System.out.println("5 - Разделить одну матрицу на другую");
        System.out.println("6 - Транспонировать матрицу");
        System.out.println("7 - Вычислить определитель");
        System.out.println("0 - Выход");
    }

    // читаем целое число, пока пользователь не введёт нормально
    static int readInt(String message) {
        System.out.print(message);
        while (true) {
            String s = scanner.next();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.print("Это не целое число, попробуйте ещё раз: ");
            }
        }
    }

    // читаем дробное число (дробная часть через точку)
    static double readDouble() {
        while (true) {
            String s = scanner.next();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.print("Это не число, попробуйте ещё раз: ");
            }
        }
    }

    static void createMatrix() {
        int rows = readInt("Количество строк: ");
        int cols = readInt("Количество столбцов: ");
        if (rows <= 0 || cols <= 0) {
            System.out.println("Размеры должны быть больше нуля");
            return;
        }

        ComplexMatrix m = new ComplexMatrix(rows, cols);
        System.out.println("Вводите элементы: сначала действительную часть, потом мнимую (через пробел).");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print("Элемент [" + (i + 1) + "][" + (j + 1) + "]: ");
                double re = readDouble();
                double im = readDouble();
                m.set(i, j, new Complex(re, im));
            }
        }

        matrices.add(m);
        System.out.println("Матрица сохранена под номером " + matrices.size());
        System.out.println(m);
    }

    static void showMatrices() {
        if (matrices.size() == 0) {
            System.out.println("Пока нет ни одной матрицы");
            return;
        }
        for (int i = 0; i < matrices.size(); i++) {
            ComplexMatrix m = matrices.get(i);
            System.out.println("Матрица №" + (i + 1) + " (" + m.getRows() + "x" + m.getCols() + "):");
            System.out.println(m);
        }
    }

    // просим выбрать матрицу по номеру, возвращаем её
    static ComplexMatrix chooseMatrix(String message) {
        int number = readInt(message);
        while (number < 1 || number > matrices.size()) {
            number = readInt("Нет матрицы с таким номером, попробуйте ещё раз: ");
        }
        return matrices.get(number - 1);
    }

    // результат операции сохраняем как новую матрицу
    static void saveResult(ComplexMatrix result) {
        matrices.add(result);
        System.out.println("Результат (сохранён как матрица №" + matrices.size() + "):");
        System.out.println(result);
    }

    static void addMatrices() {
        if (matrices.size() == 0) {
            System.out.println("Сначала создайте хотя бы одну матрицу");
            return;
        }
        ComplexMatrix a = chooseMatrix("Номер первой матрицы: ");
        ComplexMatrix b = chooseMatrix("Номер второй матрицы: ");
        try {
            saveResult(a.add(b));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    static void multiplyMatrices() {
        if (matrices.size() == 0) {
            System.out.println("Сначала создайте хотя бы одну матрицу");
            return;
        }
        ComplexMatrix a = chooseMatrix("Номер первой матрицы: ");
        ComplexMatrix b = chooseMatrix("Номер второй матрицы: ");
        try {
            saveResult(a.multiply(b));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    static void divideMatrices() {
        if (matrices.size() == 0) {
            System.out.println("Сначала создайте хотя бы одну матрицу");
            return;
        }
        ComplexMatrix a = chooseMatrix("Номер делимой матрицы (A): ");
        ComplexMatrix b = chooseMatrix("Номер матрицы-делителя (B): ");
        try {
            saveResult(a.divide(b));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (ArithmeticException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    static void transposeMatrix() {
        if (matrices.size() == 0) {
            System.out.println("Сначала создайте хотя бы одну матрицу");
            return;
        }
        ComplexMatrix a = chooseMatrix("Номер матрицы: ");
        saveResult(a.transpose());
    }

    static void findDeterminant() {
        if (matrices.size() == 0) {
            System.out.println("Сначала создайте хотя бы одну матрицу");
            return;
        }
        ComplexMatrix a = chooseMatrix("Номер матрицы: ");
        try {
            System.out.println("Определитель: " + a.determinant());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
