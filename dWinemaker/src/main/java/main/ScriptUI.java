package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.javafx.JavaFXUtils;
import data.ItemIdentifier;

import data.SimpleItemIdentifier;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.image.ImageView;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_WINE = "selected_wine";

    private final Script script;
    private ComboBox<ItemIdentifier> wineComboBox;

    private static final ItemIdentifier[] WINE_OPTIONS = {
            new SimpleItemIdentifier(ItemID.JUG_OF_WINE),
            new SimpleItemIdentifier(ItemID.WINE_OF_ZAMORAK)
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        Label wineLabel = new Label("Choose wine to make");
        wineComboBox = createWineComboBox(core);

        // Load saved wine selection
        int savedItemId = prefs.getInt(PREF_SELECTED_WINE, ItemID.JUG_OF_WINE);
        for (ItemIdentifier wine : WINE_OPTIONS) {
            if (wine.getItemID() == savedItemId) {
                wineComboBox.getSelectionModel().select(wine);
                break;
            }
        }
        script.log("SAVESETTINGS", "Loaded selected wine ID from preferences: " + savedItemId);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            ItemIdentifier selected = wineComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                prefs.putInt(PREF_SELECTED_WINE, selected.getItemID());
                script.log("SAVESETTINGS", "Saved selected wine ID to preferences: " + selected.getItemID());
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(wineLabel, wineComboBox, confirmButton);
        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<ItemIdentifier> createWineComboBox(ScriptCore core) {
        ComboBox<ItemIdentifier> comboBox = new ComboBox<>();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ItemIdentifier item) {
                return item != null ? core.getItemManager().getItemName(item.getItemID()) : "";
            }

            @Override
            public ItemIdentifier fromString(String string) {
                return null;
            }
        });
        comboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ItemIdentifier item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    int itemId = item.getItemID();
                    String name = core.getItemManager().getItemName(itemId);
                    ImageView imageView = JavaFXUtils.getItemImageView(core, itemId);
                    setGraphic(imageView);
                    setText(name);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });
        comboBox.getItems().addAll(WINE_OPTIONS);
        return comboBox;
    }

    public ItemIdentifier getSelectedWine() {
        return wineComboBox.getSelectionModel().getSelectedItem();
    }
}