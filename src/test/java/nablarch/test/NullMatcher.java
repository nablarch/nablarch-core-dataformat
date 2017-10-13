package nablarch.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * NullにマッチするMatcherクラス。
 *
 * @param <T> 比較するオブジェクトの型
 */
public class NullMatcher<T> extends BaseMatcher<T> {

    /** {@inheritDoc} */
    public boolean matches(Object o) {
        return o == null;
    }

    /** {@inheritDoc} */
    public void describeTo(Description description) {
        description.appendValue(null);
    }
}
