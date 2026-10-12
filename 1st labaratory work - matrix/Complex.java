// Комплексное число вида re + im*i
public class Complex {
    private double re;
    private double im;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public double getRe() {
        return re;
    }

    public double getIm() {
        return im;
    }

    // сложение
    public Complex add(Complex other) {
        return new Complex(re + other.re, im + other.im);
    }

    // вычитание
    public Complex subtract(Complex other) {
        return new Complex(re - other.re, im - other.im);
    }

    // умножение: (a+bi)(c+di) = (ac-bd) + (ad+bc)i
    public Complex multiply(Complex other) {
        double newRe = re * other.re - im * other.im;
        double newIm = re * other.im + im * other.re;
        return new Complex(newRe, newIm);
    }

    // деление: (a+bi)/(c+di) = ((ac+bd) + (bc-ad)i) / (c^2+d^2)
    public Complex divide(Complex other) {
        double denom = other.re * other.re + other.im * other.im;
        if (denom == 0) {
            throw new ArithmeticException("Деление на комплексный ноль");
        }
        double newRe = (re * other.re + im * other.im) / denom;
        double newIm = (im * other.re - re * other.im) / denom;
        return new Complex(newRe, newIm);
    }

    // модуль числа
    public double abs() {
        return Math.sqrt(re * re + im * im);
    }

    // проверка на ноль с небольшой погрешностью
    public boolean isZero() {
        return abs() < 1e-9;
    }

    // округляем до 4 знаков, чтобы не выводить длинные хвосты
    private double round(double x) {
        return Math.round(x * 10000) / 10000.0;
    }

    public String toString() {
        double a = round(re);
        double b = round(im);
        if (b < 0) {
            return a + " - " + (-b) + "i";
        } else {
            return a + " + " + b + "i";
        }
    }
}
