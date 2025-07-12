package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.Setup;
import tasks.Sawmiller;
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
import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(
        name = "dSawmillRunner",
        description = "Creates planks of your choice at multiple sawmills",
        skillCategory = SkillCategory.CONSTRUCTION,
        version = 1.1,
        author = "JustDavyy"
)
public class dSawmillRunner extends Script {
    public static final String scriptVersion = "1.1";
    public static boolean setupDone = false;

    public static boolean useVouchers = false;
    public static boolean useRingOfElements = false;
    public static int neededLogs;
    public static int selectedPlank;
    public static String location = "N/A";
    public static int plankCount = 0;
    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();
    private static final Font ARIEL = Font.getFont("Ariel");

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    private List<Task> tasks;
    private ScriptUI ui;

    public dSawmillRunner(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{13151, 12895, 13150, 12853, 13109, 13110, 6198, 6454};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dSawmillRunner v" + scriptVersion);
        checkForUpdates();

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Sawmill Runner Options", false);

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

        selectedPlank = ui.getSelectedPlank();
        location = ui.getSelectedLocation().name();
        neededLogs = ui.getLogs();
        useRingOfElements = ui.isRingOfElementsEnabled();

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));
        tasks.add(new Sawmiller(this));
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            sendWebhook();
            lastWebhookSent = System.currentTimeMillis();
        }

        if (tasks != null) {
            for (Task taskObj : tasks) {
                if (taskObj.activate()) {
                    taskObj.execute();
                    return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        int planksPerHour = (int) ((plankCount * 3600000L) / elapsed);

        double regularXp = plankCount * getRegularXpPerPlank();
        double mhXp = plankCount * getMhXpPerPlank();
        double mhXpOutfit = plankCount * getMhXpPerPlankWithOutfit();

        // Formatter: full integers with dots as grouping separator
        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 250, 220, Color.BLACK.getRGB(), 1);
        c.drawRect(5, y, 250, 220, Color.BLACK.getRGB());

        c.drawText("Planks made: " + f.format(plankCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Planks/hr: " + f.format(planksPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        y += 10;

        c.drawText("Regular XP banked: " + f.format((int) regularXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("MH XP banked: " + f.format((int) mhXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("MH with outfit: " + f.format((int) mhXpOutfit), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        y += 10;

        c.drawText("Location: " + location, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    private double getRegularXpPerPlank() {
        return switch (selectedPlank) {
            case ItemID.PLANK -> 29;
            case ItemID.OAK_PLANK -> 60;
            case ItemID.TEAK_PLANK -> 90;
            case ItemID.MAHOGANY_PLANK -> 140;
            default -> 0;
        };
    }

    private double getMhXpPerPlank() {
        return switch (selectedPlank) {
            case ItemID.PLANK -> 93.7;
            case ItemID.OAK_PLANK -> 200.0;
            case ItemID.TEAK_PLANK -> 287.9;
            case ItemID.MAHOGANY_PLANK -> 346.1;
            default -> 0;
        };
    }

    private double getMhXpPerPlankWithOutfit() {
        return switch (selectedPlank) {
            case ItemID.PLANK -> 96.0;
            case ItemID.OAK_PLANK -> 205.0;
            case ItemID.TEAK_PLANK -> 295.1;
            case ItemID.MAHOGANY_PLANK -> 354.8;
            default -> 0;
        };
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);
            int planksPerHour = (int) ((plankCount * 3600000L) / elapsed);

            double regularXp = plankCount * getRegularXpPerPlank();
            double mhXp = plankCount * getMhXpPerPlank();
            double mhXpOutfit = plankCount * getMhXpPerPlankWithOutfit();

            DecimalFormat f = new DecimalFormat("#,###.0");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dSawmillRunner Stats - ")
                    .append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"Planks made\",\"value\":\"").append(f.format(plankCount)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Planks/hr\",\"value\":\"").append(f.format(planksPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Regular XP banked\",\"value\":\"").append(f.format(regularXp)).append("\",\"inline\":true},")
                        .append("{\"name\":\"MH XP banked\",\"value\":\"").append(f.format(mhXp)).append("\",\"inline\":true},")
                        .append("{\"name\":\"MH with outfit\",\"value\":\"").append(f.format(mhXpOutfit)).append("\",\"inline\":true},")
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

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dSawmillRunner/src/main/java/main/dSawmillRunner.java");
        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }
        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("dSawmillRunner.jar") || n.startsWith("dSawmillRunner-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dSawmillRunner-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dSawmillRunner/jar/dSawmillRunner.jar");

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
}
