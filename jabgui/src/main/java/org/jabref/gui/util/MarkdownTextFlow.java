package org.jabref.gui.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringJoiner;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.preferences.GuiPreferences;

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
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.LinkRef;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MarkdownTextFlow extends SelectableTextFlow {
    private static final String BULLET_LIST_PATTERN = "^\\s*[-*•]\\s+$";
    private static final String NUMBERED_LIST_PATTERN = "^\\s*\\d+\\.\\s+$";
    private static final String UNICODE_BULLET = "\u2022";
    private static final String BLOCKQUOTE_MARKER = "> ";

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

    public void setMarkdown(@NonNull String markdownText) {
        super.clearSelection();
        getChildren().clear();

        if (markdownText.trim().isEmpty()) {
            return;
        }

        MarkdownRenderer renderer = new MarkdownRenderer();
        renderer.render(parser.parse(markdownText));
    }

    private void addTextNode(@Nullable String content, Node astNode, String... styleClasses) {
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

    private void addHyperlinkNode(String text, String url, Node astNode, String... styleClasses) {
        if (text == null || text.isEmpty()) {
            return;
        }

        MarkdownAwareHyperlink hyperlink = new MarkdownAwareHyperlink(text, astNode);
        hyperlink.setOnAction(_ -> new OpenBrowserAction(
                url,
                Injector.instantiateModelOrService(DialogService.class),
                Injector.instantiateModelOrService(GuiPreferences.class)
                        .getExternalApplicationsPreferences()).execute()
        );

        if (styleClasses != null) {
            for (String styleClass : styleClasses) {
                if (styleClass != null && !styleClass.isEmpty()) {
                    hyperlink.getStyleClass().add(styleClass);
                }
            }
        }
        hyperlink.getStyleClass().add("markdown-link");
        getChildren().add(hyperlink);
    }

    @Override
    public void copySelectedText() {
        if (!isSelectionActive()) {
            return;
        }

        int selStart = getSelectionStartIndex();
        int selEnd = getSelectionEndIndex();

        StringJoiner result = new StringJoiner("");
        int currentPos = 0;

        for (javafx.scene.Node fxNode : getChildren()) {
            String renderedText;
            String markdownText;
            Node astNode;

            if (fxNode instanceof MarkdownAwareText mat) {
                renderedText = mat.getText();
                astNode = mat.astNode;
                markdownText = getMarkdownRepresentation(astNode, renderedText);
            } else if (fxNode instanceof MarkdownAwareHyperlink mah) {
                renderedText = mah.getText();
                astNode = mah.astNode;
                markdownText = getMarkdownRepresentation(astNode, renderedText);
            } else {
                continue;
            }

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
                    if (astNode instanceof DelimitedNode delimitedNode) {
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
        } else if (astNode instanceof Link link) {
            String linkText = link.getText().toString();
            String url = link.getUrl().toString();
            String title = link.getTitle().toString();
            if (title.isEmpty()) {
                return "[" + linkText + "](" + url + ")";
            } else {
                return "[" + linkText + "](" + url + " \"" + title + "\")";
            }
        } else if (astNode instanceof LinkRef linkRef) {
            String linkText = linkRef.getText().toString();
            String reference = linkRef.getReference().toString();
            return "[" + linkText + "][" + reference + "]";
        } else if (astNode instanceof FencedCodeBlock fencedCodeBlock) {
            String info = fencedCodeBlock.getInfo().toString();
            String openingFence = fencedCodeBlock.getOpeningFence().toString();
            String closingFence = fencedCodeBlock.getClosingFence().toString();
            // NOTE: Hack. Flexmark always add \n at beginning, \n\n at end.
            String content = fencedCodeBlock.getContentChars().toString();
            return openingFence + info + content.substring(0, content.length() - 1) + closingFence;
        } else if (astNode instanceof IndentedCodeBlock) {
            return astNode.getChars().toString();
        } else if (astNode instanceof BlockQuote) {
            return renderedText;
        } else if (renderedText.matches(BULLET_LIST_PATTERN)) {
            return renderedText.replace(UNICODE_BULLET, "-");
        } else if (renderedText.matches(NUMBERED_LIST_PATTERN)) {
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
                    new VisitHandler<>(Link.class, this::visit),
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

        private void visit(Link link) {
            String text = link.getText().toString();
            String url = link.getUrl().toString();
            addHyperlinkNode(text, url, link);
        }

        private void visit(FencedCodeBlock codeBlock) {
            addNewlinesBetweenBlocks(codeBlock);
            String content = codeBlock.getContentChars().toString();
            /*
             * NOTE: Flexmark always append \n at the beginning and \n\n at the end.
             * For example, ```java
             * public class HelloWorld { ... }
             * ``` -> contains content `\npublic class HelloWorld { ... }\n\n`
             * Therefore, we need to remove the first and last characters.
             */
            String processedContent = content;
            if (content.length() >= 3 && content.startsWith("\n") && content.endsWith("\n\n")) {
                processedContent = content.substring(1, content.length() - 2);
            }
            addTextNode(processedContent, codeBlock, "markdown-code-block");
            previousBlock = codeBlock;
        }

        private void visit(IndentedCodeBlock codeBlock) {
            addNewlinesBetweenBlocks(codeBlock);
            String content = codeBlock.getContentChars().toString();
            // NOTE: Similar to FencedCodeBlock, Flexmark always appends \n at the beginning and \n\n at the end.
            String processedContent = content;
            if (content.length() >= 3 && content.startsWith("\n") && content.endsWith("\n\n")) {
                processedContent = content.substring(1, content.length() - 2);
            }
            addTextNode(processedContent, codeBlock, "markdown-code-block");
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
                    processQuoteParagraph(quote, child);
                } else {
                    addTextNode(BLOCKQUOTE_MARKER, quote, "markdown-blockquote-marker");
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

        private void processQuoteParagraph(BlockQuote quote, Node child) {
            String text = child.getChildChars().toString();
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    addTextNode("\n", quote);
                }
                addTextNode(BLOCKQUOTE_MARKER, quote, "markdown-blockquote-marker");
                addTextNode(lines[i], child, "markdown-blockquote");
            }
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

    private static class MarkdownAwareHyperlink extends Hyperlink {
        private final Node astNode;

        public MarkdownAwareHyperlink(String text, Node astNode) {
            super(text);
            this.astNode = astNode;
            setUserData(astNode);
        }
    }
}
