package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_SPELL = "dtempalcher_selected_alch_spell";
    private static final String PREF_SELECTED_SLOT_ID = "dtempalcher_selected_slot_id";
    private static final String PREF_SELECTED_MULTIPLE_SLOTS = "dtempalcher_selected_multiple_slots";
    private static final String PREF_SELECTION_MODE = "dtempalcher_selection_mode";
    private static final String PREF_WEBHOOK_ENABLED = "dtempalcher_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dtempalcher_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dtempalcher_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dtempalcher_webhook_include_user";
    private static final String PREF_WEBHOOK_INCLUDE_STATS = "dtempalcher_webhook_include_stats";

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;
    private CheckBox includeStatsCheckBox;

    private final Script script;
    private ComboBox<StandardSpellbook> spellComboBox;
    private ComboBox<String> selectionModeComboBox;
    private TextField singleSlotField;
    private ListView<Integer> multipleSlotsView;
    private final ObservableList<Integer> multipleSelectedSlotIDs = FXCollections.observableArrayList();

    private VBox itemSelectionBox;
    private int selectedSlotID = 0;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(StandardSpellbook.LOW_ALCHEMY, StandardSpellbook.HIGH_LEVEL_ALCHEMY);
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.HIGH_LEVEL_ALCHEMY.name())));

        Label modeLabel = new Label("Selection mode");
        selectionModeComboBox = new ComboBox<>();
        selectionModeComboBox.getItems().addAll("Single Slot", "Multiple Slots");
        selectionModeComboBox.getSelectionModel().select(prefs.get(PREF_SELECTION_MODE, "Single Slot"));

        itemSelectionBox = new VBox(8);
        updateSlotSelectionUI();
        selectionModeComboBox.setOnAction(e -> updateSlotSelectionUI());

        mainBox.getChildren().addAll(spellLabel, spellComboBox, modeLabel, selectionModeComboBox, itemSelectionBox);
        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 15; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));
        includeUsernameCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeStatsCheckBox = new CheckBox("Include Stats");
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

        // === Final Layout ===
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 300, 420);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void updateSlotSelectionUI() {
        itemSelectionBox.getChildren().clear();
        if (isMultipleSelectionMode()) {
            setupMultipleSlotSelection();
        } else {
            setupSingleSlotSelection();
        }
    }

    private void setupSingleSlotSelection() {
        Label slotLabel = new Label("Slot to alch (0–27)");
        selectedSlotID = prefs.getInt(PREF_SELECTED_SLOT_ID, 0);

        singleSlotField = new TextField(String.valueOf(selectedSlotID));
        singleSlotField.setMaxWidth(100);

        itemSelectionBox.getChildren().addAll(slotLabel, singleSlotField);
    }

    private void setupMultipleSlotSelection() {
        Label slotsLabel = new Label("Slots to alch (0–27)");

        multipleSlotsView = new ListView<>(multipleSelectedSlotIDs);
        multipleSlotsView.setPrefHeight(200);

        String savedMultiple = prefs.get(PREF_SELECTED_MULTIPLE_SLOTS, "");
        if (!savedMultiple.isEmpty()) {
            try {
                List<Integer> slots = Arrays.stream(savedMultiple.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                multipleSelectedSlotIDs.setAll(slots);
                script.log("SAVESETTINGS", "Loaded multiple slots: " + savedMultiple);
            } catch (Exception e) {
                script.log("ERROR", "Failed to parse multiple slots: " + savedMultiple);
            }
        }

        Button addButton = new Button("Add Slot");
        addButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Enter slot ID (0–27)");
            dialog.showAndWait().ifPresent(input -> {
                try {
                    int slot = Integer.parseInt(input.trim());
                    if (slot >= 0 && slot <= 27 && !multipleSelectedSlotIDs.contains(slot)) {
                        multipleSelectedSlotIDs.add(slot);
                    }
                } catch (NumberFormatException ignored) {}
            });
        });

        Button removeButton = new Button("Remove Selected");
        removeButton.setOnAction(e -> {
            Integer selected = multipleSlotsView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                multipleSelectedSlotIDs.remove(selected);
            }
        });

        HBox buttonBox = new HBox(addButton, removeButton);
        buttonBox.setSpacing(8);

        itemSelectionBox.getChildren().addAll(slotsLabel, multipleSlotsView, buttonBox);
    }

    private void saveSettings() {
        if (spellComboBox.getSelectionModel().isEmpty()) return;

        prefs.put(PREF_SELECTED_SPELL, spellComboBox.getSelectionModel().getSelectedItem().name());
        prefs.put(PREF_SELECTION_MODE, selectionModeComboBox.getSelectionModel().getSelectedItem()); // <-- Save mode!

        if (isMultipleSelectionMode()) {
            String joinedSlots = multipleSelectedSlotIDs.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            prefs.put(PREF_SELECTED_MULTIPLE_SLOTS, joinedSlots);
            script.log("SAVESETTINGS", "Saved multiple slots: " + joinedSlots);
        } else {
            try {
                selectedSlotID = Integer.parseInt(singleSlotField.getText().trim());
            } catch (Exception e) {
                script.log("ERROR", "Invalid slot entered.");
                return;
            }
            if (selectedSlotID >= 0 && selectedSlotID <= 27) {
                prefs.putInt(PREF_SELECTED_SLOT_ID, selectedSlotID);
                script.log("SAVESETTINGS", "Saved single slot: " + selectedSlotID);
            }
        }

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_STATS, isStatsIncluded());

        Stage stage = (Stage) itemSelectionBox.getScene().getWindow();
        stage.close();
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedSlotId() {
        return selectedSlotID;
    }

    public List<Integer> getMultipleSelectedSlotIds() {
        return multipleSelectedSlotIDs;
    }

    public boolean isMultipleSelectionMode() {
        return selectionModeComboBox.getSelectionModel().getSelectedItem().equals("Multiple Slots");
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
