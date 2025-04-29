package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.javafx.JavaFXUtils;
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
    private ComboBox<Integer> cookingComboBox;

    private static final Integer[] COOKING_OPTIONS = {
            ItemID.RAW_SHRIMPS,
            ItemID.SEAWEED,
            ItemID.GIANT_SEAWEED,
            ItemID.BREAD_DOUGH,
            ItemID.RAW_CHICKEN,
            ItemID.RAW_ANCHOVIES,
            ItemID.RAW_SARDINE,
            ItemID.RAW_HERRING,
            ItemID.RAW_MACKEREL,
            ItemID.UNCOOKED_BERRY_PIE,
            ItemID.RAW_TROUT,
            ItemID.RAW_COD,
            ItemID.RAW_PIKE,
            ItemID.UNCOOKED_MEAT_PIE,
            ItemID.RAW_SALMON,
            ItemID.UNCOOKED_STEW,
            ItemID.RAW_TUNA,
            ItemID.UNCOOKED_APPLE_PIE,
            ItemID.RAW_KARAMBWAN,
            ItemID.RAW_GARDEN_PIE,
            ItemID.RAW_LOBSTER,
            ItemID.RAW_BASS,
            ItemID.RAW_SWORDFISH,
            ItemID.RAW_FISH_PIE,
            ItemID.UNCOOKED_BOTANICAL_PIE,
            ItemID.UNCOOKED_MUSHROOM_PIE,
            ItemID.UNCOOKED_CURRY,
            ItemID.RAW_MONKFISH,
            ItemID.RAW_ADMIRAL_PIE,
            ItemID.UNCOOKED_DRAGONFRUIT_PIE,
            ItemID.RAW_SHARK,
            ItemID.RAW_SEA_TURTLE,
            ItemID.RAW_ANGLERFISH,
            ItemID.RAW_WILD_PIE,
            ItemID.RAW_DARK_CRAB,
            ItemID.RAW_MANTA_RAY,
            ItemID.RAW_SUMMER_PIE
    };

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
        for (Integer option : COOKING_OPTIONS) {
            if (option.equals(savedItemId)) {
                cookingComboBox.getSelectionModel().select(option);
                break;
            }
        }

        script.log("SAVESETTINGS", "Loaded selected cooking item ID from preferences: " + savedItemId);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            Integer selected = cookingComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                prefs.putInt(PREF_SELECTED_COOKING_ITEM, selected);
                script.log("SAVESETTINGS", "Saved selected cooking item ID to preferences: " + selected);
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(cookingLabel, cookingComboBox, confirmButton);
        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createCookingComboBox(ScriptCore core) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer itemId) {
                return itemId != null ? core.getItemManager().getItemName(itemId) : "";
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        comboBox.setCellFactory(param -> createItemCell(core));
        comboBox.setButtonCell(createItemCell(core)); // Shows icon + name when dropdown is closed

        comboBox.getItems().addAll(COOKING_OPTIONS);
        return comboBox;
    }

    private ListCell<Integer> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId != null && !empty) {
                    String name = core.getItemManager().getItemName(itemId);
                    ImageView imageView = JavaFXUtils.getItemImageView(core, itemId);
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
        return cookingComboBox.getSelectionModel().getSelectedItem();
    }
}
