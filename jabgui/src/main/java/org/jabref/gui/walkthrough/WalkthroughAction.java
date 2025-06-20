package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.control.ContextMenu;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.step.PanelPosition;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
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

        WalkthroughStep step1 = TooltipStep
                .builder("Click on \"File\" menu")
                .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.BOTTOM)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughStep step2 = TooltipStep
                .builder("Click on \"Preferences\"")
                .resolver(NodeResolver.menuItem("Preferences"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.RIGHT)
                .activeWindow(WindowResolver.clazz(ContextMenu.class))
                .highlight(new MultiWindowHighlight(
                        new WindowEffect(HighlightEffect.ANIMATED_PULSE),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .build();

        WalkthroughStep step3 = TooltipStep
                .builder("Select the \"Linked files\" tab")
                .content(new TextBlock(Localization.lang("This section manages how JabRef handles your PDF files and other documents.")))
                .width(400)
                .resolver(NodeResolver.predicate(node ->
                        node.getStyleClass().contains("list-cell") &&
                                node.toString().contains("Linked files")))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .autoFallback(false)
                .activeWindow(WindowResolver.title("JabRef preferences"))
                .highlight(new MultiWindowHighlight(
                        new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .build();

        WalkthroughStep step4 = TooltipStep
                .builder("Enable \"Main file directory\" option")
                .content(new TextBlock("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step."))
                .width(400)
                .resolver(NodeResolver.fxId("useMainFileDirectory"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(new MultiWindowHighlight(
                        new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .activeWindow(WindowResolver.title("JabRef preferences"))
                .build();

        WalkthroughStep step5 = PanelStep
                .builder("Click \"OK\" to save changes")
                .content(
                        new TextBlock("Congratulations! Your main file directory is now configured. JabRef will use this location to automatically find and organize your research documents."),
                        new InfoBlock("Additional information on main file directory can be found in https://docs.jabref.org/v5/finding-sorting-and-cleaning-entries/filelinks")
                )
                .height(180)
                .resolver(NodeResolver.predicate(node -> node.getStyleClass().contains("button") && node.toString().contains(Localization.lang("Save"))))
                .navigation(NavigationPredicate.onClick())
                .position(PanelPosition.TOP)
                .highlight(new MultiWindowHighlight(
                        new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                        new WindowEffect(WindowResolver.title("JabRef"), HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .activeWindow(WindowResolver.title("JabRef preferences"))
                .build();

        Walkthrough mainFileDirectory = new Walkthrough(step1, step2, step3, step4, step5);
        registry.put("mainFileDirectory", mainFileDirectory);
        return registry;
    }
}
