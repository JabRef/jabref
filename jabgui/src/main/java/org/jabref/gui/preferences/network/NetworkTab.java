package org.jabref.gui.preferences.network;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.forms.PasswordFieldEditor;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.dlsc.gemsfx.EnhancedPasswordField;
import com.tobiasdiez.easybind.EasyBind;

import static org.jabref.gui.preferences.forms.FormMetrics.GAP;

public class NetworkTab extends AbstractPreferenceTabView<NetworkTabViewModel> {

    // Kept as fields so validation can be attached after the grids are assembled.
    private TextField proxyHostname;
    private TextField proxyPort;
    private TextField proxyUsername;
    private EnhancedPasswordField proxyPassword;

    public NetworkTab() {
        this.viewModel = new NetworkTabViewModel(
                dialogService,
                preferences.getProxyPreferences(),
                preferences.getInternalPreferences(),
                preferences.getSSLPreferences(),
                preferences.getFilePreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Network");
    }

    private void buildView() {
        Label hint = new Label(Localization.lang("If you encounter an issue or a bug, please check the latest version, whether the issue is still present."));
        hint.setWrapText(true);

        setContent(form()
                .checkbox(Localization.lang("Check for updates on startup"), viewModel.versionCheckProperty())
                .custom(hint)

                .section(Localization.lang("Proxy configuration"), proxy -> proxy
                        .custom(buildProxyGrid(), grid -> grid
                                .validate(viewModel.proxyHostnameValidationStatus(), proxyHostname)
                                .validate(viewModel.proxyPortValidationStatus(), proxyPort)
                                .validate(viewModel.proxyUsernameValidationStatus(), proxyUsername)
                                .validate(viewModel.proxyPasswordValidationStatus(), proxyPassword)))

                .section(Localization.lang("SSL configuration"), ssl -> ssl
                        .custom(buildSslGrid()))

                .build());
    }

    private GridPane buildProxyGrid() {
        CheckBox proxyUse = new CheckBox(Localization.lang("Use custom proxy configuration"));
        proxyUse.selectedProperty().bindBidirectional(viewModel.proxyUseProperty());
        BooleanBinding proxyOff = proxyUse.selectedProperty().not();

        Label hostnameLabel = disabledWhen(new Label(Localization.lang("Hostname")), proxyOff);
        proxyHostname = new TextField();
        proxyHostname.setPrefWidth(200.0);
        proxyHostname.textProperty().bindBidirectional(viewModel.proxyHostnameProperty());
        proxyHostname.disableProperty().bind(proxyOff);

        Label portLabel = disabledWhen(new Label(Localization.lang("Port")), proxyOff);
        proxyPort = new TextField();
        proxyPort.setMaxWidth(100.0);
        proxyPort.textProperty().bindBidirectional(viewModel.proxyPortProperty());
        proxyPort.disableProperty().bind(proxyOff);

        CheckBox proxyUseAuthentication = new CheckBox(Localization.lang("Proxy requires authentication"));
        proxyUseAuthentication.selectedProperty().bindBidirectional(viewModel.proxyUseAuthenticationProperty());
        proxyUseAuthentication.disableProperty().bind(proxyOff);

        BooleanBinding proxyAuthOn = proxyUse.selectedProperty().and(proxyUseAuthentication.selectedProperty());
        BooleanBinding proxyAuthOff = proxyAuthOn.not();

        Label usernameLabel = disabledWhen(new Label(Localization.lang("Username")), proxyAuthOff);
        proxyUsername = new TextField();
        proxyUsername.setPrefWidth(200.0);
        proxyUsername.textProperty().bindBidirectional(viewModel.proxyUsernameProperty());
        proxyUsername.disableProperty().bind(proxyAuthOff);

        Label passwordLabel = disabledWhen(new Label(Localization.lang("Password")), proxyAuthOff);
        proxyPassword = PasswordFieldEditor.create(viewModel.proxyPasswordProperty()).withRevealButton().withClearButton().field();
        proxyPassword.setPrefWidth(200.0);
        proxyPassword.disableProperty().bind(proxyAuthOff);

        CheckBox proxyPersistPassword = new CheckBox(Localization.lang("Persist password between sessions"));
        proxyPersistPassword.selectedProperty().bindBidirectional(viewModel.proxyPersistPasswordProperty());
        proxyPersistPassword.disableProperty().bind(proxyAuthOn.and(viewModel.passwordPersistAvailable()).not());
        SplitPane persistWrapper = credentialTooltipWrapper(proxyPersistPassword);

        Button checkConnection = new Button(Localization.lang("Check connection"));
        checkConnection.setPrefWidth(200.0);
        checkConnection.setOnAction(_ -> viewModel.checkConnection());

        GridPane grid = threeColumnGrid();
        addSpanning(grid, proxyUse, 0, 3);
        grid.add(hostnameLabel, 0, 1);
        grid.add(proxyHostname, 1, 1);
        grid.add(portLabel, 0, 2);
        grid.add(proxyPort, 1, 2);
        addSpanning(grid, proxyUseAuthentication, 3, 3);
        grid.add(usernameLabel, 0, 4);
        grid.add(proxyUsername, 1, 4);
        grid.add(passwordLabel, 0, 5);
        grid.add(proxyPassword, 1, 5);
        grid.add(persistWrapper, 2, 5);
        grid.add(checkConnection, 1, 6);
        return grid;
    }

    private GridPane buildSslGrid() {
        TableView<CustomCertificateViewModel> table = new TableView<>();
        table.setPrefHeight(200.0);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().add(column(Localization.lang("Serial number"), data -> data.getValue().serialNumberProperty()));
        table.getColumns().add(column(Localization.lang("Issuer"), data -> data.getValue().issuerProperty()));
        table.getColumns().add(column(Localization.lang("Valid from"), data -> EasyBind.map(data.getValue().validFromProperty(), this::formatDate)));
        table.getColumns().add(column(Localization.lang("Valid to"), data -> EasyBind.map(data.getValue().validToProperty(), this::formatDate)));
        table.getColumns().add(column(Localization.lang("Signature algorithm"), data -> data.getValue().signatureAlgorithmProperty()));
        table.getColumns().add(column(Localization.lang("Version"), data -> EasyBind.map(data.getValue().versionProperty(), this::formatVersion)));

        TableColumn<CustomCertificateViewModel, String> actions = new TableColumn<>();
        actions.setMaxWidth(35.0);
        actions.setMinWidth(35.0);
        actions.setPrefWidth(35.0);
        actions.setResizable(false);
        actions.setReorderable(false);
        actions.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getThumbprint()));
        new ValueTableCellFactory<CustomCertificateViewModel, String>()
                .withGraphic(name -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove formatter '%0'", name))
                .withOnMouseClickedEvent(thumbprint -> _ -> viewModel.customCertificateListProperty().removeIf(cert -> cert.getThumbprint().equals(thumbprint)))
                .install(actions);
        table.getColumns().add(actions);

        table.itemsProperty().set(viewModel.customCertificateListProperty());

        Button addCertificate = new Button(Localization.lang("Browse for certificate..."));
        addCertificate.setGraphic(IconTheme.JabRefIcons.ADD_NOBOX.getGraphicNode());
        addCertificate.getStyleClass().add("add-certificate");
        addCertificate.setOnAction(_ -> viewModel.addCertificateFile());
        GridPane.setHalignment(addCertificate, HPos.RIGHT);
        GridPane.setValignment(addCertificate, VPos.TOP);

        GridPane grid = threeColumnGrid();
        addSpanning(grid, table, 0, 3);
        grid.add(addCertificate, 2, 1);
        return grid;
    }

    private TableColumn<CustomCertificateViewModel, String> column(
            String title,
            javafx.util.Callback<TableColumn.CellDataFeatures<CustomCertificateViewModel, String>, javafx.beans.value.ObservableValue<String>> valueFactory) {
        TableColumn<CustomCertificateViewModel, String> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        return column;
    }

    private SplitPane credentialTooltipWrapper(Node content) {
        // A disabled node swallows mouse events and so never shows its tooltip; the wrapper carries it instead.
        // It is a SplitPane because setTooltip needs a Control, which Pane and StackPane are not.
        SplitPane wrapper = new SplitPane(content);
        EasyBind.subscribe(viewModel.passwordPersistAvailable(), available ->
                wrapper.setTooltip(available ? null : new Tooltip(Localization.lang("Credential store not available."))));
        return wrapper;
    }

    private static Label disabledWhen(Label label, BooleanBinding condition) {
        label.disableProperty().bind(condition);
        return label;
    }

    private static void addSpanning(GridPane grid, Node node, int row, int columnSpan) {
        grid.add(node, 0, row);
        GridPane.setColumnSpan(node, columnSpan);
    }

    private static GridPane threeColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setHgrow(Priority.SOMETIMES);
            grid.getColumnConstraints().add(constraints);
        }
        return grid;
    }

    private String formatDate(LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
    }

    private String formatVersion(String version) {
        return "V%s".formatted(version);
    }
}
