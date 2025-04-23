package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_SPELL = "selected_teleport_spell";

    private final Script script;
    private ComboBox<StandardSpellbook> spellComboBox;

    private static final StandardSpellbook[] TELEPORT_SPELLS = {
            StandardSpellbook.VARROCK_TELEPORT,
            StandardSpellbook.LUMBRIDGE_TELEPORT,
            StandardSpellbook.FALADOR_TELEPORT,
            StandardSpellbook.CAMELOT_TELEPORT,
            StandardSpellbook.KOUREND_TELEPORT,
            StandardSpellbook.ARDOUGNE_TELEPORT,
            StandardSpellbook.CIVITAS_ILLA_FORTIS_TELEPORT,
            StandardSpellbook.WATCHTOWER_TELEPORT,
            StandardSpellbook.TROLLHEIM_TELEPORT
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        // Spell selection
        Label spellLabel = new Label("Choose teleport to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(TELEPORT_SPELLS);

        String savedSpell = prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.CAMELOT_TELEPORT.name());
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(savedSpell));
        script.log("SAVESETTINGS", "Loaded saved teleport spell from preferences: " + savedSpell);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            if (spellComboBox.getSelectionModel().getSelectedIndex() >= 0) {
                prefs.put(PREF_SELECTED_SPELL, spellComboBox.getSelectionModel().getSelectedItem().name());

                script.log("SAVESETTINGS", "Saved selected teleport spell to preferences: " + spellComboBox.getSelectionModel().getSelectedItem().name());

                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(spellLabel, spellComboBox, confirmButton);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }
}