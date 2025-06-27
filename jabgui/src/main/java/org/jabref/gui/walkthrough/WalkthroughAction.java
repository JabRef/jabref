package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javafx.scene.control.ContextMenu;
import javafx.stage.Stage;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.PreferencesDialogView;
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

import com.airhacks.afterburner.injection.Injector;

public class WalkthroughAction extends SimpleCommand {
    private static final Map<String, Function<Stage, Walkthrough>> WALKTHROUGH_REGISTRY = Map.of("mainFileDirectory", WalkthroughAction::createMainFileDirectoryWalkthrough);
    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new HashMap<>(); // must be mutable to allow caching of created walkthroughs

    private final Walkthrough walkthrough;
    private final Stage mainStage;

    public WalkthroughAction(String name) {
        this.mainStage = Injector.instantiateModelOrService(Stage.class);
        if (WALKTHROUGH_CACHE.containsKey(name)) {
            this.walkthrough = WALKTHROUGH_CACHE.get(name);
        } else {
            Function<Stage, Walkthrough> walkthroughProvider = WALKTHROUGH_REGISTRY.get(name);
            Objects.requireNonNull(walkthroughProvider, "Walkthrough not found: " + name);
            this.walkthrough = walkthroughProvider.apply(mainStage);
            WALKTHROUGH_CACHE.put(name, this.walkthrough);
        }
    }

    @Override
    public void execute() {
        walkthrough.start(this.mainStage);
    }

    private static Walkthrough createMainFileDirectoryWalkthrough(Stage mainStage) {
        WindowResolver mainResolver = () -> Optional.of(mainStage);

        WalkthroughStep step1 = TooltipStep
                .builder(Localization.lang("Click on \"File\" menu"))
                .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.BOTTOM)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughStep step2 = TooltipStep
                .builder(Localization.lang("Click on \"Preferences\""))
                .resolver(NodeResolver.menuItem("Preferences"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.RIGHT)
                .activeWindow(WindowResolver.clazz(ContextMenu.class))
                .highlight(new MultiWindowHighlight(
                        new WindowEffect(HighlightEffect.ANIMATED_PULSE),
                        new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .build();

        MultiWindowHighlight preferenceHighlight = new MultiWindowHighlight(
                new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );
        WalkthroughStep step3 = TooltipStep
                .builder(Localization.lang("Select the \"Linked files\" tab"))
                .content(new TextBlock(Localization.lang("This section manages how JabRef handles your PDF files and other documents.")))
                .width(400)
                .resolver(NodeResolver.predicate(node ->
                        node.getStyleClass().contains("list-cell") &&
                                node.toString().contains(Localization.lang("Linked files"))))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .highlight(preferenceHighlight)
                .build();

        WalkthroughStep step4 = TooltipStep
                .builder(Localization.lang("Enable \"Main file directory\" option"))
                .content(new TextBlock(Localization.lang("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step.")))
                .width(400)
                .resolver(NodeResolver.fxId("useMainFileDirectory"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(preferenceHighlight)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        WalkthroughStep step5 = PanelStep
                .builder(Localization.lang("Click \"Save\" to save changes"))
                .content(
                        new TextBlock(Localization.lang("Congratulations. Your main file directory is now configured. JabRef will use this location to automatically find and organize your research documents.")),
                        new InfoBlock(Localization.lang("Additional information on main file directory can be found in https://docs.jabref.org/v5/finding-sorting-and-cleaning-entries/filelinks"))
                )
                .height(180)
                .resolver(NodeResolver.predicate(node -> node.getStyleClass().contains("button") && node.toString().contains(Localization.lang("Save"))))
                .navigation(NavigationPredicate.onClick())
                .position(PanelPosition.TOP)
                .highlight(preferenceHighlight)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        return new Walkthrough(step1, step2, step3, step4, step5);
    }
}
