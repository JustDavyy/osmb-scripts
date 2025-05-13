package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_BANKING_OPTION = "dkarambwanfisher_banking_option";
    private static final String PREF_FAIRY_RING_OPTION = "dkarambwanfisher_fairy_ring_option";

    private final Script script;
    private ComboBox<String> bankingOptionsComboBox;
    private ComboBox<String> fairyRingOptionsComboBox;
    private Scene scene;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label bankingLabel = new Label("Banking options");
        bankingOptionsComboBox = new ComboBox<>();
        bankingOptionsComboBox.setItems(FXCollections.observableArrayList("Zanaris", "Crafting Guild"));
        String savedBanking = prefs.get(PREF_BANKING_OPTION, "Zanaris");
        bankingOptionsComboBox.getSelectionModel().select(savedBanking);
        script.log(getClass().getSimpleName(), "Loaded banking option: " + savedBanking);

        Label fairyRingLabel = new Label("Fairy ring options");
        fairyRingOptionsComboBox = new ComboBox<>();
        fairyRingOptionsComboBox.setItems(FXCollections.observableArrayList("Zanaris", "Ardougne cloak", "Quest cape"));
        String savedFairy = prefs.get(PREF_FAIRY_RING_OPTION, "Zanaris");
        fairyRingOptionsComboBox.getSelectionModel().select(savedFairy);
        script.log(getClass().getSimpleName(), "Loaded fairy ring option: " + savedFairy);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        root.getChildren().addAll(bankingLabel, bankingOptionsComboBox, fairyRingLabel, fairyRingOptionsComboBox, confirmButton);

        scene = new Scene(root, 220, 250);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void saveSettings() {
        prefs.put(PREF_BANKING_OPTION, bankingOptionsComboBox.getSelectionModel().getSelectedItem());
        prefs.put(PREF_FAIRY_RING_OPTION, fairyRingOptionsComboBox.getSelectionModel().getSelectedItem());
        script.log(getClass().getSimpleName(), "Saved banking option: " + getSelectedBankingOption());
        script.log(getClass().getSimpleName(), "Saved fairy ring option: " + getSelectedFairyRingOption());
        ((Stage) bankingOptionsComboBox.getScene().getWindow()).close();
    }

    public String getSelectedBankingOption() {
        return bankingOptionsComboBox.getSelectionModel().getSelectedItem();
    }

    public String getSelectedFairyRingOption() {
        return fairyRingOptionsComboBox.getSelectionModel().getSelectedItem();
    }
}
