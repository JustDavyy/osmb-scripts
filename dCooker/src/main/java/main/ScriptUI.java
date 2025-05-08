package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.script.Script;
import data.CookingItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTION_MODE = "dcooker_selection_mode";
    private static final String PREF_SELECTED_ITEM = "dcooker_selected_item";
    private static final String PREF_SELECTED_MULTIPLE_ITEMS = "dcooker_selected_multiple_items";
    private static final String PREF_BANK_METHOD = "dcooker_bank_method";

    private final Script script;
    private ComboBox<String> selectionModeComboBox;
    private ComboBox<String> bankMethodComboBox;

    private ComboBox<CookingItem> singleItemComboBox;
    private ListView<Integer> multipleItemsListView;
    private final ObservableList<Integer> multipleItemIds = FXCollections.observableArrayList();

    private VBox itemSelectionBox;

    private static final String[] BANK_METHOD_OPTIONS = {"Item by item", "Deposit all"};

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-spacing: 10; -fx-alignment: center");

        // Mode selection
        Label modeLabel = new Label("Selection mode");
        selectionModeComboBox = new ComboBox<>();
        selectionModeComboBox.getItems().addAll("Single Item", "Multiple Items");
        String savedMode = prefs.get(PREF_SELECTION_MODE, "Single Item");
        selectionModeComboBox.getSelectionModel().select(savedMode);
        script.log("SAVESETTINGS", "Loaded selection mode: " + savedMode);

        // Item selection UI
        itemSelectionBox = new VBox();
        itemSelectionBox.setSpacing(8);
        itemSelectionBox.setStyle("-fx-alignment: center");
        updateItemSelectionUI(core);
        selectionModeComboBox.setOnAction(e -> updateItemSelectionUI(core));

        // Bank method
        Label bankLabel = new Label("Bank method");
        bankMethodComboBox = new ComboBox<>();
        bankMethodComboBox.getItems().addAll(BANK_METHOD_OPTIONS);
        String savedBankMethod = prefs.get(PREF_BANK_METHOD, BANK_METHOD_OPTIONS[0]);
        bankMethodComboBox.getSelectionModel().select(savedBankMethod);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            prefs.put(PREF_SELECTION_MODE, selectionModeComboBox.getSelectionModel().getSelectedItem());
            prefs.put(PREF_BANK_METHOD, bankMethodComboBox.getSelectionModel().getSelectedItem());

            if (isMultipleSelectionMode()) {
                String joined = multipleItemIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                prefs.put(PREF_SELECTED_MULTIPLE_ITEMS, joined);
                script.log("SAVESETTINGS", "Saved multiple cooking item IDs: " + joined);
            } else {
                CookingItem selected = singleItemComboBox.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    prefs.putInt(PREF_SELECTED_ITEM, selected.getRawItemId());
                    script.log("SAVESETTINGS", "Saved single cooking item ID: " + selected.getRawItemId());
                }
            }

            ((Stage) confirmButton.getScene().getWindow()).close();
        });

        root.getChildren().addAll(modeLabel, selectionModeComboBox, itemSelectionBox, bankLabel, bankMethodComboBox, confirmButton);
        Scene scene = new Scene(root, 250, 400);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void updateItemSelectionUI(ScriptCore core) {
        itemSelectionBox.getChildren().clear();
        if (isMultipleSelectionMode()) {
            setupMultipleSelection(core);
        } else {
            setupSingleSelection(core);
        }
    }

    private void setupSingleSelection(ScriptCore core) {
        Label label = new Label("Select item to cook");

        singleItemComboBox = new ComboBox<>();
        singleItemComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CookingItem item) {
                return item != null ? core.getItemManager().getItemName(item.getRawItemId()) : "";
            }

            @Override
            public CookingItem fromString(String string) {
                return null;
            }
        });
        singleItemComboBox.setCellFactory(param -> createItemCell(core));
        singleItemComboBox.setButtonCell(createItemCell(core));
        singleItemComboBox.getItems().addAll(CookingItem.values());

        int savedId = prefs.getInt(PREF_SELECTED_ITEM, ItemID.RAW_SHRIMPS);
        CookingItem savedItem = CookingItem.fromRawItemId(savedId);
        if (savedItem != null) {
            singleItemComboBox.getSelectionModel().select(savedItem);
        }

        itemSelectionBox.getChildren().addAll(label, singleItemComboBox);
    }

    private void setupMultipleSelection(ScriptCore core) {
        Label label = new Label("Select multiple items to cook");

        multipleItemsListView = new ListView<>(multipleItemIds);
        multipleItemsListView.setPrefHeight(280);
        multipleItemsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId != null && !empty) {
                    String name = core.getItemManager().getItemName(itemId);
                    ImageView img = JavaFXUtils.getItemImageView(core, itemId);
                    if (img != null) {
                        img.setFitWidth(16);
                        img.setFitHeight(16);
                    }
                    setGraphic(img);
                    setText(name != null ? name : "Unknown");
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        String saved = prefs.get(PREF_SELECTED_MULTIPLE_ITEMS, "");
        if (!saved.isEmpty()) {
            try {
                List<Integer> ids = Arrays.stream(saved.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                multipleItemIds.setAll(ids);
            } catch (Exception e) {
                script.log("ERROR", "Failed to parse multiple item IDs: " + saved);
            }
        }

        Button addButton = new Button("➕ Add Item");
        addButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) addButton.getScene().getWindow());
            if (itemID != -1 && !multipleItemIds.contains(itemID)) {
                multipleItemIds.add(itemID);
            }
        });

        Button removeButton = new Button("❌ Remove Selected");
        removeButton.setOnAction(event -> {
            Integer selected = multipleItemsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                multipleItemIds.remove(selected);
            }
        });

        HBox buttons = new HBox(addButton, removeButton);
        buttons.setSpacing(10);
        buttons.setStyle("-fx-alignment: center");

        itemSelectionBox.getChildren().addAll(label, multipleItemsListView, buttons);
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

    public boolean isMultipleSelectionMode() {
        return "Multiple Items".equals(selectionModeComboBox.getSelectionModel().getSelectedItem());
    }

    public int getSelectedItemId() {
        CookingItem selected = singleItemComboBox.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getRawItemId() : -1;
    }

    public int getSelectedCookedItemId() {
        CookingItem selected = singleItemComboBox.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getCookedItemId() : -1;
    }

    public ObservableList<Integer> getMultipleSelectedItemIds() {
        return multipleItemIds;
    }

    public String getSelectedBankMethod() {
        return bankMethodComboBox.getSelectionModel().getSelectedItem();
    }
}