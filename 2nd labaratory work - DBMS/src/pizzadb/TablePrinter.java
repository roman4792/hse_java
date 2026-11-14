package pizzadb;

import java.util.List;

/**
 * Печатает результат запроса на экран в виде таблицы.
 */
public class TablePrinter {

    public static void print(QueryResult result) {
        if (result.hasTable()) {
            printTable(result.getColumnNames(), result.getRows());
        }
        System.out.println(result.getMessage());
    }

    private static void printTable(List<String> names, List<Object[]> rows) {
        int count = names.size();

        // ширина каждого столбца = самое длинное значение
        int[] widths = new int[count];
        for (int c = 0; c < count; c++) {
            widths[c] = names.get(c).length();
        }
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < count; c++) {
                int len = toText(rows.get(r)[c]).length();
                if (len > widths[c]) {
                    widths[c] = len;
                }
            }
        }

        String line = makeLine(widths);
        System.out.println(line);

        StringBuilder header = new StringBuilder("|");
        for (int c = 0; c < count; c++) {
            header.append(" ").append(pad(names.get(c), widths[c])).append(" |");
        }
        System.out.println(header);
        System.out.println(line);

        for (int r = 0; r < rows.size(); r++) {
            StringBuilder sb = new StringBuilder("|");
            for (int c = 0; c < count; c++) {
                sb.append(" ").append(pad(toText(rows.get(r)[c]), widths[c])).append(" |");
            }
            System.out.println(sb);
        }
        System.out.println(line);
    }

    private static String toText(Object value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString();
    }

    private static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String makeLine(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int c = 0; c < widths.length; c++) {
            for (int i = 0; i < widths[c] + 2; i++) {
                sb.append('-');
            }
            sb.append('+');
        }
        return sb.toString();
    }
}
