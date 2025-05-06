package main;

// GENERAL JAVA IMPORTS

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.FirstBank;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dCannonballSmelter",
        description = "Turns steel bars into cannonballs",
        skillCategory = SkillCategory.SMITHING,
        version = 1.3,
        author = "JustDavyy"
)
public class dCannonballSmelter extends Script {
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };
    public static boolean setupDone = false;
    public static boolean bankSetupDone = false;
    private List<Task> tasks;

    public dCannonballSmelter(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent or limit global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12342, // Edgeville
                9275,  // Neitiznot
                11828, // Falador
                13150, // Prifdinnas
                11310, // Shilo
                5179,  // Mount Karuulm
                14646, // Port Phasmatys
                10064, // Mor Ul Rek
        };
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dCannonballSmelter v1.3");

        // Build task list
        tasks = Arrays.asList(
                new Setup(this),
                new FirstBank(this),
                new BankTask(this),
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
