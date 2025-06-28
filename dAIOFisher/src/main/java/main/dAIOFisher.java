package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.SearchableImage;
import data.FishingLocation;
import data.FishingMethod;
import data.HandlingMode;
import javafx.scene.Scene;
import tasks.*;
import utils.Task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;

import javax.imageio.ImageIO;

@ScriptDefinition(
        name = "dAIOFisher",
        description = "AIO Fisher that fishes, banks and/or drops to get those gains!",
        skillCategory = SkillCategory.FISHING,
        version = 1.91,
        author = "JustDavyy"
)
public class dAIOFisher extends Script {
    public static String scriptVersion = "1.91";
    public static boolean setupDone = false;
    public static boolean usingBarrel = false;
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    public static String task = "N/A";
    public static final Stopwatch switchTabTimer = new Stopwatch();

    public static boolean bankMode = false;
    public static boolean dropMode = false;
    public static boolean cookMode = false;
    public static boolean noteMode = false;
    public static FishingMethod fishingMethod;
    public static FishingLocation fishingLocation;
    public static HandlingMode handlingMode;
    public static String menuHook;
    public static boolean readyToReadFishingXP = false;
    public static boolean readyToReadCookingXP = false;
    public static boolean alreadyCountedFish = false;

    public static double previousFishingXpRead = -1;
    public static double previousCookingXpRead = -1;

    public static double fishingXp = 0;
    public static double cookingXp = 0;

    public static long lastXpGained = System.currentTimeMillis() - 20000;

    // Trackers
    public static int fish1Caught = 0;
    public static int fish2Caught = 0;
    public static int fish3Caught = 0;
    public static int fish4Caught = 0;
    public static int fish5Caught = 0;
    public static int fish6Caught = 0;
    public static int fish7Caught = 0;
    public static int fish8Caught = 0;
    public static int startAmount = 0;

    private static final Stopwatch webhookTimer = new Stopwatch();
    private static String webhookUrl = "";
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static int webhookIntervalMinutes = 5;
    private static String user = "";

    // Karambwan merged stuff
    public static final PolyArea fishingArea = new PolyArea(List.of(new WorldPosition(2896, 3119, 0),new WorldPosition(2894, 3118, 0),new WorldPosition(2893, 3116, 0),new WorldPosition(2894, 3115, 0),new WorldPosition(2895, 3114, 0),new WorldPosition(2895, 3113, 0),new WorldPosition(2895, 3112, 0),new WorldPosition(2895, 3110, 0),new WorldPosition(2897, 3109, 0),new WorldPosition(2898, 3108, 0),new WorldPosition(2899, 3107, 0),new WorldPosition(2900, 3106, 0),new WorldPosition(2909, 3106, 0),new WorldPosition(2912, 3108, 0),new WorldPosition(2916, 3111, 0),new WorldPosition(2914, 3115, 0),new WorldPosition(2914, 3116, 0),new WorldPosition(2913, 3117, 0),new WorldPosition(2913, 3118, 0),new WorldPosition(2911, 3118, 0),new WorldPosition(2910, 3117, 0),new WorldPosition(2909, 3116, 0),new WorldPosition(2908, 3115, 0),new WorldPosition(2907, 3115, 0),new WorldPosition(2906, 3116, 0),new WorldPosition(2905, 3117, 0),new WorldPosition(2904, 3118, 0),new WorldPosition(2903, 3119, 0),new WorldPosition(2901, 3119, 0),new WorldPosition(2900, 3119, 0),new WorldPosition(2899, 3118, 0),new WorldPosition(2897, 3119, 0),new WorldPosition(2898, 3118, 0)));
    public static int equippedCloakId = -1;
    public static int teleportCapeId = -1;
    public static String bankOption;
    public static String fairyOption;
    public static boolean doneBanking = false;
    public static double totalXpGained = 0.0;
    public static WorldPosition currentPos;

    // Minnows stuff
    public static SearchableImage minnowTileImageTop;
    public static SearchableImage minnowTileImageBottom;

    private List<Task> tasks;

    public dAIOFisher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        if (fishingLocation == null) {
            return new int[0];
        }

        return switch (fishingLocation) {
            case Karambwans -> new int[]{
                    11568, 9541, 11571, 10804, 10290, 10546
            };
            case Barb_Village -> new int[]{
                    12341, 12342
            };
            case Ottos_Grotto -> new int[]{
                    10038, 10039, 9782, 9783
            };
            case Mount_Quidamortem_CoX -> new int[]{
                    4919
            };
            case Karamja_West -> new int[]{
                    11055, 11054, 11311
            };
            case Shilo_Village -> new int[]{
                    11310
            };
            case Lumbridge_Goblins, Lumbridge_Swamp -> new int[]{
                    12850, 12849
            };
            case Mor_Ul_Rek_East, Mor_Ul_Rek_West -> new int[]{
                    10064, 10063, 9808, 9807
            };
            case Zul_Andra -> new int[]{
                    8751, 8752
            };
            case Port_Piscarilius_East, Port_Piscarilius_West -> new int[]{
                    6971, 7227, 7226, 6970
            };
            case Kingstown -> new int[]{
                    6713
            };
            case Farming_Guild -> new int[]{
                    4922, 4921
            };
            case Chaos_Druid_Tower -> new int[]{
                    10292, 10036, 10037
            };
            case Seers_SinclairMansion -> new int[]{
                    10807, 10806
            };
            // Fishing_Guild case left commented intentionally
            case Rellekka_MiddlePier, Rellekka_NorthPier, Rellekka_WestPier -> new int[]{
                    10553, 10554
            };
            case Jatizso -> new int[]{
                    9531
            };
            case Lands_End_East, Lands_End_West -> new int[]{
                    5941, 6197
            };
            case Isle_Of_Souls_East, Isle_Of_Souls_North, Isle_Of_Souls_South -> new int[]{
                    8491, 9004, 9006
            };
            case Burgh_de_Rott -> new int[]{
                    13874, 13873
            };
            case Tree_Gnome_Village -> new int[]{
                    9777
            };
            case Piscatoris -> new int[]{
                    9273
            };
            case Prifddinas_North, Prifddinas_South_NorthSide, Prifddinas_South_SouthSide -> new int[]{
                    12896, 13152, 13150, 13149, 8757, 9010
            };
            case Corsair_Cove -> new int[]{
                    10028
            };
            case Myths_Guild -> new int[]{
                    9773
            };
            case Varlamore -> new int[]{
                    6193
            };
            case Entrana_East, Entrana_Middle -> new int[]{
                    11316, 11572
            };
            case Catherby -> new int[]{
                    11317, 11061
            };
            case Fishing_Guild_South, Fishing_Guild_North, Minnows -> new int[]{
                    10293
            };
            default -> new int[0];
        };
    }

    @Override
    public void onPaint(Canvas c) {
        c.fillRect(5, 40, 270, 275, Color.BLACK.getRGB(), 1);
        c.drawRect(5, 40, 270, 275, Color.BLACK.getRGB());

        long elapsed = System.currentTimeMillis() - startTime;
        int caughtCount = fish1Caught + fish2Caught + fish3Caught + fish4Caught + fish5Caught + fish6Caught + fish7Caught + fish8Caught;
        int caughtPerHour = elapsed > 0 ? (int) ((caughtCount * 3600000L) / elapsed) : 0;

        int fishingXpPerHour = elapsed > 0 ? (int) ((fishingXp * 3600000L) / elapsed) : 0;
        int cookingXpPerHour = elapsed > 0 ? (int) ((cookingXp * 3600000L) / elapsed) : 0;

        int cookingXpBanked = caughtCount * 190;

        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);

        int y = 40;
        c.drawText("Catches: " + formatter.format(caughtCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Catches/hr: " + formatter.format(caughtPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        if (fishingLocation.equals(FishingLocation.Minnows)) {
            c.drawText("Sharks: " + formatter.format(caughtCount / 40), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Sharks/hr: " + formatter.format(caughtPerHour / 40), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        }

        y += 10;
        c.drawText("Fishing XP: " + formatter.format(fishingXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Fishing XP/hr: " + formatter.format(fishingXpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);

        if (cookMode) {
            c.drawText("Cooking XP: " + formatter.format(cookingXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Cooking XP/hr: " + formatter.format(cookingXpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        }
        if (fishingLocation.equals(FishingLocation.Karambwans)) {
            y += 10;
            c.drawText("Cooking XP banked: " + formatter.format(cookingXpBanked), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Bank method: " + bankOption, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Travel method: " + fairyOption, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        }
        y += 10;

        c.drawText("Current task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Location: " + fishingLocation.name() + " (" + fishingMethod.getMenuEntry() + ")", 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Handling mode: " + handlingMode.name(), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Script version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dAIOFisher v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dAIOFisher Options", false);

        bankMode = ui.getSelectedHandlingMethod().equals(HandlingMode.BANK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnBANK);
        dropMode = ui.getSelectedHandlingMethod().equals(HandlingMode.DROP) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOK);
        cookMode = ui.getSelectedHandlingMethod().equals(HandlingMode.COOK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnBANK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnNOTE);
        noteMode = ui.getSelectedHandlingMethod().equals(HandlingMode.NOTE) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnNOTE);
        fishingMethod = ui.getSelectedMethod();
        fishingLocation = ui.getSelectedLocation();
        menuHook = fishingMethod.getMenuEntry();
        handlingMode = ui.getSelectedHandlingMethod();

        if (fishingLocation.equals(FishingLocation.Karambwans)) {
            KarambwanUI wambamUI = new KarambwanUI(this);
            Scene wambamScene = wambamUI.buildScene(this);
            getStageController().show(wambamScene, "Karambwan Options", false);

            bankOption = wambamUI.getSelectedBankingOption();
            fairyOption = wambamUI.getSelectedFairyRingOption();

            log(getClass(), "Bank option: " + bankOption + " | Fairy ring option: " + fairyOption);
        }

        if (fishingLocation.equals(FishingLocation.Minnows)) {
            SearchableImage[] itemImages = getItemManager().getItem(ItemID.MINNOW, true);
            minnowTileImageTop = itemImages[itemImages.length - 1];
            minnowTileImageBottom = new SearchableImage(minnowTileImageTop.copy(), minnowTileImageTop.getToleranceComparator(), minnowTileImageTop.getColorModel());
            makeHalfTransparent(minnowTileImageTop, true);
            makeHalfTransparent(minnowTileImageBottom, false);
        }

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        if (webhookEnabled) {
            log("WEBHOOK", "Webhook enabled: " + true + " | Interval: " + webhookIntervalMinutes + " min.");
            // Initialize the timer with the interval (in milliseconds)
            webhookTimer.reset(webhookIntervalMinutes * 60_000L);
            // Get username for webhook purposes
            user = getWidgetManager().getChatbox().getUsername();
            // See what to show in the webhook
            webhookShowUser = ui.isUsernameIncluded();
            webhookShowStats = ui.isStatsIncluded();
            sendWebhook();
        }

        checkForUpdates();

        List<Task> taskList = new ArrayList<>();

        if (fishingLocation == FishingLocation.Karambwans) {
            taskList.add(new dkTravel(this));
            taskList.add(new dkFish(this));
            taskList.add(new dkBank(this));
        } else if (fishingLocation == FishingLocation.Minnows) {
            taskList.add(new dmFish(this));
        } //else if (fishingLocation == FishingLocation.Wilderness_Resource_Area) {
         //   taskList.add(new dCrabs(this));
        //}
        else {
            taskList.add(new Setup(this));
            taskList.add(new Travel(this));
            taskList.add(new Fish(this));
            taskList.add(new Cook(this));
            taskList.add(new Drop(this));
            taskList.add(new Bank(this));
        }

        // Build a readable list of task class names
        List<String> taskNames = taskList.stream()
                .map(task -> task.getClass().getSimpleName())
                .collect(Collectors.toList());

        log(getClass(), "Loaded " + taskList.size() + " task(s) for location: " + fishingLocation +
                " -> " + String.join(", ", taskNames));

        tasks = taskList;
    }

    @Override
    public int poll() {
        if (webhookEnabled && webhookTimer.hasFinished()) {
            sendWebhook();
            webhookTimer.reset(webhookIntervalMinutes * 60_000L);
        }

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

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dAIOFisher/src/main/java/main/dAIOFisher.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                File[] old = dir.listFiles((d, n) -> n.equals("dAIOFisher.jar") || n.startsWith("dAIOFisher-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dAIOFisher-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dAIOFisher/jar/dAIOFisher.jar");
                try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }

                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();

            } catch (Exception e) {
                log("UPDATE", "❌ Error downloading new version: " + e.getMessage());
            }
        } else {
            log("SCRIPTVERSION", "✅ You are running the latest version (v" + scriptVersion + ").");
        }
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
        return 0;
    }

    private void sendWebhook() {
        ByteArrayOutputStream baos = null;

        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log("WEBHOOK", "⚠ Webhook URL is empty. Skipping send.");
                return;
            }

            com.osmb.api.visual.image.Image screenImage = getScreen().getImage();
            BufferedImage bufferedImage = screenImage.toBufferedImage();

            baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String boundary = "----WebhookBoundary" + System.currentTimeMillis();
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            long elapsed = System.currentTimeMillis() - startTime;
            int caughtCount = fish1Caught + fish2Caught + fish3Caught + fish4Caught + fish5Caught + fish6Caught + fish7Caught + fish8Caught;
            int caughtPerHour = elapsed > 0 ? (int) ((caughtCount * 3600000L) / elapsed) : 0;
            int fishingXpPerHour = elapsed > 0 ? (int) ((fishingXp * 3600000L) / elapsed) : 0;
            int cookingXpPerHour = elapsed > 0 ? (int) ((cookingXp * 3600000L) / elapsed) : 0;
            int cookingXpBanked = caughtCount * 190;
            String formattedRuntime = formatDuration(elapsed);

            DecimalFormat formatter = new DecimalFormat("#,###");
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            formatter.setDecimalFormatSymbols(symbols);

            String usernameDisplay = webhookShowUser && user != null && !user.isEmpty() ? escapeJson(user) : "anonymous";

            StringBuilder payloadBuilder = new StringBuilder();
            payloadBuilder.append("{")
                    .append("\"embeds\": [{")
                    .append("\"title\": \"\\uD83D\\uDCCA dAIOFisher Stats - ").append(usernameDisplay).append("\",")
                    .append("\"color\": 4620980,");

            if (webhookShowStats) {
                payloadBuilder.append("\"fields\": [")
                        .append("{\"name\": \"Catches\", \"value\": \"").append(formatter.format(caughtCount)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Catches/hr\", \"value\": \"").append(formatter.format(caughtPerHour)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Fishing XP\", \"value\": \"").append(formatter.format(fishingXp)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Fishing XP/hr\", \"value\": \"").append(formatter.format(fishingXpPerHour)).append("\", \"inline\": true},");

                if (cookMode) {
                    payloadBuilder.append("{\"name\": \"Cooking XP\", \"value\": \"").append(formatter.format(cookingXp)).append("\", \"inline\": true},")
                            .append("{\"name\": \"Cooking XP/hr\", \"value\": \"").append(formatter.format(cookingXpPerHour)).append("\", \"inline\": true},");
                }
                if (fishingLocation.equals(FishingLocation.Karambwans)) {
                    payloadBuilder.append("{\"name\": \"Cooking XP banked\", \"value\": \"").append(formatter.format(cookingXpBanked)).append("\", \"inline\": true},")
                            .append("{\"name\": \"Methods\", \"value\": \"Bank: ").append(escapeJson(bankOption))
                            .append("\\nTravel: ").append(escapeJson(fairyOption)).append("\", \"inline\": true},");
                }

                payloadBuilder.append("{\"name\": \"Current task\", \"value\": \"").append(escapeJson(task)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Location\", \"value\": \"").append(escapeJson(fishingLocation.name() + " (" + fishingMethod.getMenuEntry() + ")")).append("\", \"inline\": true},")
                        .append("{\"name\": \"Handling mode\", \"value\": \"").append(escapeJson(handlingMode.name())).append("\", \"inline\": true},")
                        .append("{\"name\": \"Script version\", \"value\": \"").append(escapeJson(scriptVersion)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Runtime\", \"value\": \"").append(formattedRuntime).append("\", \"inline\": true}")
                        .append("],");
            } else {
                payloadBuilder.append("\"description\": \"Current task: ").append(escapeJson(task)).append("\",");
            }

            payloadBuilder.append("\"image\": {\"url\": \"attachment://screen.png\"}")
                    .append("}]")
                    .append("}");

            String payloadJson = payloadBuilder.toString();

            try (OutputStream output = connection.getOutputStream()) {
                output.write(("--" + boundary + "\r\n").getBytes());
                output.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                output.write(payloadJson.getBytes(StandardCharsets.UTF_8));
                output.write("\r\n".getBytes());

                output.write(("--" + boundary + "\r\n").getBytes());
                output.write("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\n".getBytes());
                output.write("Content-Type: image/png\r\n\r\n".getBytes());
                output.write(imageBytes);
                output.write("\r\n".getBytes());

                output.write(("--" + boundary + "--\r\n").getBytes());
                output.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 204 || responseCode == 200) {
                log("WEBHOOK", "✅ Webhook with screenshot sent successfully.");
            } else {
                log("WEBHOOK", "⚠ Failed to send webhook. HTTP " + responseCode);
            }

        } catch (Exception e) {
            log("WEBHOOK", "❌ Error sending webhook: " + e.getMessage());
        } finally {
            try {
                if (baos != null) baos.close();
            } catch (IOException ignore) {}
        }
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%02dd %02d:%02d:%02d", days, hours, minutes, secs);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
    }

    private void makeHalfTransparent(SearchableImage image, boolean topHalf) {
        int startY = topHalf ? 0 : image.getHeight() / 2;
        int endY = topHalf ? image.getHeight() / 2 : image.getHeight();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = startY; y < endY; y++) {
                image.setRGB(x, y, ColorUtils.TRANSPARENT_PIXEL);
            }
        }
    }
}
