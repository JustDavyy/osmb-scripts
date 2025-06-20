package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import data.FishingLocation;
import data.FishingMethod;
import data.HandlingMode;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    private static final String PREF_WEBHOOK_ENABLED = "daiofisher_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "daiofisher_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "daiofisher_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "daiofisher_webhook_include_user";
    private static final String PREF_WEBHOOK_INCLUDE_STATS = "daiofisher_webhook_include_stats";
    private static final String PREF_SELECTED_LOCATION = "daiofisher_selected_location";
    private static final String PREF_SELECTED_METHOD = "daiofisher_selected_method";
    private static final String PREF_SELECTED_HANDLING = "daiofisher_selected_handling";

    private final Script script;
    private ComboBox<FishingLocation> locationComboBox;
    private ComboBox<FishingMethod> methodComboBox;
    private ComboBox<HandlingMode> handlingComboBox;

    private HBox fishPreviewBox;
    private Timeline fishCycleTimeline;
    private int fishCycleIndex = 0;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;
    private CheckBox includeStatsCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label locationLabel = new Label("Select location:");
        locationComboBox = new ComboBox<>();
        locationComboBox.setItems(FXCollections.observableArrayList(FishingLocation.values()));
        locationComboBox.getItems().sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        String savedLocation = prefs.get(PREF_SELECTED_LOCATION, FishingLocation.Barb_Village.name());
        FishingLocation initialLocation;
        try {
            initialLocation = FishingLocation.valueOf(savedLocation);
        } catch (IllegalArgumentException e) {
            initialLocation = FishingLocation.Barb_Village;
        }
        locationComboBox.getSelectionModel().select(initialLocation);

        Label methodLabel = new Label("Fishing method:");
        methodComboBox = new ComboBox<>();

        Label handlingLabel = new Label("Handling method:");
        handlingComboBox = new ComboBox<>();

        Label previewLabel = new Label("Catch preview:");
        fishPreviewBox = new HBox(10);
        fishPreviewBox.setStyle("-fx-padding: 5; -fx-alignment: center;");

        // === Listeners ===
        locationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                methodComboBox.setItems(FXCollections.observableArrayList(newVal.getMethods()));
                methodComboBox.getSelectionModel().selectFirst();
            }
        });

        methodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Set handling modes
                handlingComboBox.setItems(FXCollections.observableArrayList(newVal.getHandlingModes()));
                String savedHandling = prefs.get(PREF_SELECTED_HANDLING, "DROP");
                try {
                    HandlingMode handling = HandlingMode.valueOf(savedHandling);
                    if (newVal.getHandlingModes().contains(handling)) {
                        handlingComboBox.getSelectionModel().select(handling);
                    } else {
                        handlingComboBox.getSelectionModel().selectFirst();
                    }
                } catch (IllegalArgumentException e) {
                    handlingComboBox.getSelectionModel().selectFirst();
                }

                startFishPreviewCycle(core, newVal);
            }
        });

        // Initialize with saved location and method
        methodComboBox.setItems(FXCollections.observableArrayList(initialLocation.getMethods()));
        String savedMethod = prefs.get(PREF_SELECTED_METHOD, "Fly Fishing Rod (Feathers)");
        methodComboBox.getItems().stream()
                .filter(m -> m.getName().equals(savedMethod))
                .findFirst()
                .ifPresentOrElse(
                        m -> methodComboBox.getSelectionModel().select(m),
                        () -> methodComboBox.getSelectionModel().selectFirst()
                );

        mainBox.getChildren().addAll(
                locationLabel, locationComboBox,
                methodLabel, methodComboBox,
                previewLabel, fishPreviewBox,
                handlingLabel, handlingComboBox
        );

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField();
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setText(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 15; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username in Webhook");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));
        includeUsernameCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeStatsCheckBox = new CheckBox("Include Script Stats in Webhook");
        includeStatsCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_STATS, true));
        includeStatsCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
            includeStatsCheckBox.setDisable(!enabled);
        });

        webhookBox.getChildren().addAll(
                webhookEnabledCheckBox,
                webhookUrlField,
                new Label("Send interval (minutes)"),
                webhookIntervalComboBox,
                includeUsernameCheckBox,
                includeStatsCheckBox
        );
        Tab webhookTab = new Tab("Webhooks", webhookBox);
        webhookTab.setClosable(false);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 330, 520);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void startFishPreviewCycle(ScriptCore core, FishingMethod method) {
        if (fishCycleTimeline != null) {
            fishCycleTimeline.stop();
        }

        List<Integer> fishList = method.getCatchableFish();
        fishPreviewBox.getChildren().clear();

        if (fishList.size() <= 3) {
            for (Integer id : fishList) {
                ImageView img = JavaFXUtils.getItemImageView(core, id);
                if (img != null) {
                    img.setFitWidth(32);
                    img.setFitHeight(32);
                    fishPreviewBox.getChildren().add(img);
                }
            }
        } else {
            fishCycleIndex = 0;
            fishCycleTimeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
                fishPreviewBox.getChildren().clear();
                for (int i = 0; i < 3; i++) {
                    int index = (fishCycleIndex + i) % fishList.size();
                    ImageView img = JavaFXUtils.getItemImageView(core, fishList.get(index));
                    if (img != null) {
                        img.setFitWidth(32);
                        img.setFitHeight(32);
                        fishPreviewBox.getChildren().add(img);
                    }
                }
                fishCycleIndex = (fishCycleIndex + 1) % fishList.size();
            }));
            fishCycleTimeline.setCycleCount(Animation.INDEFINITE);
            fishCycleTimeline.play();
        }
    }

    private void saveSettings() {
        prefs.put(PREF_SELECTED_LOCATION, getSelectedLocation().name());
        prefs.put(PREF_SELECTED_METHOD, getSelectedMethod().getName());
        prefs.put(PREF_SELECTED_HANDLING, getSelectedHandlingMethod().name());
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_STATS, isStatsIncluded());

        if (fishCycleTimeline != null) {
            fishCycleTimeline.stop();
        }

        ((Stage) methodComboBox.getScene().getWindow()).close();
    }

    public FishingLocation getSelectedLocation() {
        return locationComboBox.getValue();
    }

    public FishingMethod getSelectedMethod() {
        return methodComboBox.getValue();
    }

    public HandlingMode getSelectedHandlingMethod() {
        return handlingComboBox.getValue();
    }

    public boolean isWebhookEnabled() {
        return webhookEnabledCheckBox != null && webhookEnabledCheckBox.isSelected();
    }

    public String getWebhookUrl() {
        return webhookUrlField != null ? webhookUrlField.getText().trim() : "";
    }

    public int getWebhookInterval() {
        return webhookIntervalComboBox != null && webhookIntervalComboBox.getValue() != null
                ? webhookIntervalComboBox.getValue()
                : 5;
    }

    public boolean isUsernameIncluded() {
        return includeUsernameCheckBox != null && includeUsernameCheckBox.isSelected();
    }

    public boolean isStatsIncluded() {
        return includeStatsCheckBox != null && includeStatsCheckBox.isSelected();
    }
}