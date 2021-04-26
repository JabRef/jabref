package org.jabref.gui.entryeditor.fileannotationtab;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileAnnotationViewModelTest {

    private FileAnnotationViewModel annotationViewModel;

    @BeforeEach
    void setup() {
        String content = "This is content";
        String marking = String.format("This is paragraph 1.%n" +
                "This is paragr-%naph 2, and it crosses%nseveral lines,%nnow you can see next paragraph:%n"
                + "This is paragraph%n3.");

        FileAnnotation linkedFileAnnotation = new FileAnnotation("John", LocalDateTime.now(), 3, content, FileAnnotationType.FREETEXT, Optional.empty());
        FileAnnotation annotation = new FileAnnotation("Jaroslav Kucha ˇr", LocalDateTime.parse("2017-07-20T10:11:30"), 1, marking, FileAnnotationType.HIGHLIGHT, Optional.of(linkedFileAnnotation));

        annotationViewModel = new FileAnnotationViewModel(annotation);
    }

    @Test
    public void sameAuthor() {
        assertEquals("Jaroslav Kucha ˇr", annotationViewModel.getAuthor());
    }

    @Test
    public void retrieveCorrectPageNumberAsString() {
        assertEquals("1", annotationViewModel.getPage());
    }

    @Test
    public void retrieveCorrectDateAsString() {
        assertEquals("2017-07-20 10:11:30", annotationViewModel.getDate());
    }

    @Test
    public void retrieveCorrectContent() {
        assertEquals("This is content", annotationViewModel.getContent());
    }

    @Test
    public void removeOnlyLineBreaksNotPrecededByPeriodOrColon() {
        String expectedMarking = String.format("This is paragraph 1.%n" +
                "This is paragraph 2, and it crosses several lines, now you can see next paragraph:%n"
                + "This is paragraph 3.");

        assertEquals(expectedMarking, annotationViewModel.getMarking());
    }
}
