package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_SPELL = "dpublicalcher_selected_alch_spell";
    private static final String PREF_SELECTED_ITEM_ID = "dpublicalcher_selected_item_id";
    private static final String PREF_SELECTED_MULTIPLE_ITEMS = "dpublicalcher_selected_multiple_item_ids";
    private static final String PREF_SELECTION_MODE = "dpublicalcher_selection_mode";

    private final Script script;
    private ComboBox<StandardSpellbook> spellComboBox;
    private ComboBox<String> selectionModeComboBox;
    private ImageView itemToAlchView;
    private ListView<Integer> multipleItemsView;
    private Scene scene;
    private Stage window;

    private int selectedItemID = ItemID.BANK_FILLER;
    private ObservableList<Integer> multipleSelectedItemIDs = FXCollections.observableArrayList();

    private VBox itemSelectionBox;

    private static final StandardSpellbook[] ALCHEMY_SPELLS = {
            StandardSpellbook.LOW_ALCHEMY,
            StandardSpellbook.HIGH_LEVEL_ALCHEMY
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(ALCHEMY_SPELLS);

        String savedSpell = prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.HIGH_LEVEL_ALCHEMY.name());
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(savedSpell));

        script.log("SAVESETTINGS", "Loaded saved spell from preferences: " + savedSpell);

        Label modeLabel = new Label("Selection mode");
        selectionModeComboBox = new ComboBox<>();
        selectionModeComboBox.getItems().addAll("Single Item", "Multiple Items");

        String savedMode = prefs.get(PREF_SELECTION_MODE, "Single Item");
        selectionModeComboBox.getSelectionModel().select(savedMode);
        script.log("SAVESETTINGS", "Loaded selection mode: " + savedMode);

        itemSelectionBox = new VBox();
        itemSelectionBox.setSpacing(8);
        itemSelectionBox.setStyle("-fx-alignment: center");

        updateItemSelectionUI(core);

        selectionModeComboBox.setOnAction(e -> updateItemSelectionUI(core));

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        root.getChildren().addAll(spellLabel, spellComboBox, modeLabel, selectionModeComboBox, itemSelectionBox, confirmButton);

        scene = new Scene(root, 200, 400);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void updateItemSelectionUI(ScriptCore core) {
        itemSelectionBox.getChildren().clear();
        if (isMultipleSelectionMode()) {
            setupMultipleItemSelection(core);
        } else {
            setupSingleItemSelection(core);
        }
    }

    private void setupSingleItemSelection(ScriptCore core) {
        Label itemLabel = new Label("Item to alch");

        selectedItemID = prefs.getInt(PREF_SELECTED_ITEM_ID, ItemID.BANK_FILLER);

        itemToAlchView = JavaFXUtils.getItemImageView(core, selectedItemID);
        if (itemToAlchView == null) {
            itemToAlchView = new ImageView();
        }
        itemToAlchView.setFitWidth(32);
        itemToAlchView.setFitHeight(32);

        Button searchButton = new Button("\uD83D\uDD0E Search");
        searchButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) searchButton.getScene().getWindow());
            if (itemID != -1) {
                selectedItemID = itemID;
                ImageView newImage = JavaFXUtils.getItemImageView(core, itemID);
                if (newImage != null) {
                    itemToAlchView.setImage(newImage.getImage());
                }
            }
        });

        HBox hbox = new HBox(itemToAlchView, searchButton);
        hbox.setSpacing(8);
        hbox.setStyle("-fx-alignment: center");

        itemSelectionBox.getChildren().addAll(itemLabel, hbox);
    }

    private void setupMultipleItemSelection(ScriptCore core) {
        Label itemsLabel = new Label("Items to alch");

        multipleItemsView = new ListView<>(multipleSelectedItemIDs);
        multipleItemsView.setPrefHeight(320);
        multipleItemsView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Integer> call(ListView<Integer> param) {
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
                            setMaxWidth(250);
                            setStyle("-fx-alignment: center-left;");
                        } else {
                            setText(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        });

        String savedMultiple = prefs.get(PREF_SELECTED_MULTIPLE_ITEMS, "");
        if (!savedMultiple.isEmpty()) {
            try {
                List<Integer> ids = Arrays.stream(savedMultiple.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                multipleSelectedItemIDs.setAll(ids);
                script.log("SAVESETTINGS", "Loaded multiple selected items: " + savedMultiple);
            } catch (Exception e) {
                script.log("ERROR", "Failed parsing multiple item IDs: " + savedMultiple);
            }
        }

        Button addItemButton = new Button("\uD83D\uDD0E Add Item");
        addItemButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) addItemButton.getScene().getWindow());
            if (itemID != -1 && !multipleSelectedItemIDs.contains(itemID)) {
                multipleSelectedItemIDs.add(itemID);
            }
        });

        Button removeItemButton = new Button("Remove Selected");
        removeItemButton.setOnAction(event -> {
            Integer selectedItem = multipleItemsView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                multipleSelectedItemIDs.remove(selectedItem);
            }
        });

        HBox buttonBox = new HBox(addItemButton, removeItemButton);
        buttonBox.setSpacing(8);
        buttonBox.setStyle("-fx-alignment: center");

        itemSelectionBox.getChildren().addAll(itemsLabel, multipleItemsView, buttonBox);
    }

    private void saveSettings() {
        if (spellComboBox.getSelectionModel().isEmpty()) return;

        prefs.put(PREF_SELECTED_SPELL, spellComboBox.getSelectionModel().getSelectedItem().name());
        prefs.put(PREF_SELECTION_MODE, selectionModeComboBox.getSelectionModel().getSelectedItem()); // << Save mode

        if (isMultipleSelectionMode()) {
            String joinedIds = multipleSelectedItemIDs.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            prefs.put(PREF_SELECTED_MULTIPLE_ITEMS, joinedIds);
            script.log("SAVESETTINGS", "Saved multiple selected items: " + joinedIds);
        } else {
            prefs.putInt(PREF_SELECTED_ITEM_ID, selectedItemID);
            script.log("SAVESETTINGS", "Saved selected single item ID: " + selectedItemID);
        }

        ((Stage) itemSelectionBox.getScene().getWindow()).close();
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedItemId() {
        return selectedItemID;
    }

    public ObservableList<Integer> getMultipleSelectedItemIds() {
        return multipleSelectedItemIDs;
    }

    public boolean isMultipleSelectionMode() {
        return selectionModeComboBox.getSelectionModel().getSelectedItem().equals("Multiple Items");
    }
}
