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
    private static final Map<String, Function<Stage, Walkthrough>> WALKTHROUGH_REGISTRY = Map.of(
            "mainFileDirectory", WalkthroughAction::createMainFileDirectoryWalkthrough,
            "customizeEntryTable", WalkthroughAction::createEntryTableWalkthrough
    );
    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new HashMap<>();

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

    private static Walkthrough createEntryTableWalkthrough(Stage stage) {
        WalkthroughBuilder builder = new WalkthroughBuilder(stage);

        WalkthroughStep step1 = builder.createFileMenuStep();
        WalkthroughStep step2 = builder.createPreferencesStep();
        WalkthroughStep step3 = builder.createTabSelectionStep(
                "Entry table",
                Localization.lang("This section allows you to customize the columns displayed in the entry table when viewing your bibliography.")
        );

        WalkthroughStep step4 = PanelStep
                .builder(Localization.lang("Customize your entry table columns"))
                .content(
                        new TextBlock(Localization.lang("Here you can customize which columns appear in your entry table. You can add, remove, or reorder columns such as citation key, title, author, year, and journal. This helps you see the most relevant information for your research at a glance.")),
                        new InfoBlock(Localization.lang("The columns you configure here will be displayed whenever you open a library in JabRef. You can always return to this settings page to modify your column preferences."))
                )
                .continueButton(Localization.lang("Next"))
                .backButton(Localization.lang("Back"))
                .resolver(NodeResolver.fxId("columnsList"))
                .navigation(NavigationPredicate.manual())
                .position(PanelPosition.RIGHT)
                .highlight(builder.getPreferenceHighlight())
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        WalkthroughStep step5 = builder.createSaveStep(
                Localization.lang("Great! Your entry table columns are now configured. These settings will be applied to all your libraries in JabRef."),
                Localization.lang("You can find more information about customizing JabRef at https://docs.jabref.org/"),
                150
        );

        return new Walkthrough(step1, step2, step3, step4, step5);
    }

    private static Walkthrough createMainFileDirectoryWalkthrough(Stage stage) {
        WalkthroughBuilder builder = new WalkthroughBuilder(stage);

        WalkthroughStep step1 = builder.createFileMenuStep();
        WalkthroughStep step2 = builder.createPreferencesStep();
        WalkthroughStep step3 = builder.createTabSelectionStep(
                "Linked files",
                Localization.lang("This section manages how JabRef handles your PDF files and other documents.")
        );

        WalkthroughStep step4 = TooltipStep
                .builder(Localization.lang("Enable \"Main file directory\" option"))
                .content(new TextBlock(Localization.lang("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step.")))
                .width(400)
                .resolver(NodeResolver.fxId("useMainFileDirectory"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(builder.getPreferenceHighlight())
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        WalkthroughStep step5 = builder.createSaveStep(
                Localization.lang("Congratulations. Your main file directory is now configured. JabRef will use this location to automatically find and organize your research documents."),
                Localization.lang("Additional information on main file directory can be found in https://docs.jabref.org/v5/finding-sorting-and-cleaning-entries/filelinks"),
                180
        );

        return new Walkthrough(step1, step2, step3, step4, step5);
    }

    private static class WalkthroughBuilder {
        private final WindowResolver mainResolver;
        private final MultiWindowHighlight preferenceHighlight;

        public WalkthroughBuilder(Stage stage) {
            this.mainResolver = () -> Optional.of(stage);
            this.preferenceHighlight = new MultiWindowHighlight(
                    new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                    new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
            );
        }

        public WalkthroughStep createFileMenuStep() {
            return TooltipStep
                    .builder(Localization.lang("Click on \"File\" menu"))
                    .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                    .navigation(NavigationPredicate.onClick())
                    .position(TooltipPosition.BOTTOM)
                    .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                    .build();
        }

        public WalkthroughStep createPreferencesStep() {
            return TooltipStep
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
        }

        public WalkthroughStep createTabSelectionStep(String tabName, String description) {
            return TooltipStep
                    .builder(Localization.lang("Select the \"" + tabName + "\" tab"))
                    .content(new TextBlock(description))
                    .width(400)
                    .resolver(NodeResolver.predicate(node ->
                            node.getStyleClass().contains("list-cell") &&
                                    node.toString().contains(Localization.lang(tabName))))
                    .navigation(NavigationPredicate.onClick())
                    .position(TooltipPosition.AUTO)
                    .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                    .highlight(preferenceHighlight)
                    .build();
        }

        public WalkthroughStep createSaveStep(String completionMessage, String infoMessage, int height) {
            return PanelStep
                    .builder(Localization.lang("Click \"Save\" to save changes"))
                    .content(
                            new TextBlock(completionMessage),
                            new InfoBlock(infoMessage)
                    )
                    .height(height)
                    .resolver(NodeResolver.predicate(node ->
                            node.getStyleClass().contains("button") &&
                                    node.toString().contains(Localization.lang("Save"))))
                    .navigation(NavigationPredicate.onClick())
                    .position(PanelPosition.TOP)
                    .highlight(preferenceHighlight)
                    .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                    .build();
        }

        public MultiWindowHighlight getPreferenceHighlight() {
            return preferenceHighlight;
        }
    }
}
