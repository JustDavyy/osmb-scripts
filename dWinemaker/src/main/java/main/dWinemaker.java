package main;

// GENERAL JAVA IMPORTS
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;


// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dWinemaker",
        description = "Turns your grapes into Jug of Wines or Wine of Zamorak for hefty cooking experience.",
        skillCategory = SkillCategory.COOKING,
        version = 1.0,
        author = "JustDavyy"
)

public class dWinemaker extends Script {
    public static boolean setupDone = false;
    public static boolean hasReqs;
    public static int grapeID;
    public static int wineID;
    public static boolean shouldBank = false;

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        // if object has no name
        if (gameObject.getName() == null) {
            return false;
        }
        // has no interact options (eg. bank, open etc.)
        if (gameObject.getActions() == null) {
            return false;
        }

        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
            return false;
        }

        // if no actions contain bank or open
        if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
            return false;
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };

    private List<Task> tasks;

    public dWinemaker(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12598
        };
    }

    @Override
    public void onStart(){
        log("INFO", "Starting dWinemaker v1.0");

        // Build and show our UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Winemaker Options", false);


        log(dWinemaker.class, "We're making " + getItemManager().getItemName(ui.getSelectedWine().getItemID()) + " during this run, enjoy!");

        if (ui.getSelectedWine().getItemID() == ItemID.WINE_OF_ZAMORAK) {
            grapeID = ItemID.ZAMORAKS_GRAPES;
            wineID = ItemID.WINE_OF_ZAMORAK;
        } else {
            grapeID = ItemID.GRAPES;
            wineID = ItemID.JUG_OF_WINE;
        }

        // Build our list of tasks, tasks will be trying to execute from top to bottom
        tasks = Arrays.asList(
                new Setup(this),
                new ProcessTask(this),
                new BankTask(this)
        );
    }

    // This triggers the user right after onStart to select a bank tab
    @Override
    public boolean promptBankTabDialogue() {
        return true;
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
