package main;

// GENERAL JAVA IMPORTS

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dCooker",
        description = "Cooks a wide variety of fish and other items at cookable objects.",
        skillCategory = SkillCategory.COOKING,
        version = 1.1,
        author = "JustDavyy"
)
public class dCooker extends Script {
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final String[] COOKING_ACTIONS = {"cook"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };
    public static boolean setupDone = false;
    public static int cookingItemID;
    public static int cookedItemID;
    private List<Task> tasks;

    public dCooker(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent or limit global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12109, // Rogues den
                11061, // Catherby
                11317, // Catherby2
                6712,  // Hosidius Kitchen
                13105, // Al-Kharid
                9772,  // Myth's Guild
                12597, // Cook's Guild
                12588, // Ruins of Unkah
                13613, // Nardah
                14907, // Museum Camp
                9275,  // Neitiznot
                12895, // Priff NW
                13151, // Priff NE
                9541,  // Zanaris
                12084, // Falador East
        };
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dCooker v1.1");

        // Build and show UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Cooking Options", false);

        cookingItemID = ui.getSelectedItemId();
        cookedItemID = ui.getSelectedCookedItemId();

        log(getClass().getSimpleName(), "We're cooking " + getItemManager().getItemName(cookingItemID) + " during this run, enjoy!");

        // Build task list
        tasks = Arrays.asList(
                new BankTask(this),
                new Setup(this),
                new ProcessTask(this)
        );
    }

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
