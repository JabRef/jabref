package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.stage.Window;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;
import org.jabref.logic.l10n.Localization;

public class WalkthroughAction extends SimpleCommand {
    private static final Map<String, Walkthrough> WALKTHROUGH_REGISTRY = buildRegistry();

    private final Walkthrough walkthrough;
    private final JabRefFrame frame;

    public WalkthroughAction(String name, JabRefFrame frame) {
        this.walkthrough = WALKTHROUGH_REGISTRY.get(name);
        this.frame = frame;
    }

    @Override
    public void execute() {
        walkthrough.start(frame.getMainStage());
    }

    private static Map<String, Walkthrough> buildRegistry() {
        Map<String, Walkthrough> registry = new HashMap<>();

        // FIXME: Not internationalized.
        WalkthroughNode step1 = TooltipStep
                .builder("Hover over \"File\" menu")
                .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.BOTTOM)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughNode step2 = TooltipStep
                .builder("Select \"Preferences\"")
                .resolver(NodeResolver.menuItem("Preferences"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.RIGHT)
                .highlight(MultiWindowHighlight.multiple(
                        new WindowEffect(Optional::empty, HighlightEffect.FULL_SCREEN_DARKEN),
                        new WindowEffect(
                                () -> Window.getWindows().stream()
                                            .filter(w -> w instanceof ContextMenu cm && cm.isShowing())
                                            .findFirst(),
                                HighlightEffect.ANIMATED_PULSE,
                                NodeResolver.menuItem("Preferences")
                        )
                ))
                .build();

        WalkthroughNode step3 = TooltipStep
                .builder("Select \"Linked files\" tab")
                .resolver(NodeResolver.predicate(node ->
                        node.getStyleClass().contains("list-cell") &&
                                node.toString().contains("Linked files")))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(MultiWindowHighlight.multiple(
                        new WindowEffect(Optional::empty, HighlightEffect.BACKDROP_HIGHLIGHT),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .autoFallback(false)
                .activeWindow(WindowResolver.title("JabRef preferences"))
                .build();

        WalkthroughNode step4 = TooltipStep
                .builder("Choose to use main file directory")
                .resolver(NodeResolver.fxId("useMainFileDirectory"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(MultiWindowHighlight.multiple(
                        new WindowEffect(Optional::empty, HighlightEffect.BACKDROP_HIGHLIGHT),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .activeWindow(WindowResolver.title("JabRef preferences"))
                .build();

        WalkthroughNode step5 = PanelStep
                .builder("Click \"OK\" to save changes")
                .resolver(NodeResolver.predicate(node -> node.getStyleClass().contains("button") && node.toString().contains(Localization.lang("Save"))))
                .navigation(NavigationPredicate.onClick())
                .position(Pos.TOP_CENTER)
                .highlight(MultiWindowHighlight.multiple(
                        new WindowEffect(Optional::empty, HighlightEffect.BACKDROP_HIGHLIGHT),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .activeWindow(WindowResolver.title("JabRef preferences"))
                .build();

        Walkthrough mainFileDirectory = new Walkthrough(List.of(step1, step2, step3, step4, step5));
        registry.put("mainFileDirectory", mainFileDirectory);

        Walkthrough editEntry = new Walkthrough(List.of());
        registry.put("editEntry", editEntry);
        return registry;
    }
}
