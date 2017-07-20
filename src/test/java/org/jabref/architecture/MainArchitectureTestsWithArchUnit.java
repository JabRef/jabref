package org.jabref.architecture;

import org.jabref.JabRefMain;
import org.jabref.collab.FileUpdateMonitor;
import org.jabref.gui.DefaultInjector;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.groups.EntryTableTransferHandler;
import org.jabref.gui.groups.GroupTreeNodeViewModel;
import org.jabref.gui.importer.actions.AppendDatabaseAction;
import org.jabref.gui.keyboard.EmacsKeyBindings;
import org.jabref.gui.logging.GuiAppender;
import org.jabref.gui.openoffice.OpenOfficePanel;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import org.jabref.logic.bst.BibtexWidth;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.exporter.OpenDocumentSpreadsheetCreator;
import org.jabref.logic.exporter.OpenOfficeDocumentCreator;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.fetcher.BibsonomyScraper;
import org.jabref.logic.importer.util.JSONEntryParser;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.logging.JabRefLogger;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.client.RemoteListenerClient;
import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.io.XMLUtil;
import org.jabref.logic.xmp.XMPUtil;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.pdf.FileAnnotationType;
import org.jabref.shared.DBMSConnection;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.jabref")
public class MainArchitectureTestsWithArchUnit {

    @ArchTest
    public static final ArchRule doNotUseApacheCommonsLang3 =
            noClasses().that().areNotAnnotatedWith(ApacheCommonsLang3Allowed.class)
            .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3");

    @ArchTest
    public static final ArchRule doNotUseApacheCommonsLogging =
            noClasses().that()
                    // all these classes have static methods using a logger
                    .areNotAssignableTo(AppendDatabaseAction.class).and()
                    .areNotAssignableTo(BackupManager.class).and()
                    .areNotAssignableTo(BibtexKeyPatternUtil.class).and()
                    .areNotAssignableTo(BibtexWidth.class).and()
                    .areNotAssignableTo(BibsonomyScraper.class).and()
                    .areNotAssignableTo(CitationStyle.class).and()
                    .areNotAssignableTo(CitationStyleGenerator.class).and()
                    .areNotAssignableTo(ControlHelper.class).and()
                    .areNotAssignableTo(DBMSConnection.class).and()
                    .areNotAssignableTo(DefaultInjector.class).and()
                    .areNotAssignableTo(DefaultTaskExecutor.class).and()
                    .areNotAssignableTo(DuplicateCheck.class).and()
                    .areNotAssignableTo(EntryTableTransferHandler.class).and()
                    .areNotAssignableTo(EmacsKeyBindings.class).and()
                    .areNotAssignableTo(FieldEditors.class).and()
                    .areNotAssignableTo(FileAnnotationType.class).and()
                    .areNotAssignableTo(FileBasedLock.class).and()
                    .areNotAssignableTo(FileUpdateMonitor.class).and()
                    .areNotAssignableTo(FileUtil.class).and()
                    .areNotAssignableTo(GuiAppender.class).and()
                    .areNotAssignableTo(GUIGlobals.class).and()
                    .areNotAssignableTo(GroupTreeNodeViewModel.class).and()
                    .areNotAssignableTo(JabRefDesktop.class).and()
                    .areNotAssignableTo(JabRefLogger.class).and()
                    .areNotAssignableTo(JabRefMain.class).and()
                    .areNotAssignableTo(JournalAbbreviationLoader.class).and()
                    .areNotAssignableTo(JSONEntryParser.class).and()
                    .areNotAssignableTo(IconTheme.class).and()
                    .areNotAssignableTo(Localization.class).and()
                    .areNotAssignableTo(MetaDataParser.class).and()
                    .areNotAssignableTo(OpenDatabase.class).and()
                    .areNotAssignableTo(OpenDocumentSpreadsheetCreator.class).and()
                    .areNotAssignableTo(OpenOfficeDocumentCreator.class).and()
                    .areNotAssignableTo(OpenOfficePanel.class).and()
                    .areNotAssignableTo(PreferencesMigrations.class).and()
                    .areNotAssignableTo(ProtectedTermsLoader.class).and()
                    .areNotAssignableTo(RemoteListenerClient.class).and()
                    .areNotAssignableTo(SidePaneManager.class).and()
                    .areNotAssignableTo(URLDownload.class).and()
                    .areNotAssignableTo(org.jabref.logic.util.Version.class).and()
                    .areNotAssignableTo(org.jabref.logic.util.Version.DevelopmentStage.class).and()
                    .areNotAssignableTo(XMLUtil.class).and()
                    .areNotAssignableTo(XMPUtil.class)
            .should().accessClassesThat().resideInAPackage("org.apache.commons.logging");

}
