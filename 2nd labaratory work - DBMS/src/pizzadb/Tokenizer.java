package pizzadb;

import java.util.ArrayList;
import java.util.List;

/**
 * Режет текст запроса на кусочки (токены): слова, числа, строки в кавычках и знаки.
 * Строки остаются в одинарных кавычках, чтобы их можно было отличить от слов.
 */
public class Tokenizer {

    public static List<String> split(String sql) {
        List<String> tokens = new ArrayList<String>();
        int i = 0;
        int length = sql.length();

        while (i < length) {
            char c = sql.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '\'') {
                // строка в кавычках; две кавычки подряд внутри строки означают одну кавычку
                StringBuilder sb = new StringBuilder();
                sb.append('\'');
                i++;
                boolean closed = false;
                while (i < length) {
                    char d = sql.charAt(i);
                    if (d == '\'') {
                        if (i + 1 < length && sql.charAt(i + 1) == '\'') {
                            sb.append('\'');
                            i += 2;
                        } else {
                            closed = true;
                            i++;
                            break;
                        }
                    } else {
                        sb.append(d);
                        i++;
                    }
                }
                if (!closed) {
                    throw new DbException("Не закрыта кавычка в строке");
                }
                sb.append('\'');
                tokens.add(sb.toString());
            } else if (c == '(' || c == ')' || c == ',' || c == ';' || c == '*' || c == '=') {
                tokens.add(String.valueOf(c));
                i++;
            } else if (c == '<' || c == '>' || c == '!') {
                if (i + 1 < length && sql.charAt(i + 1) == '=') {
                    tokens.add(sql.substring(i, i + 2));
                    i += 2;
                } else if (c == '<' && i + 1 < length && sql.charAt(i + 1) == '>') {
                    tokens.add("<>");
                    i += 2;
                } else {
                    tokens.add(String.valueOf(c));
                    i++;
                }
            } else if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                int start = i;
                while (i < length && (Character.isLetterOrDigit(sql.charAt(i))
                        || sql.charAt(i) == '_' || sql.charAt(i) == '-' || sql.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(sql.substring(start, i));
            } else {
                throw new DbException("Неизвестный символ в запросе: " + c);
            }
        }
        return tokens;
    }
}
