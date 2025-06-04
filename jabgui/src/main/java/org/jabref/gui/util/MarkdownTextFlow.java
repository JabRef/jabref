package org.jabref.gui.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.StringJoiner;

import javafx.geometry.NodeOrientation;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import org.jabref.gui.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownTextFlow extends SelectableTextFlow {
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public MarkdownTextFlow(Pane parent) {
        super(parent);
        this.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        this.getStyleClass().add("markdown-textflow");
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    public void setMarkdown(String markdownText) {
        Objects.requireNonNull(markdownText);
        super.clearSelection();
        getChildren().clear();

        if (markdownText.trim().isEmpty()) {
            return;
        }

        MarkdownRenderer renderer = new MarkdownRenderer();
        renderer.render(parser.parse(markdownText));
    }

    private void addTextNode(String content, Node astNode, String... styleClasses) {
        if (content == null || content.isEmpty()) {
            return;
        }

        MarkdownAwareText textNode = new MarkdownAwareText(content, astNode);
        if (styleClasses != null) {
            for (String styleClass : styleClasses) {
                if (styleClass != null && !styleClass.isEmpty()) {
                    textNode.getStyleClass().add(styleClass);
                }
            }
        }
        getChildren().add(textNode);
    }

    @Override
    public void copySelectedText() {
        if (startHit.isEmpty() || endHit.isEmpty()) {
            return;
        }

        int hitStart = startHit.get().getCharIndex();
        int hitEnd = endHit.get().getCharIndex();
        int selStart = Math.min(hitStart, hitEnd);
        int selEnd = Math.max(hitStart + 1, hitEnd + 1);

        StringJoiner result = new StringJoiner("");
        int currentPos = 0;

        for (javafx.scene.Node fxNode : getChildren()) {
            if (!(fxNode instanceof MarkdownAwareText mat)) {
                continue;
            }

            String renderedText = mat.getText();
            String markdownText = getMarkdownRepresentation(mat.astNode, renderedText);

            int segmentStart = currentPos;
            int segmentEnd = currentPos + renderedText.length();

            if (segmentEnd <= selStart || segmentStart >= selEnd) {
                currentPos = segmentEnd;
                continue;
            }

            int overlapStart = Math.max(segmentStart, selStart);
            int overlapEnd = Math.min(segmentEnd, selEnd);

            if (overlapStart < overlapEnd) {
                int startInSegment = overlapStart - segmentStart;
                int endInSegment = overlapEnd - segmentStart;

                if (startInSegment == 0 && endInSegment == renderedText.length()) {
                    result.add(markdownText);
                } else {
                    String partialText = renderedText.substring(startInSegment, endInSegment);
                    if (mat.astNode instanceof DelimitedNode delimitedNode) {
                        // e.g., select "llo" inside "*hello*" would return "*llo*"
                        partialText = delimitedNode.getOpeningMarker() + partialText + delimitedNode.getClosingMarker();
                    }
                    result.add(partialText);
                }
            }

            currentPos = segmentEnd;
        }

        if (result.length() == 0) {
            return;
        }

        ClipBoardManager clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        clipBoardManager.setHtmlContent(htmlRenderer.render(parser.parse(result.toString())), result.toString());
    }

    private String getMarkdownRepresentation(Node astNode, String renderedText) {
        if ("\n".equals(renderedText) || "\n\n".equals(renderedText)) {
            return renderedText;
        } else if (astNode instanceof Heading heading) {
            return "#".repeat(heading.getLevel()) + " " + heading.getText().toString().trim();
        } else if (astNode instanceof Code) {
            return "`" + astNode.getChildChars() + "`";
        } else if (astNode instanceof Emphasis) {
            return "*" + astNode.getChildChars() + "*";
        } else if (astNode instanceof StrongEmphasis) {
            return "**" + astNode.getChildChars() + "**";
        } else if (astNode instanceof FencedCodeBlock fenced) {
            String info = fenced.getInfo().toString();
            String openingFence = fenced.getOpeningFence().toString();
            String closingFence = fenced.getClosingFence().toString();
            // NOTE: Hack. Flexmark always add \n at beginning, \n\n at end.
            String content = fenced.getContentChars().toString();
            return openingFence + info + content.substring(0, content.length() - 1) + closingFence;
        } else if (astNode instanceof IndentedCodeBlock) {
            return astNode.getChars().toString();
        } else if (astNode instanceof BlockQuote) {
            return renderedText;
        } else if (renderedText.matches("^\\s*[-*•]\\s+$")) {
            return renderedText.replace("\u2022", "-");
        } else if (renderedText.matches("^\\s*\\d+\\.\\s+$")) {
            return renderedText;
        } else {
            return renderedText;
        }
    }

    private class MarkdownRenderer {
        private final NodeVisitor visitor;
        private final Deque<Integer> orderedListCounters = new ArrayDeque<>();
        private int listIndentationLevel = 0;
        private boolean isFirstBlockElement = true;
        private Node previousBlock = null;

        MarkdownRenderer() {
            visitor = new NodeVisitor(
                    new VisitHandler<>(Document.class, this::visit),
                    new VisitHandler<>(Heading.class, this::visit),
                    new VisitHandler<>(Paragraph.class, this::visit),
                    new VisitHandler<>(com.vladsch.flexmark.ast.Text.class, this::visit),
                    new VisitHandler<>(Emphasis.class, this::visit),
                    new VisitHandler<>(StrongEmphasis.class, this::visit),
                    new VisitHandler<>(Code.class, this::visit),
                    new VisitHandler<>(FencedCodeBlock.class, this::visit),
                    new VisitHandler<>(IndentedCodeBlock.class, this::visit),
                    new VisitHandler<>(BulletList.class, this::visit),
                    new VisitHandler<>(OrderedList.class, this::visit),
                    new VisitHandler<>(BulletListItem.class, this::visit),
                    new VisitHandler<>(OrderedListItem.class, this::visit),
                    new VisitHandler<>(BlockQuote.class, this::visit),
                    new VisitHandler<>(HtmlInline.class, this::visit),
                    new VisitHandler<>(HtmlBlock.class, this::visit)
            );
        }

        void render(Node node) {
            visitor.visit(node);
        }

        private void visit(Document document) {
            for (Node child : document.getChildren()) {
                visitor.visit(child);
            }
        }

        private void visit(Heading heading) {
            addNewlinesBetweenBlocks(heading);
            String text = heading.getText().toString();
            addTextNode(text, heading, "markdown-h" + heading.getLevel());
            previousBlock = heading;
        }

        private void visit(Paragraph paragraph) {
            boolean isInListItem = isInsideListItem(paragraph);

            if (!isInListItem) {
                addNewlinesBetweenBlocks(paragraph);
            } else if (paragraph.getPrevious() instanceof Paragraph) {
                addTextNode("\n", paragraph);
            }

            for (Node child : paragraph.getChildren()) {
                visitor.visit(child);
            }

            if (!isInListItem) {
                previousBlock = paragraph;
            }
        }

        private void visit(com.vladsch.flexmark.ast.Text text) {
            addTextNode(text.getChars().toString(), text);
        }

        private void visit(Emphasis emphasis) {
            addTextNode(emphasis.getText().toString(), emphasis, "markdown-italic");
        }

        private void visit(StrongEmphasis strong) {
            addTextNode(strong.getText().toString(), strong, "markdown-bold");
        }

        private void visit(Code code) {
            addTextNode(code.getText().toString(), code, "markdown-code");
        }

        private void visit(FencedCodeBlock codeBlock) {
            addNewlinesBetweenBlocks(codeBlock);
            String content = codeBlock.getContentChars().toString();
            addTextNode(content.substring(1, content.length() - 2), codeBlock, "markdown-code-block");
            previousBlock = codeBlock;
        }

        private void visit(IndentedCodeBlock codeBlock) {
            addNewlinesBetweenBlocks(codeBlock);
            String content = codeBlock.getContentChars().toString();
            addTextNode(content.substring(1, content.length() - 2), codeBlock, "markdown-code-block");
            previousBlock = codeBlock;
        }

        private void visit(BulletList list) {
            addNewlinesBetweenBlocks(list);
            listIndentationLevel++;

            for (Node child : list.getChildren()) {
                visitor.visit(child);
            }

            listIndentationLevel--;
            if (listIndentationLevel == 0) {
                previousBlock = list;
            }
        }

        private void visit(OrderedList list) {
            addNewlinesBetweenBlocks(list);
            orderedListCounters.push(list.getStartNumber() - 1);
            listIndentationLevel++;

            for (Node child : list.getChildren()) {
                visitor.visit(child);
            }

            listIndentationLevel--;
            orderedListCounters.pop();
            if (listIndentationLevel == 0) {
                previousBlock = list;
            }
        }

        private void visit(BulletListItem item) {
            if (item.getPrevious() != null) {
                addTextNode("\n", item);
            }

            String indent = "  ".repeat(Math.max(0, listIndentationLevel - 1));
            addTextNode(indent + "• ", item, "markdown-list-bullet");

            for (Node child : item.getChildren()) {
                visitor.visit(child);
            }
        }

        private void visit(OrderedListItem item) {
            if (item.getPrevious() != null) {
                addTextNode("\n", item);
            }

            int number = orderedListCounters.pop() + 1;
            orderedListCounters.push(number);

            String indent = "  ".repeat(Math.max(0, listIndentationLevel - 1));
            addTextNode(indent + number + ". ", item, "markdown-list-number");

            for (Node child : item.getChildren()) {
                visitor.visit(child);
            }
        }

        private void visit(BlockQuote quote) {
            addNewlinesBetweenBlocks(quote);

            for (Node child : quote.getChildren()) {
                if (child instanceof Paragraph) {
                    String text = child.getChildChars().toString();
                    String[] lines = text.split("\n", -1);
                    for (int i = 0; i < lines.length; i++) {
                        if (i > 0) {
                            addTextNode("\n", quote);
                        }
                        addTextNode("> ", quote, "markdown-blockquote-marker");
                        addTextNode(lines[i], child, "markdown-blockquote");
                    }
                } else {
                    addTextNode("> ", quote, "markdown-blockquote-marker");
                    visitor.visit(child);
                }
            }

            previousBlock = quote;
        }

        private void visit(HtmlInline html) {
            addTextNode(html.getChars().toString(), html, "markdown-code");
        }

        private void visit(HtmlBlock html) {
            addNewlinesBetweenBlocks(html);
            addTextNode(html.getChars().toString(), html, "markdown-code-block");
            previousBlock = html;
        }

        private boolean isInsideListItem(Node node) {
            Node parent = node.getParent();
            while (parent != null) {
                if (parent instanceof ListItem) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        }

        private void addNewlinesBetweenBlocks(Node currentBlock) {
            if (isFirstBlockElement) {
                isFirstBlockElement = false;
                return;
            }

            int newlineCount = 1;

            if (previousBlock instanceof Heading || currentBlock instanceof Heading) {
                newlineCount = 2;
            } else if (previousBlock instanceof Paragraph) {
                newlineCount = 2;
            } else if (currentBlock instanceof ListBlock && listIndentationLevel == 0) {
                newlineCount = 2;
            } else if (previousBlock instanceof FencedCodeBlock || previousBlock instanceof IndentedCodeBlock ||
                    currentBlock instanceof FencedCodeBlock || currentBlock instanceof IndentedCodeBlock) {
                newlineCount = 2;
            }

            addTextNode("\n".repeat(newlineCount), currentBlock);
        }
    }

    private static class MarkdownAwareText extends Text {
        private final Node astNode;

        public MarkdownAwareText(String text, Node astNode) {
            super(text);
            this.astNode = astNode;
            setUserData(astNode);
        }
    }
}
