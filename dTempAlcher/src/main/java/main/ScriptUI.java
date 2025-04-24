package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_SPELL = "selected_alch_spell";
    private static final String PREF_SELECTED_SLOT_ID = "selected_slot_id";

    private final Script script;
    private ComboBox<StandardSpellbook> spellComboBox;
    private TextField itemSlotField;
    private int itemSlotID;

    private static final StandardSpellbook[] ALCHEMY_SPELLS = {
            StandardSpellbook.LOW_ALCHEMY,
            StandardSpellbook.HIGH_LEVEL_ALCHEMY
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        // Spell selection
        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(ALCHEMY_SPELLS);

        String savedSpell = prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.HIGH_LEVEL_ALCHEMY.name());
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(savedSpell));
        script.log("SAVESETTINGS", "Loaded saved spell from preferences: " + savedSpell);

        // Load item ID
        itemSlotID = prefs.getInt(PREF_SELECTED_SLOT_ID, 0);
        script.log("SAVESETTINGS", "Loaded saved Slot ID from preferences: " + itemSlotID);

        // Item ID entry
        Label itemIdLabel = new Label("Slot to alch items of (0-27)");
        itemSlotField = new TextField("12"); // Default: Yew Longbows noted
        itemSlotField.setMaxWidth(100);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            if (spellComboBox.getSelectionModel().getSelectedIndex() >= 0) {
                prefs.put(PREF_SELECTED_SPELL, spellComboBox.getSelectionModel().getSelectedItem().name());


                try {
                    itemSlotID = Integer.parseInt(itemSlotField.getText().trim());
                } catch (NumberFormatException e) {
                    script.log("ERROR", "Invalid slot ID entered. Please enter a number between 0 and 27.");
                    return; // Don't proceed if invalid input
                }

                if (itemSlotID < 0 || itemSlotID > 27) {
                    script.log("ERROR", "Slot ID out of range. Must be between 0 and 27.");
                    return;
                }

                prefs.putInt(PREF_SELECTED_SLOT_ID, itemSlotID);

                script.log("SAVESETTINGS", "Saved selected spell to preferences: " + spellComboBox.getSelectionModel().getSelectedItem().name());
                script.log("SAVESETTINGS", "Saved selected Slot ID to preferences: " + itemSlotID);

                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(spellLabel, spellComboBox, itemIdLabel, itemSlotField, confirmButton);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedSlotId() {
        return itemSlotID;
    }
}