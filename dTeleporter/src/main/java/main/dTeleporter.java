package main;

// GENERAL JAVA IMPORTS
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

import tasks.TeleportTask;
import tasks.Setup;
import utils.Task;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;


// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dTeleporter",
        description = "Trains magic by continuously casting teleportation spells.",
        skillCategory = SkillCategory.MAGIC,
        version = 1.1,
        author = "JustDavyy"
)

public class dTeleporter extends Script {
    public static boolean setupDone = false;
    public static StandardSpellbook spellToCast;
    public static boolean hasReqs = true;

    private List<Task> tasks;

    public dTeleporter(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12853, // Varrock
                12850, // Lumbridge
                11828, // Falador
                11062, // Camelot
                6457,  // Kourend
                10547, // Ardougne
                6704,  // Kourend1
                6705,  // Kourend2
                10032, // Watchtower
                11577, // Trollheim
        };
    }

    @Override
    public void onStart(){
        log("INFO", "Starting dTeleporter v1.1");

        // Build and show our UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Teleporter Options", false);

        spellToCast = ui.getSelectedSpell();

        log("DEBUG", "We are casting " + spellToCast.getName());

        // Build our list of tasks, tasks will be trying to execute from top to bottom
        tasks = Arrays.asList(
                new Setup(this),
                new TeleportTask(this)
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
