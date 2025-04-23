package main;

// GENERAL JAVA IMPORTS
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResult;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

import tasks.AlchTask;
import tasks.Setup;
import utils.Task;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;


// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dTemp Alcher",
        description = "Used as a substitute from dPublic Alcher to allow noted items to be alched by chosing a slot ID to alch instead.",
        skillCategory = SkillCategory.MAGIC,
        version = 1.0,
        author = "JustDavyy"
)

public class dTempAlcher extends Script {
    public static boolean setupDone = false;
    public static StandardSpellbook spellToCast;
    public static int alchSlotID;
    public static String itemName;
    public static boolean hasReqs = true;
    public static UIResult<Rectangle> itemRect;

    private List<Task> tasks;

    public dTempAlcher(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12598, // Grand Exchange
                6461, // Wintertodt bank
                7222, // Tithe farm
                12633, // Death's office
        };
    }

    @Override
    public void onStart(){
        log("INFO", "Starting dTemp Alcher v1.0");

        // Build and show our UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Alcher Options", false);

        spellToCast = ui.getSelectedSpell();
        alchSlotID = ui.getSelectedSlotId();

        log("DEBUG", "We are alching items in slot: " + alchSlotID + " using: " + spellToCast);

        // Build our list of tasks, tasks will be trying to execute from top to bottom
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
