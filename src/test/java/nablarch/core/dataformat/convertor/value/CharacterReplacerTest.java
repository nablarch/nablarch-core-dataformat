package nablarch.core.dataformat.convertor.value;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.hamcrest.text.IsEmptyString;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.CharacterReplacementConfig;
import nablarch.core.dataformat.CharacterReplacementManager;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SyntaxErrorException;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link CharacterReplacer}のテスト
 */
public class CharacterReplacerTest {

    private CharacterReplacer sut = new CharacterReplacer();


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        ThreadContext.clear();
        SystemRepository.clear();

        final CharacterReplacementConfig zenkaku = new CharacterReplacementConfig();
        zenkaku.setTypeName("type_zenkaku");
        zenkaku.setFilePath("classpath:nablarch/core/dataformat/type_zenkaku.properties");
        zenkaku.setEncoding("ms932");
        zenkaku.setByteLengthCheck(true);

        final CharacterReplacementConfig hankaku = new CharacterReplacementConfig();
        hankaku.setTypeName("type_hankaku");
        hankaku.setFilePath("classpath:nablarch/core/dataformat/type_hankaku.properties");
        hankaku.setEncoding("ms932");
        hankaku.setByteLengthCheck(true);

        final CharacterReplacementManager characterReplacementManager = new CharacterReplacementManager();
        characterReplacementManager.setConfigList(new ArrayList<CharacterReplacementConfig>() {{
            add(zenkaku);
            add(hankaku);
        }});

        characterReplacementManager.initialize();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final HashMap<String, Object> result = new HashMap<String, Object>();
                result.put("characterReplacementManager", characterReplacementManager);
                return result;
            }
        });
    }

    @Test
    public void writeNotNull() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_hankaku");

        assertThat(sut.convertOnWrite("ABC"), is("ABC"));
        assertThat(sut.convertOnWrite("A\\C"), is("A[C"));
    }

    @Test
    public void writeEmptyString() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_hankaku");

        assertThat(sut.convertOnWrite(""), is(isEmptyString()));
    }

    @Test
    public void writeNull() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_hankaku");

        assertThat(sut.convertOnWrite(null), is(nullValue()));
    }

    @Test
    public void readNotNull() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_hankaku");

        assertThat(sut.convertOnRead("abc"), is("abc"));
        assertThat(sut.convertOnRead("a\\c"), is("a[c"));
    }

    @Test
    public void readEmptyString() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_hankaku");

        assertThat(sut.convertOnRead(""), isEmptyString());
    }

    @Test
    public void readNull() throws Exception {
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_hankaku");

        assertThat(sut.convertOnRead(null), is(nullValue()));
    }

    @Test
    public void invalidDataTypeOfWrite() throws Exception {
        expectedException.expect(InvalidDataFormatException.class);
        expectedException.expectMessage("invalid parameter type was specified. parameter type must be 'java.lang.String'. type=[class java.math.BigDecimal].");

        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));
        sut.initialize(fieldDefinition, "type_zenkaku");
        sut.convertOnWrite(BigDecimal.ONE);
    }

    @Test
    public void invalidTypeName() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("replacement type name was not found."
                + " value=[invalid_type]. must specify defined replacement type name. convertor=[CharacterReplacer].");

        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));

        sut.initialize(fieldDefinition, "invalid_type");
    }

    @Test
    public void replaceEncodingNotEqualsFileTypeEncoding() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("field encoding 'UTF-8' was invalid."
                + " field encoding must match the encoding that is defined as replacement type 'type_zenkaku'.");
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("utf-8"));

        sut.initialize(fieldDefinition, "type_zenkaku");
    }

    @Test
    public void notSpecifyOptions() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("parameter size was invalid."
                + " parameter size must be one, but was '0'. parameter=[], convertor=[CharacterReplacer].");
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));

        sut.initialize(fieldDefinition);
    }

    @Test
    public void typeNameIsNull() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("1st parameter was null. parameter=[null], convertor=[CharacterReplacer].");
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));

        sut.initialize(fieldDefinition, new Object[] {null});
    }

    @Test
    public void typeNameIsNotString() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("invalid parameter type was specified."
                + " parameter type must be 'String', but was 'java.lang.Integer'. parameter=[1], convertor=[CharacterReplacer].");
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));

        sut.initialize(fieldDefinition, 1);
    }

    @Test
    public void typeNameIsEmpty() throws Exception {
        expectedException.expect(SyntaxErrorException.class);
        expectedException.expectMessage("parameter was empty. parameter must not be empty. parameter=[], convertor=[CharacterReplacer].");
        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));

        sut.initialize(fieldDefinition, "");
    }

    @Test
    public void specifyNullToOptions() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("args must not be null");

        final FieldDefinition fieldDefinition = new FieldDefinition()
                .setName("str2")
                .setEncoding(Charset.forName("ms932"));

        sut.initialize(fieldDefinition, (Object[]) null);
    }
}
