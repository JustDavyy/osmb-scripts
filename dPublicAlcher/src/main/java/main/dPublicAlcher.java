package main;

import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResult;
import javafx.scene.Scene;
import tasks.AlchTask;
import tasks.Setup;
import utils.Task;

import java.util.Arrays;
import java.util.List;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;

@ScriptDefinition(
        name = "dPublic Alcher",
        description = "Alchs items (both high & low) until out of items or runes.",
        skillCategory = SkillCategory.MAGIC,
        version = 1.4,
        author = "JustDavyy"
)
public class dPublicAlcher extends Script {
    public static boolean setupDone = false;
    public static StandardSpellbook spellToCast;
    public static int alchItemID;
    public static String itemName;
    public static int stackSize;
    public static boolean hasReqs;
    public static UIResult<Rectangle> itemRect;

    public static boolean multipleItemsMode = false;
    public static List<Integer> itemsToAlch;
    public static int currentItemIndex = 0;

    private List<Task> tasks;

    public dPublicAlcher(Object scriptCore) {
        super(scriptCore);
    }

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
    public void onStart() {
        log("INFO", "Starting dPublic Alcher v1.4");

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Alcher Options", false);

        spellToCast = ui.getSelectedSpell();

        if (ui.isMultipleSelectionMode()) {
            multipleItemsMode = true;
            itemsToAlch = ui.getMultipleSelectedItemIds();
            if (itemsToAlch.isEmpty()) {
                log("ERROR", "Multiple item mode selected, but no items found to alch. Stopping.");
                stop();
                return;
            }
            alchItemID = itemsToAlch.get(0);
        } else {
            multipleItemsMode = false;
            alchItemID = ui.getSelectedItemId();
        }

        itemName = getItemManager().getItemName(alchItemID);

        log("DEBUG", "Mode: " + (multipleItemsMode ? "Multiple" : "Single"));
        log("DEBUG", "Starting with item: " + itemName + " (ID=" + alchItemID + ") using: " + spellToCast);

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
