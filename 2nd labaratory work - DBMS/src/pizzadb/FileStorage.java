package pizzadb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Работа с файлами на диске (по сути DAO).
 * Каждая таблица лежит в файле имя.tbl, каждый индекс - в файле имя.idx.
 * Файлы пишутся через временный файл .tmp, чтобы при сбое
 * не потерять старую версию данных.
 */
public class FileStorage {

    private File folder;

    public FileStorage(String folderName) {
        folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // остатки недописанных файлов после аварийного завершения
        File[] files = folder.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith(".tmp")) {
                    files[i].delete();
                }
            }
        }
    }

    public void saveTable(Table table) {
        writeObject(new File(folder, table.getName().toLowerCase() + ".tbl"), table);
    }

    public void saveIndex(Index index) {
        writeObject(new File(folder, index.getName() + ".idx"), index);
    }

    public void deleteTable(Table table) {
        new File(folder, table.getName().toLowerCase() + ".tbl").delete();
        for (int i = 0; i < table.getIndexes().size(); i++) {
            deleteIndex(table.getIndexes().get(i));
        }
    }

    public void deleteIndex(Index index) {
        new File(folder, index.getName() + ".idx").delete();
    }

    public List<Table> loadTables() {
        List<Table> result = new ArrayList<Table>();
        File[] files = folder.listFiles();
        if (files == null) {
            return result;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".tbl")) {
                Object obj = readObject(files[i]);
                if (obj != null) {
                    result.add((Table) obj);
                }
            }
        }
        return result;
    }

    public List<Index> loadIndexes() {
        List<Index> result = new ArrayList<Index>();
        File[] files = folder.listFiles();
        if (files == null) {
            return result;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".idx")) {
                Object obj = readObject(files[i]);
                if (obj != null) {
                    result.add((Index) obj);
                }
            }
        }
        return result;
    }

    private void writeObject(File file, Object obj) {
        File temp = new File(file.getPath() + ".tmp");
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(temp));
            try {
                out.writeObject(obj);
            } finally {
                out.close();
            }
            // старый файл заменяется новым только когда новый записан полностью
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DbException("Не удалось записать файл " + file.getName() + ": " + e.getMessage());
        }
    }

    private Object readObject(File file) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            try {
                return in.readObject();
            } finally {
                in.close();
            }
        } catch (Exception e) {
            System.out.println("Не удалось прочитать файл " + file.getName() + ", он пропущен");
            return null;
        }
    }
}
