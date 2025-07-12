package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.Bank;
import tasks.Chop;
import tasks.Drop;
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
        name = "dFossilWCer",
        description = "Cuts and drops/banks Teak or Mahogany logs on Fossil Island",
        skillCategory = SkillCategory.WOODCUTTING,
        version = 1.2,
        author = "JustDavyy"
)
public class dFossilWCer extends Script {
    public static final String scriptVersion = "1.2";

    public static boolean dropMode = false;
    public static boolean bankMode = false;
    public static boolean useShortcut = true;
    public static boolean setupDone = false;
    public static boolean useLogBasket = false;
    public static boolean usedBasketAlready = false;
    public static int logsId = -1;
    public static int centerX = -1;
    public static int centerY = -1;
    public static int logsChopped = 0;
    public static double totalXPGained = 0;
    public static boolean readyToReadXP = false;
    public static double previousXPRead = -1;

    public static String task = "Initializing...";
    public static long startTime = System.currentTimeMillis();

    private List<Task> tasks;
    private static final Font ARIEL = Font.getFont("Arial", null);
    private static final Font ARIEL_BOLD = Font.getFont("Arial Bold", null);
    private static final Font ARIEL_ITALIC = Font.getFont("Arial Italic", null);

    // Webhook
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    public dFossilWCer(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{14907, 14651, 14652, 14908};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dFossilWCer v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Script Options", false);

        String mode = ui.getMode();
        bankMode = mode.equals("Bank");
        dropMode = mode.equals("Drop");
        logsId = ui.getSelectedTree();

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
                new Bank(this),
                new Drop(this),
                new Chop(this)
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
        double hours = elapsed / 3600000.0;

        int woodcuttingXpHr = (int) (totalXPGained / hours);
        int logsHr = (int) (logsChopped / hours);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int x = 5;
        int y = 40;
        int width = 280;
        int height = 200; // Increased height to fit new line cleanly
        int borderThickness = 2;

        // Draw outer white border as highlight
        c.fillRect(x - borderThickness, y - borderThickness, width + (borderThickness * 2), height + (borderThickness * 2), Color.WHITE.getRGB(), 1);

        // Draw inner black background within border
        int innerX = x;
        int innerY = y;
        int innerWidth = width;
        int innerHeight = height;
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.BLACK.getRGB(), 1);

        // Draw inner white border inside the outer border for clarity
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        // Draw gradient header bar inside the box without covering border
        int headerHeight = 25;
        for (int i = 0; i < headerHeight; i++) {
            int gradientColor = new Color(50 + (i * 4), 100 + (i * 3), 200, 255).getRGB();
            c.drawLine(innerX + 1, innerY + 1 + i, innerX + innerWidth - 2, innerY + 1 + i, gradientColor);
        }

        // Draw bottom white border under the header with same thickness as outer border
        int bottomBorderYStart = innerY + headerHeight + 1;
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(innerX + 1, bottomBorderYStart + i, innerX + innerWidth - 2, bottomBorderYStart + i, Color.WHITE.getRGB());
        }

        // Draw script title centered in header
        String title = "🌳 dFossilWCer 🌳";
        int approxCharWidth = 7; // Adjust if needed for perfect centering
        int titlePixelWidth = title.length() * approxCharWidth;
        int titleX = innerX + (innerWidth / 2) - (titlePixelWidth / 2);
        c.drawText(title, titleX, innerY + 18, Color.BLACK.getRGB(), ARIEL_BOLD);

        y = innerY + headerHeight + 5;

        // Draw colorful stat lines
        c.drawText("Logs chopped: " + f.format(logsChopped), innerX + 10, y += 20, new Color(144, 238, 144).getRGB(), ARIEL);
        c.drawText("Logs/hr: " + f.format(logsHr), innerX + 10, y += 20, new Color(255, 215, 0).getRGB(), ARIEL);
        c.drawText("Woodcutting XP gained: " + f.format(totalXPGained), innerX + 10, y += 20, new Color(173, 216, 230).getRGB(), ARIEL);
        c.drawText("Woodcutting XP/hr: " + f.format(woodcuttingXpHr), innerX + 10, y += 20, new Color(255, 182, 193).getRGB(), ARIEL);

        // Draw current task in bright cyan
        c.drawText("Task: " + task, innerX + 10, y += 25, new Color(0, 255, 255).getRGB(), ARIEL_BOLD);

        // Determine mode text with shortcut info if mode is Bank
        String modeText;
        if (bankMode) {
            modeText = "Bank" + (useShortcut ? " (shortcut)" : " (no shortcut)");
        } else if (dropMode) {
            modeText = "Drop";
        } else {
            modeText = "Unknown";
        }

        // Draw mode line
        c.drawText("Mode: " + modeText, innerX + 10, y += 20, new Color(255, 140, 0).getRGB(), ARIEL_BOLD);

        // Draw version info with darker grey
        c.drawText("Version: " + scriptVersion, innerX + 10, y += 20, new Color(180, 180, 180).getRGB(), ARIEL_ITALIC);
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            double hours = elapsed / 3600000.0;

            int woodcuttingXpHr = (int) (totalXPGained / hours);
            int logsHr = (int) (logsChopped / hours);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            String runtime = formatRuntime(elapsed);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dFossilWCer Stats - ").append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"Logs Chopped\",\"value\":\"").append(f.format(logsChopped)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Logs/hr\",\"value\":\"").append(f.format(logsHr)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Woodcutting XP\",\"value\":\"").append(f.format(totalXPGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Woodcutting XP/hr\",\"value\":\"").append(f.format(woodcuttingXpHr)).append("\",\"inline\":true},");

                json.append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dFossilWCer/src/main/java/main/dFossilWCer.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                File[] old = dir.listFiles((d, n) -> n.equals("dFossilWCer.jar") || n.startsWith("dFossilWCer-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dFossilWCer-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dFossilWCer/jar/dFossilWCer.jar");
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
