package nablarch.core.dataformat;

import nablarch.core.dataformat.LayoutFileParser.Token;
import nablarch.core.dataformat.LayoutFileParser.TokenType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * SyntaxErrorExceptionのgetterのテスト
 * @author Masato Inoue
 */
public class SyntaxErrorExceptionTest {
    
    /**
     * getterのテスト。
     */
    @Test
    public void testGetter() {
        SyntaxErrorException exception = new SyntaxErrorException("");
        Token token = new Token(TokenType.BINARY_LITERAL, null, 0, 0);
        exception.setToken(token);
        assertSame(token, exception.getToken());

        exception = new SyntaxErrorException("error", new NullPointerException());
        assertThat(exception.getMessage(), is("error"));

    }
    
}
