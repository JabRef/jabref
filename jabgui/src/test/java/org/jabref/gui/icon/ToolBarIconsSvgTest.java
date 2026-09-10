package org.jabref.gui.icon;

import javafx.scene.Node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolBarIconsSvgTest {

    @ParameterizedTest
    @EnumSource(value = IconTheme.JabRefIcons.class, names = {
            "NEW", "OPEN", "SAVE", "ADD_ENTRY_IMMEDIATE", "ADD_ENTRY",
            "DELETE_ENTRY", "LEFT", "RIGHT", "UNDO", "REDO",
            "CUT", "COPY", "PASTE", "MAKE_KEY", "CLEANUP_ENTRIES", "GITHUB"
    })
    void toolbarIconsReturnSvgNodeInstance(IconTheme.JabRefIcons iconConstant) {
        Node graphicNode = iconConstant.getGraphicNode();
        assertNotNull(graphicNode);
        assertInstanceOf(JabRefSvgIcon.class, graphicNode,
                () -> "Icon " + iconConstant.name() + " should render as JabRefSvgIcon");

        JabRefSvgIcon svgNode = (JabRefSvgIcon) graphicNode;
        assertNotNull(svgNode.getPath(), "SVG path must not be null");
        assertFalse(svgNode.getPath().isBlank(), "SVG path must not be blank");
    }

    @Test
    void matchesReturnsTrueForIdenticalSvgIcon() {
        Node node = IconTheme.JabRefIcons.SAVE.getGraphicNode();
        assertTrue(IconTheme.JabRefIcons.SAVE.matches(node));
        assertFalse(IconTheme.JabRefIcons.OPEN.matches(node));
    }
}
