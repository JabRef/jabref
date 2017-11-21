package org.jabref.gui.util;

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
        Text text = TextUtil.createText(testText, 12);
        Assert.assertEquals("Regular", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextBold() {
        String testText = "this is a test text";
        Text text = TextUtil.createTextBold(testText, 12);
        Assert.assertEquals("Bold", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextItalic() {
        String testText = "this is a test text";
        Text text = TextUtil.createTextItalic(testText, 12);
        Assert.assertEquals("Italic", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testCreateTextMonospaced() {
        String testText = "this is a test text";
        Text text = TextUtil.createTextMonospaced(testText, 12);
        Assert.assertEquals("Monospaced", text.getFont().getFamily());
        Assert.assertEquals("Regular", text.getFont().getStyle());
        Assert.assertEquals(testText, text.getText());
    }

    @Test
    public void testTextToHTMLStringBold() {
        String testText = "this is a test text";
        Text text = TextUtil.createTextBold(testText, 12);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<b>" + testText + "</b>", htmlString);
    }

    @Test
    public void testTextToHTMLStringItalic() {
        String testText = "this is a test text";
        Text text = TextUtil.createTextItalic(testText, 12);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("<i>" + testText + "</i>", htmlString);
    }

    @Test
    public void testTextToHTMLStringMonospaced() {
        String testText = "this is a test text";
        Text text = TextUtil.createTextMonospaced(testText, 12);
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
        Text text = TextUtil.createText(testText, 12);
        String htmlString = TextUtil.textToHTMLString(text);
        Assert.assertEquals("this<br>is a<br>test text", htmlString);
    }
}
