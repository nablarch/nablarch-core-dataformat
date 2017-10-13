package nablarch.test;

import nablarch.core.util.annotation.Published;
import org.hamcrest.BaseMatcher;

/**
 * 改行コードの種類を無視するorg.hamcrest.Matcher実装クラス。<br/>
 * <pre><code>
 * assertThat("foo\r\n", IgnoringLS.equals("foo\r")); //--> success
 * assertThat("foo\r\n", IgnoringLS.equals("foo"));   //--> fail
 * </code></pre>
 * @author T.Kawasaki
 */
@Published
public final class IgnoringLS {

    /** プライベートコンストラクタ */
    private IgnoringLS() {
    }

    /**
     * 改行の種類以外が等しいことを表明するMatcherを返却する。
     *
     * @param expected 期待する値
     * @return org.hamcrest.Matcher
     */
    public static BaseMatcher<String> equals(String expected) {
        return (expected == null)
                ? new NullMatcher<String>()
                : new EqualsIgnoreLS(expected);
    }

    /**
     * 改行コードの種類を無視して、実際の値が期待値を含むことを
     * 表明するMatcherを返却する。
     *
     * @param expected 期待する値
     * @return org.hamcrest.Matcher
     */
    public static BaseMatcher<String> contains(String expected) {
        return (expected == null)
                ? new NullMatcher<String>()
                : new ContainsIgnoreLS(expected);
    }

    /**
     * 改行の種類以外が等しいことを表明するMatcherクラス。<br/>
     *
     * @author T.Kawasaki
     */
    public static final class EqualsIgnoreLS extends AbstractStringMatcher {

        /**
         * コンストラクタ。
         *
         * @param expected 期待値
         */
        private EqualsIgnoreLS(String expected) {
            super(toLf(expected));
        }

        /** {@inheritDoc} */
        @Override
        protected boolean doMatches(String actual) {
            return getExpected().equals(toLf(actual));
        }
    }


    /**
     * 改行コードの種類を無視して、実際の値が期待値を含むことを
     * 表明するMatcherクラス。<br/>
     *
     * @author T.Kawasaki
     */
    public static final class ContainsIgnoreLS extends AbstractStringMatcher {

        /**
         * コンストラクタ。
         *
         * @param expected 期待値
         */
        private ContainsIgnoreLS(String expected) {
            super(toLf(expected));
        }

        /** {@inheritDoc} */
        @Override
        protected boolean doMatches(String actual) {
            return toLf(actual).contains(getExpected());
        }
    }

    /**
     * 改行コードをLFに変換する。
     *
     * @param orig 元の文字列
     * @return 改行コードがLFに変換された文字列
     */
    private static String toLf(String orig) {
        return orig.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }
}
