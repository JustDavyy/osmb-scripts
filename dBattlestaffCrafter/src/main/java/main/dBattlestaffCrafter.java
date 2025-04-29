package main;

// GENERAL JAVA IMPORTS
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;


// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dBattlestaffCrafter",
        description = "Attaches orbs to battlestaves for quick crafting experience",
        skillCategory = SkillCategory.CRAFTING,
        version = 1.0,
        author = "JustDavyy"
)

public class dBattlestaffCrafter extends Script {
    public static boolean setupDone = false;
    public static boolean hasReqs;
    public static int staffID;

    public static boolean shouldBank = false;

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    private List<Task> tasks;

    public dBattlestaffCrafter(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent or limit global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                13104, // Shantay Pass
                13105, // Al Kharid
                13363, // Duel Arena / PvP Arena
                12850, // Lumbridge Castle
                12338, // Draynor
                12853, // Varrock East
                12597, // Varrock West + Cooks Guild
                12598, // Grand Exchange
                12342, // Edgeville
                12084, // Falador East + Mining GUild
                11828, // Falador West
                11571, // Crafting Guild
                11319, // Warriors Guild
                11061, // Catherby
                10806, // Seers
                11310, // Shilo
                10284, // Corsair Cove
                9772,  // Myths Guild
                10288, // Yanille
                10545, // Port Khazard
                10547, // Ardougne East/South
                10292, // Ardougne East/North
                10293, // Fishing Guild
                10039, // Barbarian Assault
                9782,  // Grand Tree
                9781,  // Tree Gnome Stronghold
                9776,  // Castle Wars
                9265,  // Lletya
                8748,  // Soul Wars
                8253,  // Lunar Isle
                9275,  // Neitiznot
                9531,  // Jatiszo
                6461,  // Wintertodt
                7227,  // Port Piscarilius
                6458,  // Arceeus
                6457,  // Kourend Castle
                6968,  // Hosidius
                7223,  // Vinery
                6710,  // Sand Crabs Chest
                6198,  // Woodcutting Guild
                5941,  // Land's End
                5944,  // Shayzien
                5946,  // Lovakengj South
                5691,  // Lovekengj North
                4922,  // Farming Guild
                4919,  // Chambers of Xeric
                5938,  // Quetzacalli
                6448,  // Varlamore West
                6960,  // Varlamore East
                6191,  // Hunter Guild
                5421,  // Aldarin
                5420,  // Mistrock
                14638, // Mos'le Harmless
                14642, // TOB
                14646, // Port Phasmatys
                12344, // Ferox Enclave
                12895, // Priff North
                13150, // Priff South
                13907, // Museum Camp
                14908, // Fossil Bank Chest island
        };
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dBattlestaffCrafter v1.0");

        // Build and show UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Battlestaff Crafter Options", false);

        staffID = ui.getSelectedStaffId();

        log(getClass().getSimpleName(), "We're making " + getItemManager().getItemName(staffID) + " during this run, enjoy!");

        // Build task list
        tasks = Arrays.asList(
                new Setup(this),
                new ProcessTask(this),
                new BankTask(this)
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
