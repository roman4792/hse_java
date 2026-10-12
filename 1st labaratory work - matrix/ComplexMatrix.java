// Матрица комплексных чисел
public class ComplexMatrix {
    private int rows;
    private int cols;
    private Complex[][] data;

    // создаёт матрицу, заполненную нулями
    public ComplexMatrix(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Размеры матрицы должны быть положительными");
        }
        this.rows = rows;
        this.cols = cols;
        data = new Complex[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = new Complex(0, 0);
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Complex get(int i, int j) {
        return data[i][j];
    }

    public void set(int i, int j, Complex value) {
        data[i][j] = value;
    }

    // сложение матриц
    public ComplexMatrix add(ComplexMatrix other) {
        if (rows != other.rows || cols != other.cols) {
            throw new IllegalArgumentException("Для сложения размеры матриц должны совпадать");
        }
        ComplexMatrix result = new ComplexMatrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result.data[i][j] = data[i][j].add(other.data[i][j]);
            }
        }
        return result;
    }

    // умножение матриц
    public ComplexMatrix multiply(ComplexMatrix other) {
        if (cols != other.rows) {
            throw new IllegalArgumentException(
                    "Для умножения число столбцов первой матрицы должно быть равно числу строк второй");
        }
        ComplexMatrix result = new ComplexMatrix(rows, other.cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < other.cols; j++) {
                Complex sum = new Complex(0, 0);
                for (int k = 0; k < cols; k++) {
                    sum = sum.add(data[i][k].multiply(other.data[k][j]));
                }
                result.data[i][j] = sum;
            }
        }
        return result;
    }

    // транспонирование (строки становятся столбцами)
    public ComplexMatrix transpose() {
        ComplexMatrix result = new ComplexMatrix(cols, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result.data[j][i] = data[i][j];
            }
        }
        return result;
    }

    // определитель методом Гаусса
    public Complex determinant() {
        if (rows != cols) {
            throw new IllegalArgumentException("Определитель есть только у квадратной матрицы");
        }
        int n = rows;

        // работаем с копией, чтобы не испортить исходную матрицу
        Complex[][] a = new Complex[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = data[i][j];
            }
        }

        Complex det = new Complex(1, 0);

        for (int col = 0; col < n; col++) {
            // ищем строку с самым большим по модулю элементом в этом столбце
            int pivotRow = col;
            for (int r = col + 1; r < n; r++) {
                if (a[r][col].abs() > a[pivotRow][col].abs()) {
                    pivotRow = r;
                }
            }

            // если там ноль, то определитель равен нулю
            if (a[pivotRow][col].isZero()) {
                return new Complex(0, 0);
            }

            // меняем строки местами, при этом знак определителя меняется
            if (pivotRow != col) {
                Complex[] tmp = a[pivotRow];
                a[pivotRow] = a[col];
                a[col] = tmp;
                det = det.multiply(new Complex(-1, 0));
            }

            det = det.multiply(a[col][col]);

            // зануляем всё, что стоит под главным элементом
            for (int r = col + 1; r < n; r++) {
                Complex factor = a[r][col].divide(a[col][col]);
                for (int k = col; k < n; k++) {
                    a[r][k] = a[r][k].subtract(factor.multiply(a[col][k]));
                }
            }
        }
        return det;
    }

    // обратная матрица методом Гаусса-Жордана
    public ComplexMatrix inverse() {
        if (rows != cols) {
            throw new IllegalArgumentException("Обратная матрица есть только у квадратной матрицы");
        }
        int n = rows;

        // a - копия исходной матрицы, inv - единичная матрица, которая превратится в обратную
        Complex[][] a = new Complex[n][n];
        Complex[][] inv = new Complex[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = data[i][j];
                if (i == j) {
                    inv[i][j] = new Complex(1, 0);
                } else {
                    inv[i][j] = new Complex(0, 0);
                }
            }
        }

        for (int col = 0; col < n; col++) {
            // выбираем главный элемент
            int pivotRow = col;
            for (int r = col + 1; r < n; r++) {
                if (a[r][col].abs() > a[pivotRow][col].abs()) {
                    pivotRow = r;
                }
            }
            if (a[pivotRow][col].isZero()) {
                throw new ArithmeticException("Матрица вырожденная (определитель равен 0), обратной нет");
            }

            // меняем строки в обеих матрицах
            Complex[] tmp = a[pivotRow];
            a[pivotRow] = a[col];
            a[col] = tmp;
            tmp = inv[pivotRow];
            inv[pivotRow] = inv[col];
            inv[col] = tmp;

            // делим строку на главный элемент, чтобы на диагонали стала единица
            Complex pivot = a[col][col];
            for (int k = 0; k < n; k++) {
                a[col][k] = a[col][k].divide(pivot);
                inv[col][k] = inv[col][k].divide(pivot);
            }

            // вычитаем эту строку из всех остальных, чтобы в столбце остались нули
            for (int r = 0; r < n; r++) {
                if (r != col) {
                    Complex factor = a[r][col];
                    for (int k = 0; k < n; k++) {
                        a[r][k] = a[r][k].subtract(factor.multiply(a[col][k]));
                        inv[r][k] = inv[r][k].subtract(factor.multiply(inv[col][k]));
                    }
                }
            }
        }

        ComplexMatrix result = new ComplexMatrix(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result.data[i][j] = inv[i][j];
            }
        }
        return result;
    }

    // деление матриц: A / B = A * B^(-1)
    public ComplexMatrix divide(ComplexMatrix other) {
        if (other.rows != other.cols) {
            throw new IllegalArgumentException("Делить можно только на квадратную матрицу");
        }
        if (cols != other.rows) {
            throw new IllegalArgumentException(
                    "Для деления число столбцов первой матрицы должно быть равно размеру второй");
        }
        ComplexMatrix inverseOther = other.inverse();
        return multiply(inverseOther);
    }

    // красивый вывод: все элементы выравниваются по ширине
    public String toString() {
        String[][] text = new String[rows][cols];
        int maxLen = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                text[i][j] = data[i][j].toString();
                if (text[i][j].length() > maxLen) {
                    maxLen = text[i][j].length();
                }
            }
        }

        String result = "";
        for (int i = 0; i < rows; i++) {
            result = result + "| ";
            for (int j = 0; j < cols; j++) {
                String s = text[i][j];
                while (s.length() < maxLen) {
                    s = " " + s;
                }
                result = result + s;
                if (j < cols - 1) {
                    result = result + "   ";
                }
            }
            result = result + " |";
            if (i < rows - 1) {
                result = result + "\n";
            }
        }
        return result;
    }
}
