package main;

import com.osmb.api.ScriptCore;
import courses.WyrmAdvanced;
import courses.WyrmBasic;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class UI {

    private final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_SELECTED_COURSE = "dwyrmagility_selected_course";

    private ComboBox<Course> selectCourseComboBox;

    public Scene buildScene(ScriptCore core) {
        VBox vBox = new VBox();
        vBox.setSpacing(20);
        vBox.setStyle("-fx-background-color: #636E72; -fx-padding: 10");

        HBox selectCourseBox = new HBox();
        selectCourseBox.setSpacing(10);
        selectCourseBox.setAlignment(Pos.CENTER_LEFT);

        Label selectCourseLabel = new Label("Select Course");
        selectCourseComboBox = new ComboBox<>();
        selectCourseComboBox.getItems().addAll(
                new WyrmBasic((dWyrmAgility) core),
                new WyrmAdvanced((dWyrmAgility) core)
        );

        selectCourseComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.name() : "";
            }

            @Override
            public Course fromString(String string) {
                return selectCourseComboBox.getItems().stream()
                        .filter(course -> course.name().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        loadSelectedCourse();

        selectCourseBox.getChildren().addAll(selectCourseLabel, selectCourseComboBox);

        vBox.getChildren().add(selectCourseBox);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            if (selectedCourse() != null) {
                saveSelectedCourse();
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        vBox.getChildren().add(confirmButton);

        Scene scene = new Scene(vBox);
        scene.getStylesheets().add("style.css");

        return scene;
    }

    private void loadSelectedCourse() {
        String savedCourse = prefs.get(PREF_SELECTED_COURSE, "");
        if (!savedCourse.isEmpty()) {
            Course match = selectCourseComboBox.getItems().stream()
                    .filter(course -> course.name().equals(savedCourse))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                selectCourseComboBox.getSelectionModel().select(match);
                System.out.println("[SAVESETTINGS] Loaded course: " + savedCourse);
            }
        }
    }

    private void saveSelectedCourse() {
        Course course = selectCourseComboBox.getSelectionModel().getSelectedItem();
        if (course != null) {
            prefs.put(PREF_SELECTED_COURSE, course.name());
            System.out.println("[SAVESETTINGS] Saved selected course: " + course.name());
        }
    }

    public Course selectedCourse() {
        return selectCourseComboBox.getSelectionModel().getSelectedItem();
    }
}
