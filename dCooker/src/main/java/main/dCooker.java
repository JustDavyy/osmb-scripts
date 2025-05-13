package main;

// GENERAL JAVA IMPORTS

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import data.CookingItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dCooker",
        description = "Cooks a wide variety of fish and other items at cookable objects.",
        skillCategory = SkillCategory.COOKING,
        version = 1.5,
        author = "JustDavyy"
)
public class dCooker extends Script {
    public static String scriptVersion = "1.5";
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final String[] COOKING_ACTIONS = {"cook"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };
    public static long startTime = System.currentTimeMillis();
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    public static String task = "N/A";
    public static int cookCount = 0;
    public static double totalXpGained = 0.0;
    public static boolean setupDone = false;
    public static int cookingItemID;
    public static int cookedItemID;
    public static String bankMethod;
    private List<Task> tasks;

    public static boolean isMultipleMode = false;
    public static ObservableList<Integer> selectedItemIDs = FXCollections.observableArrayList();
    public static int selectedItemIndex = 0;

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
    public void onPaint(Canvas c) {
        c.fillRect(5, 40, 220, 170, Color.BLACK.getRGB(), 0.85);
        c.drawRect(5, 40, 220, 170, Color.BLACK.getRGB());

        long elapsed = System.currentTimeMillis() - startTime;
        int cooksPerHour = (int) ((cookCount * 3600000L) / elapsed);
        int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);

        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);

        int y = 40;
        c.drawText("Food Cooked: " + formatter.format(cookCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Food/hr: " + formatter.format(cooksPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + formatter.format(totalXpGained), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + formatter.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);

        if (isMultipleMode) {
            int remaining = selectedItemIDs.size() - selectedItemIndex - 1;
            String nextFishName = "N/A";
            if (selectedItemIndex + 1 < selectedItemIDs.size()) {
                Integer nextId = selectedItemIDs.get(selectedItemIndex + 1);
                nextFishName = getItemManager().getItemName(nextId) + "(" + nextId + ")";
            }

            c.drawText("Fish types left: " + remaining, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Next fish: " + nextFishName, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        }

        c.drawText("Current task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Script version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dCooker v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Cooking Options", false);

        isMultipleMode = ui.isMultipleSelectionMode();
        selectedItemIDs = ui.getMultipleSelectedItemIds(); // ObservableList<Integer>
        selectedItemIndex = 0;

        if (isMultipleMode && !selectedItemIDs.isEmpty()) {
            cookingItemID = selectedItemIDs.get(0);
            assert CookingItem.fromRawItemId(cookingItemID) != null;
            cookedItemID = CookingItem.fromRawItemId(cookingItemID).getCookedItemId();
        } else {
            cookingItemID = ui.getSelectedItemId();
            cookedItemID = ui.getSelectedCookedItemId();
        }

        bankMethod = ui.getSelectedBankMethod();

        log(getClass().getSimpleName(), "We're cooking: " + getItemManager().getItemName(cookingItemID));
        log(getClass().getSimpleName(), "Banking method: " + bankMethod);

        String version = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCooker/src/main/java/main/dCooker.java");

        if (version == null) {
            log("SCRIPTVERSION", "⚠ Could not fetch the latest version from GitHub. Proceeding with local version: v" + scriptVersion);
        } else {
            int comparison = compareVersions(scriptVersion, version);
            if (comparison == 0) {
                log("SCRIPTVERSION", "✅ You are running the latest script version: v" + scriptVersion);
            } else if (comparison < 0) {
                for (int i = 0; i < 10; i++) {
                    log("VERSIONERROR (" + i + "/10)", "❌ You are NOT running the latest version of this script!\nYour version: v" + scriptVersion + "\nLatest version: v" + version);
                    submitTask(() -> false, random(750, 2000));
                }
            } else {
                log("SCRIPTVERSION", "✅ You are running a newer version (v" + scriptVersion + ") than the published one (v" + version + ").");
                log("SCRIPTVERSION", "🙏 Thank you for testing a development build — your time and feedback are appreciated!");
            }
        }

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

    // Script version checks
    public String getLatestVersion(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("version")) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String version = parts[1].replace(",", "").trim();
                            return version;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log("VERSIONCHECK", "Exception occurred while fetching version from GitHub.");
        }

        return null;
    }

    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0; // Equal
    }
}
