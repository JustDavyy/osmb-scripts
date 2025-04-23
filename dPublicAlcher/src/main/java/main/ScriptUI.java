package main;

// OSMB specific imports
import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import com.osmb.api.javafx.ItemSearchDialogue;

// General java imports
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptUI {
    private ImageView itemToAlchView;
    private static final StandardSpellbook[] ALCHEMY_SPELLS = {
            StandardSpellbook.LOW_ALCHEMY,
            StandardSpellbook.HIGH_LEVEL_ALCHEMY
    };

    private ComboBox<StandardSpellbook> spellComboBox;
    private int selectedItemID = ItemID.BANK_FILLER;

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        // Spell selection
        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(ALCHEMY_SPELLS);
        spellComboBox.getSelectionModel().select(StandardSpellbook.HIGH_LEVEL_ALCHEMY);

        // Item to alch
        Label itemLabel = new Label("Item to alch");

        itemToAlchView = JavaFXUtils.getItemImageView(core, selectedItemID);
        itemToAlchView.setFitWidth(32);
        itemToAlchView.setFitHeight(32);

        Button itemSearchButton = new Button("\uD83D\uDD0E Search");
        itemSearchButton.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton.getScene().getWindow());
            if (itemID == -1) {
                itemID = ItemID.BANK_FILLER;
            }
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
        confirmButton.setOnAction(actionEvent -> {
            if (spellComboBox.getSelectionModel().getSelectedIndex() >= 0)
                ((Stage) confirmButton.getScene().getWindow()).close();
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