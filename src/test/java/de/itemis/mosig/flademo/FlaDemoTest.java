package de.itemis.mosig.flademo;

import static de.itemis.mosig.flademo.FlaDemo.strlen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlaDemoTest {
    private static final String TEST_STRING = "testString";
    private static final int EXPECTED_LENGTH = TEST_STRING.length();

    @Test
    public void returns_length_of_string() {
        assertThat(strlen(TEST_STRING)).isEqualTo(EXPECTED_LENGTH);
    }

    @Test
    public void main_works_with_regular_string() {
        Assertions.assertDoesNotThrow(() -> FlaDemo.main(new String[] {TEST_STRING}));
    }

    @Test
    public void main_works_with_empty_string() {
        Assertions.assertDoesNotThrow(() -> FlaDemo.main(new String[] {""}));
    }

    @Test
    public void main_does_not_accept_null() {
        assertThatThrownBy(() -> FlaDemo.main(null)).isInstanceOf(NullPointerException.class)
            .hasMessage("args");
    }

    @Test
    public void empty_arg_array_causes_illegal_argument() {
        assertThatThrownBy(() -> FlaDemo.main(new String[0])).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Please provide a test string as first argument.");
    }

    @Test
    public void first_argument_must_not_be_null() {
        assertThatThrownBy(() -> FlaDemo.main(new String[] {null})).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Please provide a test string as first argument.");
    }
}
