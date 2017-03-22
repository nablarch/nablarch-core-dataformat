package nablarch.core.dataformat;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 *
 */
public class TestSupport {

    public static void createFile(String file, String... lines) throws Exception {
        final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
        for (final String line : lines) {
            writer.append(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }
}
