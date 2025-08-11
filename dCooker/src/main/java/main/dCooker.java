package main;

// GENERAL JAVA IMPORTS

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import data.CookingItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        version = 2.5,
        author = "JustDavyy"
)
public class dCooker extends Script {
    public static String scriptVersion = "2.5";
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
    private static final Font ARIEL_BOLD = Font.getFont("Arial Bold", null);
    private static final Font ARIEL_ITALIC = Font.getFont("Arial Italic", null);
    public static String task = "N/A";
    public static int cookCount = 0;
    public static int burnCount = 0;
    public static int totalCookCount = 0;
    public static double totalXpGained = 0.0;
    public static boolean setupDone = false;
    public static int cookingItemID;
    public static int cookedItemID;
    public static String bankMethod;
    private List<Task> tasks;

    // Webhook stuff
    private static final Stopwatch webhookTimer = new Stopwatch();
    private static String webhookUrl = "";
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static int webhookIntervalMinutes = 5;
    private static String user = "";

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
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = elapsed / 3600000.0;

        int totalCooked = cookCount + burnCount;
        int totalPerHour = (int) (totalCooked / hours);
        int xpPerHour = (int) (totalXpGained / hours);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        String rate = (totalCooked > 0)
                ? String.format("%d%%", (int) ((cookCount * 100.0) / totalCooked))
                : "N/A";

        int x = 5;
        int y = 40;
        int width = 300;
        int height = 205;
        int borderThickness = 2;

        // Draw outer white border
        c.fillRect(x - borderThickness, y - borderThickness, width + (borderThickness * 2), height + (borderThickness * 2), Color.WHITE.getRGB(), 1);

        // Inner black background
        c.fillRect(x, y, width, height, Color.BLACK.getRGB(), 1);

        // Inner white outline
        c.drawRect(x, y, width, height, Color.WHITE.getRGB());

        // Gradient header
        int headerHeight = 25;
        for (int i = 0; i < headerHeight; i++) {
            // Interpolate between purple (128, 0, 128) and red (220, 20, 60)
            int r = 128 + (int) ((220 - 128) * (i / (double) headerHeight));
            int g = 0 + (int) ((20 - 0) * (i / (double) headerHeight));
            int b = 128 - (int) ((128 - 60) * (i / (double) headerHeight));
            int gradientColor = new Color(r, g, b, 255).getRGB();
            c.drawLine(x + 1, y + 1 + i, x + width - 2, y + 1 + i, gradientColor);
        }

        // Header bottom white border
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(x + 1, y + headerHeight + i, x + width - 2, y + headerHeight + i, Color.WHITE.getRGB());
        }

        // Title centered
        String title = "🍳 dCooker 🍳";
        int approxCharWidth = 7;
        int titleWidth = title.length() * approxCharWidth;
        int titleX = x + (width / 2) - (titleWidth / 2);
        c.drawText(title, titleX, y + 18, Color.BLACK.getRGB(), ARIEL_BOLD);

        y += headerHeight + 5;

        // Stat lines
        c.drawText("Cooked: " + f.format(cookCount), x + 10, y += 20, new Color(144, 238, 144).getRGB(), ARIEL);
        c.drawText("Burned: " + f.format(burnCount), x + 10, y += 20, new Color(255, 102, 102).getRGB(), ARIEL);
        c.drawText("Cook rate: " + rate, x + 10, y += 20, new Color(255, 255, 255).getRGB(), ARIEL);
        c.drawText("Total/hr: " + f.format(totalPerHour), x + 10, y += 20, new Color(255, 215, 0).getRGB(), ARIEL);
        c.drawText("XP gained: " + f.format(totalXpGained), x + 10, y += 20, new Color(173, 216, 230).getRGB(), ARIEL);
        c.drawText("XP/hr: " + f.format(xpPerHour), x + 10, y += 20, new Color(255, 182, 193).getRGB(), ARIEL);

        if (isMultipleMode) {
            int remaining = selectedItemIDs.size() - selectedItemIndex - 1;
            String nextFishName = "N/A";
            if (selectedItemIndex + 1 < selectedItemIDs.size()) {
                Integer nextId = selectedItemIDs.get(selectedItemIndex + 1);
                nextFishName = getItemManager().getItemName(nextId) + " (" + nextId + ")";
            }

            c.drawText("Next fish: " + nextFishName, x + 10, y += 25, new Color(0, 255, 255).getRGB(), ARIEL_BOLD);
            c.drawText("Types left: " + remaining, x + 10, y += 20, new Color(255, 140, 0).getRGB(), ARIEL_BOLD);
        }

        c.drawText("Task: " + task, x + 10, y += 25, new Color(255, 255, 255).getRGB(), ARIEL_BOLD);
        c.drawText("Version: " + scriptVersion, x + 10, y += 20, new Color(180, 180, 180).getRGB(), ARIEL_ITALIC);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dCooker v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Cooking Options", false);

        // Load webhook options
        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();
        webhookShowStats = ui.isStatsIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            webhookTimer.reset(webhookIntervalMinutes * 60_000L);
            sendWebhook();
        }

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
                log("VERSIONERROR", "❌ You are NOT running the latest version of this script!\nYour version: v" + scriptVersion + "\nLatest version: v" + version);

                try {
                    String userHome = System.getProperty("user.home");
                    File scriptsDir = new File(userHome + File.separator + ".osmb" + File.separator + "Scripts");

                    // Delete old versions
                    File[] oldFiles = scriptsDir.listFiles((dir, name) ->
                            name.equals("dCooker.jar") || name.matches("dCooker-\\d+(\\.\\d+)+\\.jar"));
                    if (oldFiles != null) {
                        for (File f : oldFiles) f.delete();
                    }

                    String downloadUrl = "https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCooker/jar/dCooker.jar";
                    File newFile = new File(scriptsDir, "dCooker-" + version + ".jar");

                    try (InputStream in = new URL(downloadUrl).openStream();
                         FileOutputStream out = new FileOutputStream(newFile)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    }

                    log("SCRIPTDOWNLOAD", "✅ Downloaded new version to: " + newFile.getAbsolutePath());
                    stop();
                    return;

                } catch (Exception e) {
                    log("SCRIPTDOWNLOAD", "❌ Failed to update script: " + e.getMessage());
                }

            } else {
                log("SCRIPTVERSION", "✅ You are running a newer version than GitHub: v" + scriptVersion);
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

    private void sendWebhook() {
        ByteArrayOutputStream baos = null;

        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) return;

            com.osmb.api.visual.image.Image screenImage = getScreen().getImage();
            BufferedImage bufferedImage = screenImage.toBufferedImage();

            baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String boundary = "----WebhookBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            long elapsed = System.currentTimeMillis() - startTime;
            int totalCooked = cookCount + burnCount;
            int cooksPerHour = (int) ((totalCooked * 3600000L) / elapsed);
            int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);
            String formattedRuntime = formatDuration(elapsed);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(); symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#,###"); formatter.setDecimalFormatSymbols(symbols);

            StringBuilder payload = new StringBuilder();
            payload.append("{\"embeds\": [{");
            payload.append("\"title\": \"\\uD83D\\uDCCA dCooker Stats - ")
                    .append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",");
            payload.append("\"color\": 4620980,");

            if (webhookShowStats) {

                String rate = (totalCooked > 0)
                        ? (int) ((cookCount * 100.0) / totalCooked) + "%"
                        : "N/A";

                payload.append("\"fields\": [")
                        .append("{\"name\": \"Food Cooked\", \"value\": \"C: ").append(formatter.format(cookCount))
                        .append("  B: ").append(formatter.format(burnCount))
                        .append(" (").append(rate).append(")\", \"inline\": true},")
                        .append("{\"name\": \"Cooked/hr\", \"value\": \"").append(formatter.format(cooksPerHour)).append("\", \"inline\": true},")
                        .append("{\"name\": \"XP Gained\", \"value\": \"").append(formatter.format(totalXpGained)).append("\", \"inline\": true},")
                        .append("{\"name\": \"XP/hr\", \"value\": \"").append(formatter.format(xpPerHour)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Task\", \"value\": \"").append(escapeJson(task)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Bank Method\", \"value\": \"").append(escapeJson(bankMethod)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Runtime\", \"value\": \"").append(formattedRuntime).append("\", \"inline\": true},")
                        .append("{\"name\": \"Script version\", \"value\": \"").append(scriptVersion).append("\", \"inline\": true}")
                        .append("],");
            } else {
                payload.append("\"description\": \"Task: ").append(escapeJson(task)).append("\",");
            }

            payload.append("\"image\": {\"url\": \"attachment://screen.png\"}}]}");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\n".getBytes());
                out.write("Content-Type: image/png\r\n\r\n".getBytes());
                out.write(imageBytes);
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "--\r\n").getBytes());
                out.flush();
            }

            int response = conn.getResponseCode();
            log("WEBHOOK", response == 204 || response == 200 ? "✅ Sent webhook" : "⚠ Failed with HTTP " + response);

        } catch (Exception e) {
            log("WEBHOOK", "❌ Failed to send webhook: " + e.getMessage());
        } finally {
            try { if (baos != null) baos.close(); } catch (IOException ignored) {}
        }
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return d > 0 ? String.format("%02d:%02d:%02d:%02d", d, h, m, s) : String.format("%02d:%02d:%02d", h, m, s);
    }
}
