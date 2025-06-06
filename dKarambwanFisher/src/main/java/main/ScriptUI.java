package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    private static final String PREF_BANKING_OPTION = "dkarambwanfisher_banking_option";
    private static final String PREF_FAIRY_RING_OPTION = "dkarambwanfisher_fairy_ring_option";
    private static final String PREF_WEBHOOK_ENABLED = "dkarambwanfisher_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dkarambwanfisher_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dkarambwanfisher_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dkarambwanfisher_webhook_include_user";
    private static final String PREF_WEBHOOK_INCLUDE_STATS = "dkarambwanfisher_webhook_include_stats";

    private final Script script;
    private ComboBox<String> bankingOptionsComboBox;
    private ComboBox<String> fairyRingOptionsComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;
    private CheckBox includeStatsCheckBox;

    private Scene scene;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label bankingLabel = new Label("Banking options");
        bankingOptionsComboBox = new ComboBox<>();
        bankingOptionsComboBox.setItems(FXCollections.observableArrayList("Zanaris", "Crafting Guild"));
        String savedBanking = prefs.get(PREF_BANKING_OPTION, "Zanaris");
        bankingOptionsComboBox.getSelectionModel().select(savedBanking);
        script.log(getClass().getSimpleName(), "Loaded banking option: " + savedBanking);

        Label fairyLabel = new Label("Fairy ring options");
        fairyRingOptionsComboBox = new ComboBox<>();
        fairyRingOptionsComboBox.setItems(FXCollections.observableArrayList("Zanaris", "Ardougne cloak", "Quest cape"));
        String savedFairy = prefs.get(PREF_FAIRY_RING_OPTION, "Zanaris");
        fairyRingOptionsComboBox.getSelectionModel().select(savedFairy);
        script.log(getClass().getSimpleName(), "Loaded fairy ring option: " + savedFairy);

        mainBox.getChildren().addAll(bankingLabel, bankingOptionsComboBox, fairyLabel, fairyRingOptionsComboBox);
        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhooks Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField();
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setText(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 15; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username in Webhook");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));
        includeUsernameCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeStatsCheckBox = new CheckBox("Include Script Stats in Webhook");
        includeStatsCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_STATS, true));
        includeStatsCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
            includeStatsCheckBox.setDisable(!enabled);
        });

        webhookBox.getChildren().addAll(
                webhookEnabledCheckBox,
                webhookUrlField,
                new Label("Send interval (minutes)"),
                webhookIntervalComboBox,
                includeUsernameCheckBox,
                includeStatsCheckBox
        );
        Tab webhookTab = new Tab("Webhooks", webhookBox);
        webhookTab.setClosable(false);

        // Confirm button (bottom)
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        scene = new Scene(layout, 300, 330);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void saveSettings() {
        prefs.put(PREF_BANKING_OPTION, getSelectedBankingOption());
        prefs.put(PREF_FAIRY_RING_OPTION, getSelectedFairyRingOption());
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_STATS, isStatsIncluded());

        script.log(getClass().getSimpleName(), "Saved banking option: " + getSelectedBankingOption());
        script.log(getClass().getSimpleName(), "Saved fairy ring option: " + getSelectedFairyRingOption());
        script.log(getClass().getSimpleName(), "Saved webhook enabled: " + isWebhookEnabled());
        script.log(getClass().getSimpleName(), "Saved webhook URL: " + getWebhookUrl());
        script.log(getClass().getSimpleName(), "Saved webhook interval: " + getWebhookInterval());
        script.log(getClass().getSimpleName(), "Saved webhook include username: " + isUsernameIncluded());
        script.log(getClass().getSimpleName(), "Saved webhook include stats: " + isStatsIncluded());

        ((Stage) bankingOptionsComboBox.getScene().getWindow()).close();
    }

    public String getSelectedBankingOption() {
        return bankingOptionsComboBox.getSelectionModel().getSelectedItem();
    }

    public String getSelectedFairyRingOption() {
        return fairyRingOptionsComboBox.getSelectionModel().getSelectedItem();
    }

    public boolean isWebhookEnabled() {
        return webhookEnabledCheckBox != null && webhookEnabledCheckBox.isSelected();
    }

    public String getWebhookUrl() {
        return webhookUrlField != null ? webhookUrlField.getText().trim() : "";
    }

    public int getWebhookInterval() {
        return webhookIntervalComboBox != null && webhookIntervalComboBox.getValue() != null
                ? webhookIntervalComboBox.getValue()
                : 5;
    }

    public boolean isUsernameIncluded() {
        return includeUsernameCheckBox != null && includeUsernameCheckBox.isSelected();
    }

    public boolean isStatsIncluded() {
        return includeStatsCheckBox != null && includeStatsCheckBox.isSelected();
    }
}