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
    private static final String PREF_SELECTED_STAFF = "dbattlestaffcrafter_selected_staff";

    private final Script script;
    private ComboBox<Integer> staffComboBox;

    private static final Integer[] STAFF_OPTIONS = {
            ItemID.AIR_BATTLESTAFF,
            ItemID.WATER_BATTLESTAFF,
            ItemID.EARTH_BATTLESTAFF,
            ItemID.FIRE_BATTLESTAFF
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        Label staffLabel = new Label("Choose battlestaff to make");
        staffComboBox = createStaffComboBox(core);

        // Load saved staff selection
        int savedItemId = prefs.getInt(PREF_SELECTED_STAFF, ItemID.WATER_BATTLESTAFF);
        for (Integer option : STAFF_OPTIONS) {
            if (option.equals(savedItemId)) {
                staffComboBox.getSelectionModel().select(option);
                break;
            }
        }

        script.log("SAVESETTINGS", "Loaded selected battlestaff ID from preferences: " + savedItemId);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            Integer selected = staffComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                prefs.putInt(PREF_SELECTED_STAFF, selected);
                script.log("SAVESETTINGS", "Saved selected battlestaff ID to preferences: " + selected);
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        root.getChildren().addAll(staffLabel, staffComboBox, confirmButton);
        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createStaffComboBox(ScriptCore core) {
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

        comboBox.getItems().addAll(STAFF_OPTIONS);
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

    public int getSelectedStaffId() {
        return staffComboBox.getSelectionModel().getSelectedItem();
    }
}