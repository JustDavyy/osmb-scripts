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
    private static final String PREF_SELECTED_SPELL = "selected_spell";
    private static final String PREF_SELECTED_ITEM_ID = "selected_item_id";

    private final Script script;
    private ImageView itemToAlchView;
    private ComboBox<StandardSpellbook> spellComboBox;
    private int selectedItemID = ItemID.BANK_FILLER;

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
        selectedItemID = prefs.getInt(PREF_SELECTED_ITEM_ID, ItemID.BANK_FILLER);
        script.log("SAVESETTINGS", "Loaded saved item ID from preferences: " + selectedItemID);

        // Item view and search
        Label itemLabel = new Label("Item to alch");
        itemToAlchView = JavaFXUtils.getItemImageView(core, selectedItemID);
        itemToAlchView.setFitWidth(32);
        itemToAlchView.setFitHeight(32);

        Button itemSearchButton = new Button("\uD83D\uDD0E Search");
        itemSearchButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton.getScene().getWindow());
            if (itemID == -1) itemID = ItemID.BANK_FILLER;

            ImageView imageView = JavaFXUtils.getItemImageView(core, itemID);
            if (imageView != null) {
                selectedItemID = itemID;
                itemToAlchView.setImage(imageView.getImage());
            }
        });

        HBox itemSelectionHBox = new HBox(itemToAlchView, itemSearchButton);
        itemSelectionHBox.setSpacing(5);
        itemSelectionHBox.setStyle("-fx-alignment: center");

        VBox itemBox = new VBox(itemLabel, itemSelectionHBox);
        itemBox.setSpacing(5);
        itemBox.setStyle("-fx-alignment: center");

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            if (spellComboBox.getSelectionModel().getSelectedIndex() >= 0) {
                prefs.put(PREF_SELECTED_SPELL, spellComboBox.getSelectionModel().getSelectedItem().name());
                prefs.putInt(PREF_SELECTED_ITEM_ID, selectedItemID);

                script.log("SAVESETTINGS", "Saved selected spell to preferences: " + spellComboBox.getSelectionModel().getSelectedItem().name());
                script.log("SAVESETTINGS", "Saved selected item ID to preferences: " + selectedItemID);

                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(spellLabel, spellComboBox, itemBox, confirmButton);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedItemId() {
        return selectedItemID;
    }
}