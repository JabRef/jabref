package org.jabref.gui.util;

import javafx.geometry.NodeOrientation;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * A TextFlow that renders Markdown text with support for headings, paragraphs, emphasis, strong emphasis, code blocks, bullet lists, and ordered lists.
 */
public class MarkdownTextFlow extends SelectableTextFlow {
    private final Parser parser;
    private boolean needsLineBreak = false;
    private int currentOrderedListIndex = 0;

    /**
     * Creates a MarkdownTextFlow instance.
     *
     * @param parent The parent Pane to which this TextFlow will be added.
     */
    public MarkdownTextFlow(Pane parent) {
        super(parent);
        this.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        this.getStyleClass().add("markdown-textflow");

        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
    }

    /**
     * Sets the Markdown text to be rendered in this TextFlow.
     *
     * @param markdownText The Markdown text to render. If null or empty, the TextFlow will be cleared.
     */
    public void setMarkdown(String markdownText) {
        this.clear();
        needsLineBreak = false;

        if (markdownText == null || markdownText.trim().isEmpty()) {
            return;
        }

        try {
            Node document = parser.parse(markdownText);
            MarkdownRenderer renderer = new MarkdownRenderer();
            renderer.visit(document);
        } catch (Exception e) {
            Text plainText = new Text(markdownText);
            this.getChildren().add(plainText);
        }
    }

    private void addLineBreak() {
        if (needsLineBreak && !this.getChildren().isEmpty()) {
            Text lineBreak = new Text("\n");
            this.getChildren().add(lineBreak);
        }
        needsLineBreak = false;
    }

    private class MarkdownRenderer {
        private final NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(Heading.class, this::visit),
                new VisitHandler<>(Paragraph.class, this::visit),
                new VisitHandler<>(com.vladsch.flexmark.ast.Text.class, this::visit),
                new VisitHandler<>(Emphasis.class, this::visit),
                new VisitHandler<>(StrongEmphasis.class, this::visit),
                new VisitHandler<>(Code.class, this::visit),
                new VisitHandler<>(BulletList.class, this::visit),
                new VisitHandler<>(OrderedList.class, this::visit),
                new VisitHandler<>(BulletListItem.class, this::visit),
                new VisitHandler<>(OrderedListItem.class, this::visit)
        );

        public void visit(Node node) {
            visitor.visit(node);
        }

        private void visit(Heading heading) {
            addLineBreak();

            String content = heading.getText().toString();
            int level = heading.getLevel();

            Text headingNode = new Text(content);
            headingNode.getStyleClass().add("markdown-h" + level);

            getChildren().add(headingNode);
            needsLineBreak = true;
        }

        private void visit(Paragraph paragraph) {
            addLineBreak();

            Node parent = paragraph.getParent();
            boolean isInList = parent instanceof BulletListItem || parent instanceof OrderedListItem;

            if (!isInList && !getChildren().isEmpty()) {
                addLineBreak();
            }

            visitor.visitChildren(paragraph);
            needsLineBreak = true;
        }

        private void visit(com.vladsch.flexmark.ast.Text text) {
            String content = text.getChars().toString();
            Text textNode = new Text(content);
            getChildren().add(textNode);
        }

        private void visit(Emphasis emphasis) {
            String content = emphasis.getText().toString();
            Text italicText = new Text(content);
            italicText.getStyleClass().add("markdown-italic");
            getChildren().add(italicText);
        }

        private void visit(StrongEmphasis strongEmphasis) {
            String content = strongEmphasis.getText().toString();
            Text boldText = new Text(content);
            boldText.getStyleClass().add("markdown-bold");
            getChildren().add(boldText);
        }

        private void visit(Code code) {
            String content = code.getText().toString();
            Text codeText = new Text(content);
            codeText.getStyleClass().add("markdown-code");
            getChildren().add(codeText);
        }

        private void visit(BulletList bulletList) {
            addLineBreak();
            visitor.visitChildren(bulletList);
            needsLineBreak = true;
        }

        private void visit(OrderedList orderedList) {
            addLineBreak();
            currentOrderedListIndex = 0;
            visitor.visitChildren(orderedList);
            needsLineBreak = true;
        }

        private void visit(BulletListItem listItem) {
            addLineBreak();

            Text bulletText = new Text("• ");
            bulletText.getStyleClass().add("markdown-list-bullet");
            getChildren().add(bulletText);

            visitor.visitChildren(listItem);
        }

        private void visit(OrderedListItem listItem) {
            addLineBreak();
            currentOrderedListIndex++;

            Text numberText = new Text(currentOrderedListIndex + ". ");
            numberText.getStyleClass().add("markdown-list-number");
            getChildren().add(numberText);

            visitor.visitChildren(listItem);
        }
    }
}
