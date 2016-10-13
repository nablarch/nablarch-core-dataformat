package nablarch.core.dataformat;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author T.Kawasaki
 */
public final class DataFormatTestUtils {


    private static final String ENCODING = "Windows-31J";

    public static ByteArrayInputStream createInputStreamFrom(String source) {
        return createInputStreamFrom(source, ENCODING);
    }
    
    public static ByteArrayInputStream createInputStreamFrom(String source, String encoding) {
        try {
            return new ByteArrayInputStream(source.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
