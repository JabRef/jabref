import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BstCaseChangerTest {

    @Test
    void changeCase_empty_noChange() {
        assertEquals("", BstCaseChanger.changeCase("", FormatMode.LOWER));
        assertEquals("", BstCaseChanger.changeCase("", FormatMode.UPPER));
        assertEquals("", BstCaseChanger.changeCase("", FormatMode.TITLE));
    }

    @Test
    void changeCase_titleCase_wordBoundaries() {
        assertEquals("Hello World", BstCaseChanger.changeCase("hello world", FormatMode.TITLE));
        assertEquals("Hello   World", BstCaseChanger.changeCase("hello   world", FormatMode.TITLE));
        assertEquals("Hello-World", BstCaseChanger.changeCase("hello-world", FormatMode.TITLE));
    }
}
