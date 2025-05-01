package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.javafx.JavaFXUtils;
import data.CookingItem;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_COOKING_ITEM = "dcooker_selected_item";

    private final Script script;
    private ComboBox<CookingItem> cookingComboBox;

    private static final CookingItem[] COOKING_ITEMS = CookingItem.values();

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        Label cookingLabel = new Label("Choose item to cook");
        cookingComboBox = createCookingComboBox(core);

        // Load saved item selection
        int savedItemId = prefs.getInt(PREF_SELECTED_COOKING_ITEM, ItemID.RAW_SHRIMPS);
        CookingItem savedItem = CookingItem.fromRawItemId(savedItemId);
        if (savedItem != null) {
            cookingComboBox.getSelectionModel().select(savedItem);
        }

        script.log("SAVESETTINGS", "Loaded selected cooking item ID from preferences: " + savedItemId);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            CookingItem selected = cookingComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                prefs.putInt(PREF_SELECTED_COOKING_ITEM, selected.getRawItemId());
                script.log("SAVESETTINGS", "Saved selected cooking item ID to preferences: " + selected.getRawItemId());
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(cookingLabel, cookingComboBox, confirmButton);
        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<CookingItem> createCookingComboBox(ScriptCore core) {
        ComboBox<CookingItem> comboBox = new ComboBox<>();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CookingItem item) {
                return item != null ? core.getItemManager().getItemName(item.getRawItemId()) : "";
            }

            @Override
            public CookingItem fromString(String string) {
                return null;
            }
        });

        comboBox.setCellFactory(param -> createItemCell(core));
        comboBox.setButtonCell(createItemCell(core));
        comboBox.getItems().addAll(COOKING_ITEMS);
        return comboBox;
    }

    private ListCell<CookingItem> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(CookingItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    String name = core.getItemManager().getItemName(item.getRawItemId());
                    ImageView imageView = JavaFXUtils.getItemImageView(core, item.getRawItemId());
                    if (imageView != null) {
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                    }
                    setGraphic(imageView);
                    setText(name != null ? name : "Unknown");
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        };
    }

    public int getSelectedItemId() {
        CookingItem selected = cookingComboBox.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getRawItemId() : -1;
    }

    public int getSelectedCookedItemId() {
        CookingItem selected = cookingComboBox.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getCookedItemId() : -1;
    }
}
