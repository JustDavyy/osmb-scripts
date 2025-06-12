package main;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import com.osmb.api.visual.drawing.Canvas;

import javafx.scene.Scene;
import tasks.AlchTask;
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

@ScriptDefinition(
        name = "dTemp Alcher",
        description = "Used as a substitute from dPublic Alcher to allow noted items to be alched by choosing a slot ID to alch instead.",
        skillCategory = SkillCategory.MAGIC,
        version = 1.7,
        author = "JustDavyy"
)
public class dTempAlcher extends Script {
    public static final String scriptVersion = "1.7";
    public static boolean setupDone = false;
    public static StandardSpellbook spellToCast;
    public static int alchSlotID;
    public static boolean hasReqs = true;

    public static boolean multipleSlotsMode = false;
    public static List<Integer> slotsToAlch;
    public static int currentSlotIndex = 0;
    public static int alchCount = 0;
    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    private static final Font ARIEL = Font.getFont("Ariel");

    private List<Task> tasks;

    public dTempAlcher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12598, 6461, 7222, 12633};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dTemp Alcher v" + scriptVersion);
        checkForUpdates();

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Alcher Options", false);

        spellToCast = ui.getSelectedSpell();
        if (ui.isMultipleSelectionMode()) {
            multipleSlotsMode = true;
            slotsToAlch = ui.getMultipleSelectedSlotIds();
            if (slotsToAlch.isEmpty()) {
                log("ERROR", "Multiple slot mode selected but no slots found. Stopping.");
                stop();
                return;
            }
            alchSlotID = slotsToAlch.get(0);
        } else {
            multipleSlotsMode = false;
            alchSlotID = ui.getSelectedSlotId();
        }

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

        tasks = Arrays.asList(new Setup(this), new AlchTask(this));
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

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);
        int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
        int totalXp = alchCount * xpPerCast;
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 220, 140, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 220, 140, Color.BLACK.getRGB());

        c.drawText("Alchs done: " + f.format(alchCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Alchs/hr: " + f.format(alchsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + f.format(totalXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);

            int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);
            int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
            int totalXp = alchCount * xpPerCast;
            int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dTempAlcher Stats - ").append(webhookShowUser ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"Alchs Done\",\"value\":\"").append(f.format(alchCount)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Alchs/hr\",\"value\":\"").append(f.format(alchsPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP Gained\",\"value\":\"").append(f.format(totalXp)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP/hr\",\"value\":\"").append(f.format(xpPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                        .append("],");
            } else {
                json.append("\"description\":\"Currently on task: ").append(escapeJson(task)).append("\",");
            }

            json.append("\"image\":{\"url\":\"attachment://screen.png\"}}]}");

            String boundary = "----Boundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
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
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 204) {
                log("WEBHOOK", "✅ Sent webhook successfully.");
            } else {
                log("WEBHOOK", "⚠ Failed to send webhook: HTTP " + code);
            }

        } catch (Exception e) {
            log("WEBHOOK", "❌ Error sending webhook: " + e.getMessage());
        }
    }

    private void checkForUpdates() {
        try {
            String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dTempAlcher/src/main/java/main/dTempAlcher.java");
            if (latest == null) {
                log("VERSION", "⚠ Could not fetch latest version info.");
                return;
            }

            if (compareVersions(scriptVersion, latest) < 0) {
                log("VERSION", "⏬ New version v" + latest + " found! Updating...");

                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("dTempAlcher.jar") || n.startsWith("dTempAlcher-"));
                if (old != null) for (File f : old) f.delete();

                File out = new File(dir, "dTempAlcher-" + latest + ".jar");
                try (InputStream in = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dTempAlcher/jar/dTempAlcher.jar").openStream();
                     FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }

                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();
            } else {
                log("SCRIPTVERSION", "✅ You are running the latest version.");
            }
        } catch (Exception e) {
            log("UPDATE", "❌ Auto-update failed: " + e.getMessage());
        }
    }

    private String getLatestVersion(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        if (conn.getResponseCode() != 200) return null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                if (line.contains("version")) return line.split("=")[1].replace(",", "").trim();
        }
        return null;
    }

    public static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private String escapeJson(String text) {
        if (text == null) return "unknown";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatRuntime(long ms) {
        long s = ms / 1000;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }
}