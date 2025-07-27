package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.scene.control.ContextMenu;
import javafx.stage.Stage;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.sideeffect.OpenLibrarySideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelPosition;
import org.jabref.gui.walkthrough.declarative.step.QuitButtonPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;

public class WalkthroughAction extends SimpleCommand {
    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new HashMap<>(); // must be mutable to allow caching of created walkthroughs

    private final Walkthrough walkthrough;
    private final JabRefFrame frame;
    private final Stage stage;

    public WalkthroughAction(JabRefFrame frame, String name) {
        this.stage = Injector.instantiateModelOrService(Stage.class);
        this.frame = frame;

        if (WALKTHROUGH_CACHE.containsKey(name)) {
            this.walkthrough = WALKTHROUGH_CACHE.get(name);
        } else {
            this.walkthrough = getWalkthrough(name);
            WALKTHROUGH_CACHE.put(name, this.walkthrough);
        }
    }

    @Override
    public void execute() {
        walkthrough.start(this.stage);
    }

    private Walkthrough getWalkthrough(String name) {
        return WALKTHROUGH_CACHE.computeIfAbsent(name, _ ->
                switch (name) {
                    case "mainFileDirectory" -> createMainFileDirectoryWalkthrough();
                    default ->
                            throw new IllegalArgumentException("Unknown walkthrough: " + name);
                }
        );
    }

    private Walkthrough createMainFileDirectoryWalkthrough() {
        WindowResolver mainResolver = () -> Optional.of(stage);

        WalkthroughStep step0 = WalkthroughStep.sideEffect(Localization.lang("Open Example Library"))
                                               .sideEffect(new OpenLibrarySideEffect(frame))
                                               .build();

        WalkthroughStep step1 = WalkthroughStep
                .tooltip(Localization.lang("Welcome to the File Directory Setup"))
                .content(new TextBlock(Localization.lang("This walkthrough will guide you through setting up a main file directory. We've opened an example library so you can see how this feature works with actual bibliography entries.")))
                .resolver(NodeResolver.selector(".split-pane"))
                .continueButton(Localization.lang("Continue"))
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .position(TooltipPosition.BOTTOM)
                .build();

        WalkthroughStep step2 = WalkthroughStep
                .tooltip(Localization.lang("Click on \"File\" menu"))
                .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.BOTTOM)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughStep step3 = WalkthroughStep
                .tooltip(Localization.lang("Click on \"Preferences\""))
                .resolver(NodeResolver.menuItem("Preferences"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.RIGHT)
                .activeWindow(WindowResolver.clazz(ContextMenu.class))
                .highlight(new WalkthroughEffect(
                        new WindowEffect(HighlightEffect.ANIMATED_PULSE),
                        new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .showQuitButton(false)
                .build();

        WalkthroughEffect preferenceHighlight = new WalkthroughEffect(
                new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );

        WalkthroughStep step4 = WalkthroughStep
                .tooltip(Localization.lang("Select the \"Linked files\" tab"))
                .content(new TextBlock(Localization.lang("This section manages how JabRef handles your PDF files and other documents.")))
                .resolver(NodeResolver.predicate(node ->
                        node.getStyleClass().contains("list-cell") &&
                                node.toString().contains(Localization.lang("Linked files"))))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .highlight(preferenceHighlight)
                .build();

        WalkthroughStep step5 = WalkthroughStep
                .tooltip(Localization.lang("Enable \"Main file directory\" option"))
                .content(new TextBlock(Localization.lang("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step.")))
                .resolver(NodeResolver.fxId("useMainFileDirectory"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(preferenceHighlight)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        WalkthroughStep step6 = WalkthroughStep
                .panel(Localization.lang("Click \"Save\" to save changes"))
                .content(
                        new TextBlock(Localization.lang("Congratulations. Your main file directory is now configured. JabRef will use this location to automatically find and organize your research documents.")),
                        new InfoBlock(Localization.lang("Additional information on main file directory can be found in [help](https://docs.jabref.org/v5/finding-sorting-and-cleaning-entries/filelinks)"))
                )
                .resolver(NodeResolver.predicate(node -> node.getStyleClass().contains("button") && node.toString().contains(Localization.lang("Save"))))
                .navigation(NavigationPredicate.onClick())
                .position(PanelPosition.TOP)
                .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                .highlight(preferenceHighlight)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        return new Walkthrough(step0, step1, step2, step3, step4, step5, step6);
    }
}
