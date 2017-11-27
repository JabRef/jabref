package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.TextFlowEqualityHelper;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author jpf
 * @created 11/21/17
 */
public class TextUtilTest {

    @Test
    public void testCreateText() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.NORMAL);
        Assert.assertEquals("Regular", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextBold() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.BOLD);
        Assert.assertEquals("tooltip-text-bold", text.getStyleClass().toString());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextItalic() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.ITALIC);
        Assert.assertEquals("tooltip-text-italic", text.getStyleClass().toString());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextMonospaced() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.MONOSPACED);
        Assert.assertEquals("tooltip-text-monospaced", text.getStyleClass().toString());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testTextToHTMLStringBold() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.BOLD);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<b>" + testText + "</b>", htmlString);
    }

    @Test
    public void testTextToHTMLStringItalic() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.ITALIC);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<i>" + testText + "</i>", htmlString);
    }

    @Test
    public void testTextToHTMLStringMonospaced() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.MONOSPACED);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<kbd>" + testText + "</kbd>", htmlString);
    }

    @Test
    public void testTextToHTMLStringMonospacedBold() {
        String testText = "this is a test text";
        Text text = new Text(testText);
        text.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<b><kbd>" + testText + "</kbd></b>", htmlString);
    }

    @Test
    public void testTextToHTMLStringWithLinebreaks() {
        String testText = "this\nis a\ntest text";
        Text text = TextUtil.createText(testText, TextUtil.TextType.NORMAL);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("this<br>is a<br>test text", htmlString);
    }

    @Test
    public void testFormatToTextsNoReplacements() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("This search contains entries in which any field contains the regular expression "));
        String test = "This search contains entries in which any field contains the regular expression ";
        List<Text> textList = TextUtil.formatToTexts(test);
        Assert.assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }


    @Test
    public void testFormatToTextsEnd() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("This search contains entries in which any field contains the regular expression "));
        expectedTextList.add(TextUtil.createText("replacing text", TextUtil.TextType.BOLD));
        String test = "This search contains entries in which any field contains the regular expression <b>%0</b>";
        List<Text> textList = TextUtil.formatToTexts(test, new TextUtil.TextReplacement("<b>%0</b>", "replacing text", TextUtil.TextType.BOLD));
        Assert.assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void testFormatToTextsBegin() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("replacing text", TextUtil.TextType.BOLD));
        expectedTextList.add(TextUtil.createText(" This search contains entries in which any field contains the regular expression"));
        String test = "<b>%0</b> This search contains entries in which any field contains the regular expression";
        List<Text> textList = TextUtil.formatToTexts(test, new TextUtil.TextReplacement("<b>%0</b>", "replacing text", TextUtil.TextType.BOLD));
        Assert.assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void testFormatToTextsMiddle() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("This search contains entries "));
        expectedTextList.add(TextUtil.createText("replacing text", TextUtil.TextType.BOLD));
        expectedTextList.add(TextUtil.createText(" in which any field contains the regular expression"));
        String test = "This search contains entries <b>%0</b> in which any field contains the regular expression";
        List<Text> textList = TextUtil.formatToTexts(test, new TextUtil.TextReplacement("<b>%0</b>", "replacing text", TextUtil.TextType.BOLD));
        Assert.assertTrue(TextFlowEqualityHelper.checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }
}
