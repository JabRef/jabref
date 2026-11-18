package org.jabref.gui.openoffice;

import java.util.EnumSet;
import java.util.Set;

import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@ExtendWith(JavaFxExtension.class)
@ResourceLock("Localization.lang")
class CiteSpecialDialogViewTest {

    private static final StyleCapabilities CSL_CAPABILITIES = new StyleCapabilities(
            Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT,
                    CitationType.AUTHOR_ONLY, CitationType.YEAR_ONLY), true, false, false);
    private static final StyleCapabilities JSTYLE_CAPABILITIES = new StyleCapabilities(EnumSet.allOf(CitationType.class), true, true, true);

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHORYEAR_PAR", "AUTHORYEAR_INTEXT", "AUTHOR_ONLY", "YEAR_ONLY"})
    void cslAllowsSupportedTypesWithoutPageInfo(CitationType citationType) {
        JavaFxExtension.invokeAndWait(() -> {
            CiteSpecialDialogView dialog = new CiteSpecialDialogView(CitationType.AUTHORYEAR_PAR, CSL_CAPABILITIES);
            RadioButton option = citationOption(dialog, citationType);

            assertFalse(option.isDisabled());
            option.fire();

            assertTrue(option.isSelected());
            assertEquals(citationType, dialog.getResultConverter().call(ButtonType.OK).citationTypeProperty().get());
            assertTrue(citationOption(dialog, CitationType.AUTHORYEAR_NOPAR).isDisabled());
            assertTrue(pageInfo(dialog).isDisabled());
        });
    }

    @Test
    void cslIgnoresUnsupportedPreferenceAndCannotSelectIt() {
        JavaFxExtension.invokeAndWait(() -> {
            CiteSpecialDialogView dialog = new CiteSpecialDialogView(CitationType.AUTHORYEAR_NOPAR, CSL_CAPABILITIES);
            RadioButton noPar = citationOption(dialog, CitationType.AUTHORYEAR_NOPAR);

            assertTrue(noPar.isDisabled());
            noPar.fire();

            assertTrue(citationOption(dialog, CitationType.AUTHORYEAR_PAR).isSelected());
            assertEquals(CitationType.AUTHORYEAR_PAR, dialog.getResultConverter().call(ButtonType.OK).citationTypeProperty().get());
        });
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHORYEAR_PAR", "AUTHORYEAR_INTEXT", "AUTHOR_ONLY", "YEAR_ONLY"})
    void cslRestoresSupportedPreference(CitationType preferredType) {
        JavaFxExtension.invokeAndWait(() -> {
            CiteSpecialDialogView dialog = new CiteSpecialDialogView(preferredType, CSL_CAPABILITIES);

            assertTrue(citationOption(dialog, preferredType).isSelected());
            assertEquals(preferredType, dialog.getResultConverter().call(ButtonType.OK).citationTypeProperty().get());
        });
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY"})
    void nativeCapabilitiesDisablePartialOptionsAndIgnoreTheirPreferences(CitationType preferredType) {
        JavaFxExtension.invokeAndWait(() -> {
            StyleCapabilities nativeCapabilities = new StyleCapabilities(
                    Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT), true, false, false);
            CiteSpecialDialogView dialog = new CiteSpecialDialogView(preferredType, nativeCapabilities);

            assertTrue(citationOption(dialog, CitationType.AUTHOR_ONLY).isDisabled());
            assertTrue(citationOption(dialog, CitationType.YEAR_ONLY).isDisabled());
            assertTrue(citationOption(dialog, CitationType.AUTHORYEAR_NOPAR).isDisabled());
            assertTrue(pageInfo(dialog).isDisabled());
            assertTrue(citationOption(dialog, CitationType.AUTHORYEAR_PAR).isSelected());
            assertEquals(CitationType.AUTHORYEAR_PAR, dialog.getResultConverter().call(ButtonType.OK).citationTypeProperty().get());
        });
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, mode = EnumSource.Mode.EXCLUDE, names = "INVISIBLE_CIT")
    void jStyleKeepsAllDialogOptionsAvailable(CitationType citationType) {
        JavaFxExtension.invokeAndWait(() -> {
            CiteSpecialDialogView dialog = new CiteSpecialDialogView(CitationType.AUTHORYEAR_PAR, JSTYLE_CAPABILITIES);
            RadioButton option = citationOption(dialog, citationType);

            assertFalse(option.isDisabled());
            option.fire();

            assertEquals(citationType, dialog.getResultConverter().call(ButtonType.OK).citationTypeProperty().get());
            assertEquals(citationType == CitationType.AUTHOR_ONLY, pageInfo(dialog).isDisabled());
        });
    }

    @Test
    void jStyleReenablesPageInfoWhenSwitchingFromAuthorToYear() {
        JavaFxExtension.invokeAndWait(() -> {
            CiteSpecialDialogView dialog = new CiteSpecialDialogView(CitationType.AUTHOR_ONLY, JSTYLE_CAPABILITIES);

            assertTrue(pageInfo(dialog).isDisabled());
            citationOption(dialog, CitationType.YEAR_ONLY).fire();
            assertFalse(pageInfo(dialog).isDisabled());
            pageInfo(dialog).setText("42");
            citationOption(dialog, CitationType.AUTHOR_ONLY).fire();
            assertTrue(pageInfo(dialog).isDisabled());
            citationOption(dialog, CitationType.YEAR_ONLY).fire();

            assertFalse(pageInfo(dialog).isDisabled());
            CiteSpecialDialogViewModel result = dialog.getResultConverter().call(ButtonType.OK);
            assertEquals(CitationType.YEAR_ONLY, result.citationTypeProperty().get());
            assertEquals("42", result.pageInfoProperty().get());
        });
    }

    private RadioButton citationOption(CiteSpecialDialogView dialog, CitationType citationType) {
        return JavaFxExtension.lookup(dialog.getDialogPane(), ".radio-button", RadioButton.class,
                button -> button.getUserData() == citationType);
    }

    private TextField pageInfo(CiteSpecialDialogView dialog) {
        return JavaFxExtension.lookup(dialog.getDialogPane(), "#pageInfo", TextField.class);
    }
}
