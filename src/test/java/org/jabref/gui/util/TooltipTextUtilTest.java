package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.text.Text;

import org.jabref.gui.search.TextFlowEqualityHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TooltipTextUtilTest {

    private String testText = "this is a test text";

    @Test
    public void retrieveCorrectTextStyleNormal() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.NORMAL);
        String textStyle = "Regular";

        assertTrue(text.getFont().getStyle().equals(textStyle));
    }

    @Test
    public void stringRemainsTheSameAfterTransformationToNormal() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.NORMAL);

        assertTrue(text.getText().equals(testText));
    }

    @Test
    public void retrieveCorrectTextStyleBold() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.BOLD);
        String textStyle = "tooltip-text-bold";

        assertTrue(text.getStyleClass().toString().equals(textStyle));
    }

    @Test
    public void stringRemainsTheSameAfterTransformationToBold() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.BOLD);

        assertTrue(text.getText().equals(testText));
    }

    @Test
    public void retrieveCorrectTextStyleItalic() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.ITALIC);
        String textStyle = "tooltip-text-italic";

        assertTrue(text.getStyleClass().toString().equals(textStyle));
    }

    @Test
    public void stringRemainsTheSameAfterTransformationToItalic() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.ITALIC);

        assertTrue(text.getText().equals(testText));
    }

    @Test
    public void testCreateTextMonospaced() {
        String testText = "this is a test text";
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.MONOSPACED);
        assertEquals("tooltip-text-monospaced", text.getStyleClass().toString());
        assertEquals(testText, text.getText());
    }

    @Test
    public void retrieveCorrectStyleMonospaced() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.MONOSPACED);
        String textStyle = "tooltip-text-monospaced";

        assertTrue(text.getStyleClass().toString().equals(textStyle));
    }

    @Test
    public void stringRemainsTheSameAfterTransformationToMonospaced() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.MONOSPACED);

        assertTrue(text.getText().equals(testText));
    }

    @Test
    public void transformTextToHTMLStringBold() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.BOLD);
        String htmlString = TooltipTextUtil.textToHTMLString(text);
        String expectedString = "<b>" + testText + "</b>";

        assertTrue(htmlString.equals(expectedString));
    }

    @Test
    public void transformTextToHTMLStringItalic() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.ITALIC);
        String htmlString = TooltipTextUtil.textToHTMLString(text);
        String expectedString = "<i>" + testText + "</i>";

        assertTrue(htmlString.equals(expectedString));
    }

    @Test
    public void transformTextToHTMLStringMonospaced() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.MONOSPACED);
        String htmlString = TooltipTextUtil.textToHTMLString(text);
        String expectedString = "<kbd>" + testText + "</kbd>";

        assertTrue(htmlString.equals(expectedString));
    }

    @Test
    public void transformTextToHTMLStringMonospacedBold() {
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.MONOSPACED);
        text.getStyleClass().add("tooltip-text-bold");
        String htmlString = TooltipTextUtil.textToHTMLString(text);
        String expectedString = "<b><kbd>" + testText + "</kbd></b>";

        assertTrue(htmlString.equals(expectedString));
    }

    @Test
    public void transformTextToHTMLStringWithLinebreaks() {
        String testText = "this\nis a\ntest text";
        Text text = TooltipTextUtil.createText(testText, TooltipTextUtil.TextType.NORMAL);
        String htmlString = TooltipTextUtil.textToHTMLString(text);
        String expectedString = "this<br>is a<br>test text";

        assertTrue(htmlString.equals(expectedString));
        // assertEquals("this<br>is a<br>test text", htmlString);
    }

    @Test
    public void formatToTextsNoReplacements() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TooltipTextUtil.createText("This search contains entries in which any field contains the regular expression "));
        String test = "This search contains entries in which any field contains the regular expression ";
        List<Text> textList = TooltipTextUtil.formatToTexts(test);

        assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void formatToTextsEnd() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TooltipTextUtil.createText("This search contains entries in which any field contains the regular expression "));
        expectedTextList.add(TooltipTextUtil.createText("replacing text", TooltipTextUtil.TextType.BOLD));
        String test = "This search contains entries in which any field contains the regular expression <b>%0</b>";
        List<Text> textList = TooltipTextUtil.formatToTexts(test, new TooltipTextUtil.TextReplacement("<b>%0</b>", "replacing text", TooltipTextUtil.TextType.BOLD));

        assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void formatToTextsBegin() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TooltipTextUtil.createText("replacing text", TooltipTextUtil.TextType.BOLD));
        expectedTextList.add(TooltipTextUtil.createText(" This search contains entries in which any field contains the regular expression"));
        String test = "<b>%0</b> This search contains entries in which any field contains the regular expression";
        List<Text> textList = TooltipTextUtil.formatToTexts(test, new TooltipTextUtil.TextReplacement("<b>%0</b>", "replacing text", TooltipTextUtil.TextType.BOLD));

        assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void formatToTextsMiddle() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TooltipTextUtil.createText("This search contains entries "));
        expectedTextList.add(TooltipTextUtil.createText("replacing text", TooltipTextUtil.TextType.BOLD));
        expectedTextList.add(TooltipTextUtil.createText(" in which any field contains the regular expression"));
        String test = "This search contains entries <b>%0</b> in which any field contains the regular expression";
        List<Text> textList = TooltipTextUtil.formatToTexts(test, new TooltipTextUtil.TextReplacement("<b>%0</b>", "replacing text", TooltipTextUtil.TextType.BOLD));

        assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }
}
