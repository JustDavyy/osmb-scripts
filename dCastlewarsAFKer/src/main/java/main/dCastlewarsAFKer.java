package main;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.Setup;
import tasks.CwarsSlave;
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
        name = "dCastlewarsAFKer",
        description = "AFKs the castle wars minigame on mass worlds",
        skillCategory = SkillCategory.OTHER,
        version = 1.1,
        author = "JustDavyy"
)
public class dCastlewarsAFKer extends Script {
    public static final String scriptVersion = "1.1";
    public static boolean setupDone = false;
    public static int tickets = 0;
    public static int plaudits = 0;
    public static int ticketsGained = 0;
    public static int plauditsGained = 0;
    public static boolean canHopNow = false;
    public static boolean canBreakNow = false;

    public static String location = "N/A";
    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();
    private static final Font ARIEL = Font.getFont("Arial", null);
    private static final Font ARIEL_BOLD = Font.getFont("Arial Bold", null);
    private static final Font ARIEL_ITALIC = Font.getFont("Arial Italic", null);

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    // Outside
    public static final Area castleWarsArea = new RectangleArea(2435, 3081, 11, 17, 0);

    public static final Stopwatch switchTabTimer = new Stopwatch();

    private List<Task> tasks;
    private ScriptUI ui;

    public dCastlewarsAFKer(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{9620, 9520, 9776};
    }

    @Override
    public boolean canBreak() {
        return canBreakNow;
    }

    @Override
    public boolean canHopWorlds() {
        return canHopNow;
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dCastlewarsAFKer v" + scriptVersion);
        checkForUpdates();

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Castle Wars AFK Options", false);

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

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));
        tasks.add(new CwarsSlave(this));
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
        double hours = Math.max(1e-9, elapsed / 3_600_000.0); // avoid div-by-zero

        int ticketsPerHour  = (int) Math.round(ticketsGained  / hours);
        int plauditsPerHour = (int) Math.round(plauditsGained / hours);

        // Formatter: integers with dots as grouping separator
        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int x = 5;
        int y = 40;
        int width = 300;
        int height = 250;
        int borderThickness = 2;

        // Outer white border highlight
        c.fillRect(x - borderThickness, y - borderThickness, width + (borderThickness * 2), height + (borderThickness * 2), Color.WHITE.getRGB(), 1);

        // Black background box
        int innerX = x;
        int innerY = y;
        int innerWidth = width;
        int innerHeight = height;
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.BLACK.getRGB(), 1);

        // White inner border
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        // Gradient header
        int headerHeight = 25;
        for (int i = 0; i < headerHeight; i++) {
            int gradientColor = new Color(80 + (i * 3), 150 + (i * 3), 255, 255).getRGB();
            c.drawLine(innerX + 1, innerY + 1 + i, innerX + innerWidth - 2, innerY + 1 + i, gradientColor);
        }

        // Header bottom border
        int bottomBorderY = innerY + headerHeight + 1;
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(innerX + 1, bottomBorderY + i, innerX + innerWidth - 2, bottomBorderY + i, Color.WHITE.getRGB());
        }

        // Script title
        String title = "dCastlewarsAFKer";
        int titlePixelWidth = title.length() * 7;
        int titleX = innerX + (innerWidth / 2) - (titlePixelWidth / 2);
        c.drawText(title, titleX, innerY + 18, Color.BLACK.getRGB(), ARIEL_BOLD);

        y = innerY + headerHeight + 5;

        // New stats
        c.drawText("Tickets gained: " + f.format(ticketsGained) + " (" + f.format(ticketsPerHour) + "/hr)", innerX + 10, y += 20, new Color(144, 238, 144).getRGB(), ARIEL);
        c.drawText("Plaudits gained: " + f.format(plauditsGained) + " (" + f.format(plauditsPerHour) + "/hr)", innerX + 10, y += 20, new Color(255, 215, 0).getRGB(), ARIEL);

        y += 5;

        c.drawText("Tickets total: " + f.format(tickets), innerX + 10, y += 20, new Color(173, 216, 230).getRGB(), ARIEL);
        c.drawText("Plaudits total: " + f.format(plaudits), innerX + 10, y += 20, new Color(255, 182, 193).getRGB(), ARIEL);

        y += 10;

        c.drawText("Can break: " + canBreakNow + "  Time to break: " + getProfileManager().isDueToBreak(), innerX + 10, y += 20, new Color(173, 216, 230).getRGB(), ARIEL);
        c.drawText("Can hop: " + canHopNow + "  Time to hop: " + getProfileManager().isDueToHop(), innerX + 10, y += 20, new Color(255, 182, 193).getRGB(), ARIEL);

        y += 10;

        c.drawText("Location: " + location, innerX + 10, y += 20, new Color(240, 128, 128).getRGB(), ARIEL);
        c.drawText("Task: " + task, innerX + 10, y += 25, new Color(0, 255, 255).getRGB(), ARIEL_BOLD);
        c.drawText("Version: " + scriptVersion, innerX + 10, y += 20, new Color(180, 180, 180).getRGB(), ARIEL_ITALIC);
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);
            double hours = Math.max(1e-9, elapsed / 3_600_000.0);

            int ticketsPerHour  = (int) Math.round(ticketsGained  / hours);
            int plauditsPerHour = (int) Math.round(plauditsGained / hours);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dCastlewarsAFKer - ")
                    .append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")

                    // Optional color accent
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"Tickets gained\",\"value\":\"").append(f.format(ticketsGained)).append(" (").append(f.format(ticketsPerHour)).append("/hr)\",\"inline\":true},")
                        .append("{\"name\":\"Plaudits gained\",\"value\":\"").append(f.format(plauditsGained)).append(" (").append(f.format(plauditsPerHour)).append("/hr)\",\"inline\":true},")
                        .append("{\"name\":\"Tickets total\",\"value\":\"").append(f.format(tickets)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Plaudits total\",\"value\":\"").append(f.format(plaudits)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Location\",\"value\":\"").append(escapeJson(location)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                        .append("],");
            } else {
                // Minimal description when stats hidden
                json.append("\"description\":\"")
                        .append("Location: ").append(escapeJson(location)).append("\\n")
                        .append("Task: ").append(escapeJson(task)).append("\\n")
                        .append("Runtime: ").append(runtime).append("\\n")
                        .append("Version: ").append(scriptVersion)
                        .append("\",");
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCastlewarsAFKer/src/main/java/main/dCastlewarsAFKer.java");
        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }
        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("dCastlewarsAFKer.jar") || n.startsWith("dCastlewarsAFKer-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dCastlewarsAFKer-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCastlewarsAFKer/jar/dCastlewarsAFKer.jar");

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
