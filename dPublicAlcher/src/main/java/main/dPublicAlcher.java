package main;

import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResult;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;
import tasks.AlchTask;
import tasks.Setup;
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
import com.osmb.api.ui.spellbook.StandardSpellbook;

import javax.imageio.ImageIO;

@ScriptDefinition(
        name = "dPublic Alcher",
        description = "Alchs items (both high & low) until out of items or runes.",
        skillCategory = SkillCategory.MAGIC,
        version = 1.6,
        author = "JustDavyy"
)
public class dPublicAlcher extends Script {
    public static final String scriptVersion = "1.6";
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

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";
    public static String task = "Initialize";

    private static final Font ARIEL = Font.getFont("Ariel");

    public static long startTime = System.currentTimeMillis();
    public static int alchCount = 0;

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
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);

        int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
        int totalXp = alchCount * xpPerCast;
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        long estimatedMillisLeft = (alchsPerHour > 0)
                ? (stackSize * 3600000L) / alchsPerHour
                : 0;

        long seconds = estimatedMillisLeft / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        String timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, secs);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 220, 170, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 220, 170, Color.BLACK.getRGB());

        c.drawText("Alchs done: " + f.format(alchCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Alchs/hr: " + f.format(alchsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + f.format(totalXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Items left: " + f.format(stackSize), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Finished in: " + timeLeftFormatted, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Runtime: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }


    @Override
    public void onStart() {
        log("INFO", "Starting dPublic Alcher v" + scriptVersion);

        checkForUpdates();

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

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();
        webhookShowStats = ui.isStatsIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            lastWebhookSent = System.currentTimeMillis();
            sendWebhook();
        }

        checkForUpdates();

        tasks = Arrays.asList(
                new Setup(this),
                new AlchTask(this)
        );
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            sendWebhook();
            lastWebhookSent = System.currentTimeMillis();
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

    private void sendWebhook() {
        ByteArrayOutputStream baos = null;
        try {
            Image screenImage = getScreen().getImage();
            BufferedImage buffered = screenImage.toBufferedImage();

            baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);

            int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);
            int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
            int totalXp = alchCount * xpPerCast;
            int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);
            int itemsLeft = stackSize;

            long estimatedMillisLeft = (alchsPerHour > 0)
                    ? (itemsLeft * 3600000L) / alchsPerHour
                    : 0;
            String eta = formatRuntime(estimatedMillisLeft);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{ \"embeds\": [ {")
                    .append("\"title\": \"\\uD83D\\uDCCA dPublicAlcher Stats - ")
                    .append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\": 15844367,");

            if (webhookShowStats) {
                json.append("\"fields\": [")
                        .append("{\"name\":\"Alchs Done\",\"value\":\"").append(f.format(alchCount)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Alchs/hr\",\"value\":\"").append(f.format(alchsPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP Gained\",\"value\":\"").append(f.format(totalXp)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP/hr\",\"value\":\"").append(f.format(xpPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Items Left\",\"value\":\"").append(f.format(itemsLeft)).append("\",\"inline\":true},")
                        .append("{\"name\":\"ETA\",\"value\":\"").append(eta).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                        .append("],");
            } else {
                json.append("\"description\": \"Currently on task: ").append(escapeJson(task)).append("\",");
            }

            json.append("\"image\": { \"url\": \"attachment://screen.png\" } } ] }");

            String boundary = "----WebBoundary" + System.currentTimeMillis();
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                out.write(json.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\n".getBytes());
                out.write("Content-Type: image/png\r\n\r\n".getBytes());
                out.write(imageBytes);
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "--\r\n").getBytes());
                out.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 204) {
                log("WEBHOOK", "✅ Sent webhook successfully.");
            } else {
                log("WEBHOOK", "⚠ Failed to send webhook: HTTP " + code);
            }

        } catch (Exception e) {
            log("WEBHOOK", "❌ Error: " + e.getMessage());
        } finally {
            try {
                if (baos != null) baos.close();
            } catch (IOException ignored) {}
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "unknown";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatRuntime(long millis) {
        long seconds = millis / 1000;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String getLatestVersion(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            if (c.getResponseCode() != 200) return null;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    if (l.trim().startsWith("version")) {
                        return l.split("=")[1].replace(",", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dPublicAlcher/src/main/java/main/dPublicAlcher.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");

            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                if (!dir.exists()) dir.mkdirs();

                File[] old = dir.listFiles((d, n) -> n.equals("dPublicAlcher.jar") || n.startsWith("dPublicAlcher-"));
                if (old != null) {
                    for (File f : old) {
                        if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());
                    }
                }

                File out = new File(dir, "dPublicAlcher-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dPublicAlcher/jar/dPublicAlcher.jar");

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
            log("SCRIPTVERSION", "✅ You are running a newer version (v" + scriptVersion + ") than the published one (v" + latest + ").");
            log("SCRIPTVERSION", "🙏 Thank you for testing a development build — your time and feedback are appreciated!");
        }
    }
}
