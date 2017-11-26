package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jpf
 * @created 11/21/17
 */
public class TextUtilTest {

    @Test
    public void testCreateText() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.NORMAL);
        Assert.assertEquals("Regular", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextBold() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.BOLD);
        Assert.assertEquals("Bold", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextItalic() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.ITALIC);
        Assert.assertEquals("Italic", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextMonospaced() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.MONOSPACED);
        Assert.assertEquals("Monospaced", text.getFont().getFamily());
        Assert.assertEquals("Regular", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testTextToHTMLStringBold() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.BOLD);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<b>" + testText + "</b>", htmlString);
    }

    @Test
    public void testTextToHTMLStringItalic() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.ITALIC);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<i>" + testText + "</i>", htmlString);
    }

    @Test
    public void testTextToHTMLStringMonospaced() {
        String testText = "this is a test text";
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.MONOSPACED);
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
        Text text = TextUtil.createText(testText, 12, TextUtil.TextType.NORMAL);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("this<br>is a<br>test text", htmlString);
    }

    private boolean checkIfTextsEqualsExpectedTexts(List<Text> texts, List<Text> expectedTexts) {
        System.out.println(texts);
        System.out.println(expectedTexts);
        if (expectedTexts.size() != texts.size())
            return false;
        Text expectedText;
        for (int i = 0; i < expectedTexts.size(); i++) {
            expectedText = expectedTexts.get(i);
            // the strings contain not only the text but also the font and other properties
            // so comparing them compares the Text object as a whole
            // the equals method is not implemented...
            if (!expectedText.toString().equals(texts.get(i).toString()))
                return false;
        }
        return true;
    }

    @Test
    public void testFormatToTextsNoReplacements() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("This search contains entries in which any field contains the regular expression ", 13));
        String test = "This search contains entries in which any field contains the regular expression ";
        List<Text> textList = TextUtil.formatToTexts(test, 13);
        Assert.assertTrue(checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }


    @Test
    public void testFormatToTextsEnd() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("This search contains entries in which any field contains the regular expression ", 13));
        expectedTextList.add(TextUtil.createText("replacing text", 13, TextUtil.TextType.BOLD));
        String test = "This search contains entries in which any field contains the regular expression <b>%0</b>";
        List<Text> textList = TextUtil.formatToTexts(test, 13, new TextUtil.TextReplacement("<b>%0</b>", "replacing text", TextUtil.TextType.BOLD));
        Assert.assertTrue(checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void testFormatToTextsBegin() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("replacing text", 13, TextUtil.TextType.BOLD));
        expectedTextList.add(TextUtil.createText(" This search contains entries in which any field contains the regular expression", 13));
        String test = "<b>%0</b> This search contains entries in which any field contains the regular expression";
        List<Text> textList = TextUtil.formatToTexts(test, 13, new TextUtil.TextReplacement("<b>%0</b>", "replacing text", TextUtil.TextType.BOLD));
        Assert.assertTrue(checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }

    @Test
    public void testFormatToTextsMiddle() {
        List<Text> expectedTextList = new ArrayList<>();
        expectedTextList.add(TextUtil.createText("This search contains entries ", 13));
        expectedTextList.add(TextUtil.createText("replacing text", 13, TextUtil.TextType.BOLD));
        expectedTextList.add(TextUtil.createText(" in which any field contains the regular expression", 13));
        String test = "This search contains entries <b>%0</b> in which any field contains the regular expression";
        List<Text> textList = TextUtil.formatToTexts(test, 13, new TextUtil.TextReplacement("<b>%0</b>", "replacing text", TextUtil.TextType.BOLD));
        Assert.assertTrue(checkIfTextsEqualsExpectedTexts(expectedTextList, textList));
    }
}
