package net.sf.jabref.gui.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.model.entry.BibEntry;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;

public class WordCloudAction extends AbstractAction {

    private final JabRefFrame frame;
    private final String field;

    private final LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();

    public WordCloudAction(JabRefFrame frame, String field) {
        super("Word cloud for " + field);
        this.frame = frame;
        this.field = field;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
        frequencyAnalyzer.setWordFrequenciesToReturn(500);
        frequencyAnalyzer.setMinWordLength(3);
        List<String> words = new ArrayList<>();

        for (BibEntry entry : frame.getCurrentBasePanel().getDatabase().getEntries()) {
            entry.getFieldOptional(field).map(formatter::format).ifPresent(words::add);
        }
        final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(words);
        Dimension dimension = new Dimension(1000, 1000);
        WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);

        wordCloud.setPadding(0);
        wordCloud.setBackground(new RectangleBackground(dimension));
        wordCloud.setFontScalar(new LinearFontScalar(20, 40));
        wordCloud.build(wordFrequencies);
        final JLabel wordCloudLabel = new JLabel(new ImageIcon(wordCloud.getBufferedImage()));
        JDialog dialog = new JDialog(frame, "Word cloud for " + field);
        dialog.add(wordCloudLabel);
        dialog.pack();
        dialog.setVisible(true);
    }

}
