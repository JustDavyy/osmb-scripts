package main;

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.trackers.experiencetracker.XPTracker;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.FirstBank;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;
import utils.XPTracking;

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
import java.util.Locale;
import java.util.function.Predicate;

@ScriptDefinition(
        name = "dCannonballSmelter",
        description = "Turns steel bars into cannonballs",
        skillCategory = SkillCategory.SMITHING,
        version = 2.6,
        author = "JustDavyy"
)
public class dCannonballSmelter extends Script {
    public static final String scriptVersion = "2.6";
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public static boolean setupDone = false;
    public static boolean bankSetupDone = false;
    public static int smeltCount = 0;
    public static double totalXpGained = 0.0;
    public static String task = "Initialize";

    public static double levelProgressFraction = 0.0;
    public static int currentLevel = 1;
    public static int startLevel = 1;

    public static long startTime = System.currentTimeMillis();

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    private final XPTracking xpTracking;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    private List<Task> tasks;

    public dCannonballSmelter(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12342, // Edgeville
                9275,  // Neitiznot
                11828, // Falador
                13150, // Prifdinnas
                11310, // Shilo
                5179,  // Mount Karuulm
                14646, // Port Phasmatys
                10064, // Mor Ul Rek
        };
    }

    @Override
    public void onNewFrame() {
        xpTracking.checkXP();
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dCannonballSmelter v" + scriptVersion);

        // Build and show UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Webhook Options", false);

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();
        webhookShowStats = ui.isStatsIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            lastWebhookSent = System.currentTimeMillis();
        }

        checkForUpdates();

        tasks = Arrays.asList(
                new Setup(this),
                new FirstBank(this),
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
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);

        int ballsSmelted  = smeltCount * 4;
        int smeltsPerHour = (int) Math.round(smeltCount / hours);
        int ballsPerHour  = (int) Math.round(ballsSmelted / hours);

        String ttlText = "-";
        double etl = 0;
        double xpGainedLive = totalXpGained;
        if (xpTracking != null) {
            XPTracker xpTracker = xpTracking.getXpTracker();
            if (xpTracker != null) {
                xpGainedLive = xpTracker.getXpGained();
                ttlText = xpTracker.timeToNextLevelString();
                etl = xpTracker.getXpForNextLevel();
                int curLevelXpStart = xpTracker.getExperienceForLevel(currentLevel);
                int nextLevelXpReq  = xpTracker.getExperienceForLevel(currentLevel + 1);
                int xpNeededThisLevel = Math.max(1, nextLevelXpReq - curLevelXpStart);
                double remaining = Math.max(0, etl);
                levelProgressFraction = Math.max(0.0, Math.min(1.0, 1.0 - (remaining / xpNeededThisLevel)));
            }
        }
        int xpPerHour = (int) Math.round(xpGainedLive / hours);

        if (startLevel <= 0) startLevel = currentLevel;
        int levelsGained = Math.max(0, currentLevel - startLevel);
        String currentLevelText = (levelsGained > 0)
                ? (currentLevel + " (+" + levelsGained + ")")
                : String.valueOf(currentLevel);

        double pct = Math.max(0, Math.min(100, levelProgressFraction * 100.0));
        String levelProgressText;
        double rounded = Math.rint(pct);

        if (Math.abs(pct - rounded) < 1e-9) {
            levelProgressText = String.format(Locale.US, "%.0f%%", pct);
        } else {
            levelProgressText = String.format(Locale.US, "%.1f%%", pct);
        }

        DecimalFormat intFmt = new DecimalFormat("#,###");
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        intFmt.setDecimalFormatSymbols(sym);

        final int x = 5;
        final int baseY = 40;
        final int width = 225;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int smallGap = 6;
        final int logoBottomGap = 8;
        final int maxLogoWidth  = width - paddingX * 2;
        final int maxLogoHeight = 48;

        final int labelGray  = new Color(180,180,180).getRGB();
        final int valueWhite = Color.WHITE.getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB(); // level progress
        final int valueBlue  = new Color(70, 130, 180).getRGB(); // cballs + cballs/hr

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = null;
        if (logoImage != null) {
            double kW = logoImage.width  > maxLogoWidth  ? maxLogoWidth  / (double) logoImage.width  : 1.0;
            double kH = logoImage.height > maxLogoHeight ? maxLogoHeight / (double) logoImage.height : 1.0;
            double k  = Math.min(kW, kH);
            if (k < 1.0) {
                int targetW = Math.max(1, (int)Math.round(logoImage.width * k));
                scaledLogo = scaleImageToWidth(logoImage, targetW);
            } else {
                scaledLogo = logoImage;
            }
        }

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int totalLines = 11;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap;
        y += smallGap;
        y += 10;

        int innerHeight = Math.max(200, y - innerY);

        c.fillRect(innerX - borderThickness, innerY - borderThickness,
                innerWidth + (borderThickness * 2),
                innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.BLACK.getRGB(), 1);
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        int curY = innerY + topGap;

        if (scaledLogo != null) {
            int imgX = innerX + (innerWidth - scaledLogo.width) / 2;
            c.drawAtOn(scaledLogo, imgX, curY);
            curY += scaledLogo.height + logoBottomGap;
        }

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Cballs smelted", intFmt.format(ballsSmelted), labelGray, valueBlue,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Smelts/hr", intFmt.format(smeltsPerHour), labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Cballs/hr", intFmt.format(ballsPerHour), labelGray, valueBlue,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "XP gained", intFmt.format(Math.round(xpGainedLive)), labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "XP/hr", intFmt.format(xpPerHour), labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "ETL", intFmt.format(Math.round(etl)), labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "TTL", ttlText, labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        // Level Progress (green, bold) — after TTL
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Level Progress", levelProgressText, labelGray, valueGreen,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Current level", currentLevelText, labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Task", String.valueOf(task), labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Version", scriptVersion, labelGray, valueWhite,
                FONT_LABEL, FONT_VALUE_BOLD);
    }

    private void drawStatLine(Canvas c, int innerX, int innerWidth, int paddingX, int y,
                              String label, String value, int labelColor, int valueColor,
                              Font labelFont, Font valueFont) {
        c.drawText(label, innerX + paddingX, y, labelColor, labelFont);
        int valW = c.getFontMetrics(valueFont).stringWidth(value);
        int valX = innerX + innerWidth - paddingX - valW;
        c.drawText(value, valX, y, valueColor, valueFont);
    }

    private void ensureLogoLoaded() {
        if (logoImage != null) return;

        try (InputStream in = getClass().getResourceAsStream("/logo.png")) {
            if (in == null) {
                log(getClass(), "Logo '/logo.png' not found on classpath.");
                return;
            }
            BufferedImage buf = ImageIO.read(in);
            if (buf == null) {
                log(getClass(), "Failed to decode logo.png");
                return;
            }
            // Convert BufferedImage -> API Image
            int w = buf.getWidth();
            int h = buf.getHeight();
            int[] argb = new int[w * h];
            buf.getRGB(0, 0, w, h, argb, 0, w);
            logoImage = new Image(argb, w, h);
        } catch (Exception e) {
            log(getClass(), "Error loading logo: " + e.getMessage());
        }
    }

    /** Optional: scale an API Image down to maxWidth (keeps an aspect). */
    private Image scaleImageToWidth(Image src, int maxWidth) {
        if (src == null || src.width <= maxWidth) return src;
        double scale = maxWidth / (double) src.width;
        int nw = Math.max(1, (int) Math.round(src.width * scale));
        int nh = Math.max(1, (int) Math.round(src.height * scale));
        int[] out = new int[nw * nh];

        // nearest-neighbor (fast and fine for logos)
        for (int y = 0; y < nh; y++) {
            int sy = (int) Math.floor(y / scale);
            int rowDst = y * nw;
            int rowSrc = sy * src.width;
            for (int x = 0; x < nw; x++) {
                int sx = (int) Math.floor(x / scale);
                out[rowDst + x] = src.pixels[rowSrc + sx];
            }
        }
        return new Image(out, nw, nh);
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
            double hours = Math.max(1e-9, elapsed / 3_600_000.0);

            int ballsSmelted  = smeltCount * 4;
            int smeltsPerHour = (int) Math.round(smeltCount / hours);

            int ballsPerHour  = (int) Math.round(ballsSmelted / hours);

            double xpGainedLive = totalXpGained;
            String ttlText = "-";
            double etl = 0;
            if (xpTracking != null) {
                XPTracker xpTracker = xpTracking.getXpTracker();
                if (xpTracker != null) {
                    xpGainedLive = xpTracker.getXpGained();
                    ttlText = xpTracker.timeToNextLevelString();
                    etl = xpTracker.getXpForNextLevel();
                }
            }
            int xpPerHour = (int) Math.round(xpGainedLive / hours);

            String runtime = formatRuntime(elapsed);

            // current level with (+N) if leveled
            if (startLevel <= 0) startLevel = currentLevel;
            int levelsGained = Math.max(0, currentLevel - startLevel);
            String currentLevelText = (levelsGained > 0)
                    ? (currentLevel + " (+" + levelsGained + ")")
                    : String.valueOf(currentLevel);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{ \"embeds\": [ {")
                    .append("\"title\": \"\\uD83D\\uDCCA dCannonballSmelter Stats - ")
                    .append(webhookShowUser && user != null ? user : "anonymous")
                    .append("\",")

                    .append("\"color\": 4620980,");

            if (webhookShowStats) {
                json.append("\"fields\": [")
                        .append("{\"name\":\"Cballs smelted\",\"value\":\"").append(f.format(ballsSmelted)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Smelts/hr\",\"value\":\"").append(f.format(smeltsPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Cballs/hr\",\"value\":\"").append(f.format(ballsPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP Gained\",\"value\":\"").append(f.format(Math.round(xpGainedLive))).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP/hr\",\"value\":\"").append(f.format(xpPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"ETL\",\"value\":\"").append(f.format(Math.round(etl))).append("\",\"inline\":true},")
                        .append("{\"name\":\"TTL\",\"value\":\"").append(ttlText).append("\",\"inline\":true},")
                        .append("{\"name\":\"Current level\",\"value\":\"").append(currentLevelText).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(task).append("\",\"inline\":true},")
                        .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                        .append("],");
            } else {
                json.append("\"description\": \"Currently on task: ").append(task).append("\",");
            }

            json.append("\"image\": { \"url\": \"attachment://screen.png\" } } ] }");

            String boundary = "----WebBoundary" + System.currentTimeMillis();
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
                out.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 204) {
                log("WEBHOOK", "✅ Webhook sent.");
            } else {
                log("WEBHOOK", "⚠ Webhook failed. HTTP " + code);
            }

        } catch (Exception e) {
            log("WEBHOOK", "❌ Error: " + e.getMessage());
        } finally {
            try {
                if (baos != null) baos.close();
            } catch (IOException ignored) {}
        }
    }

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCannonballSmelter/src/main/java/main/dCannonballSmelter.java");
        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " available. Updating...");

            try {
                File dir = new File(System.getProperty("user.home") + "/.osmb/Scripts");

                File[] oldFiles = dir.listFiles((d, name) -> name.startsWith("dCannonballSmelter"));
                if (oldFiles != null) {
                    for (File f : oldFiles) {
                        if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());
                    }
                }

                File newJar = new File(dir, "dCannonballSmelter-" + latest + ".jar");
                URL dl = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCannonballSmelter/jar/dCannonballSmelter.jar");

                try (InputStream in = dl.openStream(); FileOutputStream out = new FileOutputStream(newJar)) {
                    byte[] buf = new byte[4096];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }

                log("UPDATE", "✅ Downloaded new version: " + newJar.getName());
                stop();

            } catch (Exception e) {
                log("UPDATE", "❌ Failed to download new version: " + e.getMessage());
            }
        } else {
            log("VERSION", "✅ You are running the latest version (v" + scriptVersion + ")");
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
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.trim().startsWith("version")) {
                        return line.split("=")[1].replace(",", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int compareVersions(String v1, String v2) {
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

    private String formatRuntime(long ms) {
        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return (d > 0 ? String.format("%02d:", d) : "") + String.format("%02d:%02d:%02d", h, m, sec);
    }
}
