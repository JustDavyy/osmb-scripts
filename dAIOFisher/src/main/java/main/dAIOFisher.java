package main;

import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
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
import java.util.Arrays;
import java.util.List;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;

import javax.imageio.ImageIO;

@ScriptDefinition(
        name = "dAIOFisher",
        description = "AIO Fisher that fishes, banks and/or drops to get those gains!",
        skillCategory = SkillCategory.FISHING,
        version = 1.0,
        author = "JustDavyy"
)
public class dAIOFisher extends Script {
    public static String scriptVersion = "1.0";
    public static boolean setupDone = false;
    public static boolean usingBarrel = false;
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    public static String task = "N/A";
    public static final Stopwatch switchTabTimer = new Stopwatch();

    public static boolean bankMode = false;
    public static boolean dropMode = false;
    public static boolean cookMode = false;
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

    private static final Stopwatch webhookTimer = new Stopwatch();
    private static String webhookUrl = "";
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static int webhookIntervalMinutes = 5;
    private static String user = "";

    private List<Task> tasks;

    public dAIOFisher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                // Barb village
                12341,
                12342,

                // Ottos Grotto
                10038,
                10039,
                9782,
                9783,

                // Mount Quidamortem / CoX
                4919,
        };
    }

    @Override
    public void onPaint(Canvas c) {
        c.fillRect(5, 40, 270, 240, Color.BLACK.getRGB(), 1);
        c.drawRect(5, 40, 270, 240, Color.BLACK.getRGB());

        long elapsed = System.currentTimeMillis() - startTime;
        int caughtCount = fish1Caught + fish2Caught + fish3Caught + fish4Caught + fish5Caught + fish6Caught + fish7Caught + fish8Caught;
        int caughtPerHour = elapsed > 0 ? (int) ((caughtCount * 3600000L) / elapsed) : 0;

        int fishingXpPerHour = elapsed > 0 ? (int) ((fishingXp * 3600000L) / elapsed) : 0;
        int cookingXpPerHour = elapsed > 0 ? (int) ((cookingXp * 3600000L) / elapsed) : 0;

        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);

        int y = 40;
        c.drawText("Catches: " + formatter.format(caughtCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Catches/hr: " + formatter.format(caughtPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);

        y += 10;
        c.drawText("Fishing XP: " + formatter.format(fishingXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Fishing XP/hr: " + formatter.format(fishingXpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);

        if (cookMode) {
            c.drawText("Cooking XP: " + formatter.format(cookingXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Cooking XP/hr: " + formatter.format(cookingXpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
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
        cookMode = ui.getSelectedHandlingMethod().equals(HandlingMode.COOK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnBANK);
        fishingMethod = ui.getSelectedMethod();
        fishingLocation = ui.getSelectedLocation();
        menuHook = fishingMethod.getMenuEntry();
        handlingMode = ui.getSelectedHandlingMethod();

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

        tasks = Arrays.asList(
                new Setup(this),
                new Travel(this),
                new Fish(this),
                new Cook(this),
                new Drop(this),
                new Bank(this)
        );
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
}
