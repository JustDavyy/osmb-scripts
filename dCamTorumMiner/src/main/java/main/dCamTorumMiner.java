package main;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.DropTask;
import tasks.MineTask;
import tasks.Setup;
import tasks.SmithTask;
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
        name = "dCamTorumMiner",
        description = "Mines blessed bone shards in the Cam Torum mine",
        skillCategory = SkillCategory.MINING,
        version = 1.0,
        author = "JustDavyy"
)
public class dCamTorumMiner extends Script {
    public static final String scriptVersion = "1.0";
    public static boolean setupDone = false;
    public static boolean hasReqs;
    public static int blessedShardCount = 0;
    public static int miningXpGained = 0;
    public static boolean dropMode = false;
    public static boolean smithMode = false;
    public static final Area miningArea = new RectangleArea(1498, 9541, 3, 3, 1);

    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();

    private List<Task> tasks;
    private static final Font ARIEL = Font.getFont("Arial");

    // Webhook
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    public dCamTorumMiner(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{6037, 5781};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dCamTorumMiner v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Cam Torum Options", false);

        smithMode = ui.getDepositAction().equals("Smith");
        dropMode = ui.getDepositAction().equals("Drop");

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();
        webhookShowStats = ui.isStatsIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            lastWebhookSent = System.currentTimeMillis();
            sendWebhook();
        }

        checkForUpdates();

        tasks = Arrays.asList(
                new Setup(this),
                new DropTask(this),
                new SmithTask(this),
                new MineTask(this)
        );
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            sendWebhook();
            lastWebhookSent = System.currentTimeMillis();
        }

        for (Task task : tasks) {
            if (task.activate()) {
                task.execute();
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        int shardsPerHour = (int) ((blessedShardCount * 3600000L) / elapsed);
        int miningXpHr = (int) ((miningXpGained * 3600000L) / elapsed);
        double prayerXp = blessedShardCount * 5.5;
        int prayerXpHr = (int) ((prayerXp * 3600000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 270, 180, Color.BLACK.getRGB(), 1);
        c.drawRect(5, y, 270, 180, Color.BLACK.getRGB());
        c.drawText("Bone shards gained: " + f.format(blessedShardCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Bone shards/hr: " + f.format(shardsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Mining XP gained: " + f.format(miningXpGained), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Mining XP/hr: " + f.format(miningXpHr), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Prayer XP gained: " + f.format(prayerXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Prayer XP/hr: " + f.format(prayerXpHr), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
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
            int shardsPerHour = (int) ((blessedShardCount * 3600000L) / elapsed);
            int miningXpHr = (int) ((miningXpGained * 3600000L) / elapsed);
            double prayerXp = blessedShardCount * 5.5;
            int prayerXpHr = (int) ((prayerXp * 3600000L) / elapsed);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dCamTorumMiner Stats - ").append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"Shards Collected\",\"value\":\"").append(f.format(blessedShardCount)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Shards/hr\",\"value\":\"").append(f.format(shardsPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Mining XP\",\"value\":\"").append(f.format(miningXpGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Mining XP/hr\",\"value\":\"").append(f.format(miningXpHr)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Prayer XP\",\"value\":\"").append(f.format(prayerXp)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Prayer XP/hr\",\"value\":\"").append(f.format(prayerXpHr)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
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

    private String escapeJson(String text) {
        return text == null ? "null" : text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatRuntime(long ms) {
        long s = ms / 1000;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCamTorumMiner/src/main/java/main/dCamTorumMiner.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                if (!dir.exists()) dir.mkdirs();
                File[] old = dir.listFiles((d, n) -> n.equals("dCamTorumMiner.jar") || n.startsWith("dCamTorumMiner-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dCamTorumMiner-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCamTorumMiner/jar/dCamTorumMiner.jar");
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
        } catch (Exception ignored) {
        }
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
}
