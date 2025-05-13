package main;

import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.FishingTask;
import tasks.BankingTask;
import tasks.TravelTask;
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

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;

@ScriptDefinition(
        name = "dKarambwanFisher",
        description = "Fishes and banks karambwans",
        skillCategory = SkillCategory.FISHING,
        version = 1.1,
        author = "JustDavyy"
)
public class dKarambwanFisher extends Script {
    public static String scriptVersion = "1.1";
    public static final PolyArea fishingArea = new PolyArea(List.of(new WorldPosition(2896, 3119, 0),new WorldPosition(2894, 3118, 0),new WorldPosition(2893, 3116, 0),new WorldPosition(2894, 3115, 0),new WorldPosition(2895, 3114, 0),new WorldPosition(2895, 3113, 0),new WorldPosition(2895, 3112, 0),new WorldPosition(2895, 3110, 0),new WorldPosition(2897, 3109, 0),new WorldPosition(2898, 3108, 0),new WorldPosition(2899, 3107, 0),new WorldPosition(2900, 3106, 0),new WorldPosition(2909, 3106, 0),new WorldPosition(2912, 3108, 0),new WorldPosition(2916, 3111, 0),new WorldPosition(2914, 3115, 0),new WorldPosition(2914, 3116, 0),new WorldPosition(2913, 3117, 0),new WorldPosition(2913, 3118, 0),new WorldPosition(2911, 3118, 0),new WorldPosition(2910, 3117, 0),new WorldPosition(2909, 3116, 0),new WorldPosition(2908, 3115, 0),new WorldPosition(2907, 3115, 0),new WorldPosition(2906, 3116, 0),new WorldPosition(2905, 3117, 0),new WorldPosition(2904, 3118, 0),new WorldPosition(2903, 3119, 0),new WorldPosition(2901, 3119, 0),new WorldPosition(2900, 3119, 0),new WorldPosition(2899, 3118, 0),new WorldPosition(2897, 3119, 0),new WorldPosition(2898, 3118, 0)));
    public static int equippedCloakId = -1;
    public static int teleportCapeId = -1;
    public static boolean setupDone = false;
    public static String bankOption;
    public static String fairyOption;
    public static boolean doneBanking = false;
    public static boolean usingBarrel = false;
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    public static int caughtCount = 0;
    public static double totalXpGained = 0.0;
    public static String task = "N/A";
    public static WorldPosition currentPos;
    public static final Stopwatch switchTabTimer = new Stopwatch();

    private List<Task> tasks;

    public dKarambwanFisher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                11568, // Karambwan fishing spot
                9541,  // Zanaris area
                11571, // Crafting Guild
                10804, // Legends Guild
                10290, // Monastery
                10546, // Tower of Life
        };
    }

    @Override
    public void onPaint(Canvas c) {
        c.fillRect(5, 40, 220, 190, Color.BLACK.getRGB(), 0.7);
        c.drawRect(5, 40, 220, 190, Color.BLACK.getRGB());

        long elapsed = System.currentTimeMillis() - startTime;
        int fishingXpGained = caughtCount * 50;
        int cookingXpBanked = caughtCount * 190;
        int caughtPerHour = elapsed > 0 ? (int) ((caughtCount * 3600000L) / elapsed) : 0;
        int xpPerHour = elapsed > 0 ? (int) ((fishingXpGained * 3600000L) / elapsed) : 0;

        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);

        int y = 40;
        c.drawText("Karambwans caught: " + formatter.format(caughtCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Karambwans/hr: " + formatter.format(caughtPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + formatter.format(fishingXpGained), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + formatter.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Cooking XP banked: " + formatter.format(cookingXpBanked), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Current task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Bank method: " + bankOption, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Travel method: " + fairyOption, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Script version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dKarambwanFisher v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Wambam Options", false);

        bankOption = ui.getSelectedBankingOption();
        fairyOption = ui.getSelectedFairyRingOption();

        log(getClass().getSimpleName(), "Bank option: " + bankOption + " | Fairy ring option: " + fairyOption);

        String version = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dKarambwanFisher/src/main/java/main/dKarambwanFisher.java");

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
                new TravelTask(this),
                new FishingTask(this),
                new BankingTask(this)
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
