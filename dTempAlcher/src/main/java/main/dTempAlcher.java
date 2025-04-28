package main;

import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResult;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

import tasks.AlchTask;
import tasks.Setup;
import utils.Task;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import javafx.collections.ObservableList;

@ScriptDefinition(
        name = "dTemp Alcher",
        description = "Used as a substitute from dPublic Alcher to allow noted items to be alched by choosing a slot ID to alch instead.",
        skillCategory = SkillCategory.MAGIC,
        version = 1.3,
        author = "JustDavyy"
)
public class dTempAlcher extends Script {
    public static boolean setupDone = false;
    public static StandardSpellbook spellToCast;
    public static int alchSlotID;
    public static boolean hasReqs = true;
    public static UIResult<Rectangle> itemRect;

    public static boolean multipleSlotsMode = false;
    public static List<Integer> slotsToAlch;
    public static int currentSlotIndex = 0;

    private List<Task> tasks;

    public dTempAlcher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12598, 6461, 7222, 12633
        };
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dTemp Alcher v1.3");

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Alcher Options", false);

        spellToCast = ui.getSelectedSpell();

        if (ui.isMultipleSelectionMode()) {
            multipleSlotsMode = true;
            slotsToAlch = ui.getMultipleSelectedSlotIds();
            if (slotsToAlch.isEmpty()) {
                log("ERROR", "Multiple slot mode selected but no slots found. Stopping.");
                stop();
                return;
            }
            alchSlotID = slotsToAlch.get(0);
        } else {
            multipleSlotsMode = false;
            alchSlotID = ui.getSelectedSlotId();
        }

        log("DEBUG", "Mode: " + (multipleSlotsMode ? "Multiple" : "Single"));
        log("DEBUG", "Starting with slot: " + alchSlotID + " using: " + spellToCast);

        tasks = Arrays.asList(
                new Setup(this),
                new AlchTask(this)
        );
    }

    @Override
    public int poll() {
        if (tasks != null) {
            for (Task task : tasks) {
                if (task.activate()) {
                    task.execute();
                    return 0;
                }
            }
        }
        return 0;
    }
}
