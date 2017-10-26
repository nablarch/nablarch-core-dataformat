package nablarch.test;

import nablarch.core.util.annotation.Published;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * 抽象文字列Matcherクラス。<br/>
 * 共通処理を実装する。
 */
@Published(tag = "architect")
abstract class AbstractStringMatcher extends BaseMatcher<String> {

    /** 期待値 */
    private final String expected;

    /**
     * コンストラクタ
     *
     * @param expected 期待値
     */
    AbstractStringMatcher(String expected) {
        this.expected = expected;
    }

    /**
     * 期待値を返却する。
     *
     * @return 期待値
     */
    protected String getExpected() {
        return expected;
    }

    /** {@inheritDoc} */
    public boolean matches(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof String)) {
            return false;
        }
        String actual = (String) o;
        return doMatches(actual);
    }

    /** {@inheritDoc} */
    public void describeTo(Description description) {
        description.appendValue(expected);
    }

    /**
     * マッチするか判定する。<br/>
     *
     * @param actual 実際の値
     * @return 判定結果
     */
    protected abstract boolean doMatches(String actual);
}
