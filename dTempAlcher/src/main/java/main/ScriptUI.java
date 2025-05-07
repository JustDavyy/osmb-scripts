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
        VBox root = new VBox();
        root.setSpacing(10);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(StandardSpellbook.LOW_ALCHEMY, StandardSpellbook.HIGH_LEVEL_ALCHEMY);

        String savedSpell = prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.HIGH_LEVEL_ALCHEMY.name());
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(savedSpell));
        script.log("SAVESETTINGS", "Loaded saved spell: " + savedSpell);

        Label modeLabel = new Label("Selection mode");
        selectionModeComboBox = new ComboBox<>();
        selectionModeComboBox.getItems().addAll("Single Slot", "Multiple Slots");

        String savedMode = prefs.get(PREF_SELECTION_MODE, "Single Slot");
        selectionModeComboBox.getSelectionModel().select(savedMode);
        script.log("SAVESETTINGS", "Loaded selection mode: " + savedMode);

        itemSelectionBox = new VBox();
        itemSelectionBox.setSpacing(8);

        updateSlotSelectionUI();

        selectionModeComboBox.setOnAction(e -> updateSlotSelectionUI());

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        root.getChildren().addAll(spellLabel, spellComboBox, modeLabel, selectionModeComboBox, itemSelectionBox, confirmButton);

        Scene scene = new Scene(root, 200, 400);
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
}
