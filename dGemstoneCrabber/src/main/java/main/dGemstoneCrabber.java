package main;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.Bank;
import tasks.Setup;
import tasks.Fight;
import utils.Task;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(
        name = "dGemstoneCrabber",
        description = "Trains combat by hunting the gem stone crab",
        skillCategory = SkillCategory.COMBAT,
        version = 1.1,
        author = "JustDavyy"
)
public class dGemstoneCrabber extends Script implements WebhookSender {
    public static final String scriptVersion = "1.1";
    public static boolean setupDone = false;
    public static boolean canHopNow = false;
    public static boolean canBreakNow = false;
    public static boolean canBankNow = false;
    public static boolean needToBank = false;
    public static boolean foundCrab = false;
    public static boolean alreadyFought = false;
    public static boolean needToAttack = false;
    public static boolean onlyHopAfterKill = false;
    public static boolean shouldEat = false;
    public static boolean useFood = false;
    public static boolean usePot = false;
    public static boolean useDBAXE = false;
    public static boolean useHearts = false;
    public static int heartID;
    public static int foodID = 1;
    public static int potID = 2;
    public static int foodAmount;
    public static int potAmount;
    public static int eatAtPerc = 60;

    public static long nextPotAt = 0L;
    public static long dbaNextBoostAt   = 0L;
    public static long heartNextBoostAt = 0L;

    // Position
    public static WorldPosition currentPos;

    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();
    private static final Font ARIAL        = new Font("Arial", Font.PLAIN, 14);
    private static final Font ARIAL_BOLD   = new Font("Arial", Font.BOLD, 14);
    private static final Font ARIAL_ITALIC = new Font("Arial", Font.ITALIC, 14);

    public static boolean webhookEnabled = false;
    public static boolean webhookShowUser = false;
    public static boolean webhookShowStats = false;
    public static String webhookUrl = "";
    public static int webhookIntervalMinutes = 5;
    public static long lastWebhookSent = 0;
    public static String user = "";

    // XP Reading stuff
    public static double previousXpRead = -1;
    public static double totalXp = 0;
    public static long lastXpGainAt  = 0L;

    public static final Stopwatch switchTabTimer = new Stopwatch();

    private List<Task> tasks;
    private ScriptUI ui;

    public dGemstoneCrabber(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                4913, 4912, 4911,
                5169, 5168, 5167,
                5425, 5424, 5423
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return usePot || useFood;
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
        log("INFO", "Starting dGemstoneCrabber v" + scriptVersion);
        checkForUpdates();

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Gemstone crabber Options", false);

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

        usePot = ui.isUsePotions();
        useFood = ui.isUseFood();
        onlyHopAfterKill = ui.isOnlyHopBreakAfterKill();
        if (useFood) {
            foodID = ui.getSelectedFoodItemId();
            foodAmount = ui.getFoodQuantity();
            eatAtPerc = ui.getFoodEatPercent();
        }
        if (usePot) {
            potID = ui.getSelectedPotionItemId();
            potAmount = ui.getPotionQuantity();
        }
        useDBAXE = ui.isUseDragonBattleaxeSpec();
        useHearts = ui.isUseHeart();

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));
        tasks.add(new Bank(this));
        tasks.add(new Fight(this, this));
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

        // XP
        double xpPerHour = totalXp / hours;

        // Formatters
        java.text.DecimalFormat totalFmt = new java.text.DecimalFormat("#,###"); // grouped total (no decimals)
        String totalXpText   = totalFmt.format(Math.round(totalXp));
        String xpPerHourText = formatRateKMB(xpPerHour) + "/hr";

        // ---- Layout config (dynamic sizing via FontMetrics) ----
        final int x = 5;
        final int yTop = 40;
        final int borderThickness = 2;
        final int headerHeight = 25;
        final int paddingLeft = 10, paddingRight = 10;
        final int contentTopPad = 5, contentBottomPad = 8;
        final int groupGap = 10;

        FontMetrics fm       = c.getFontMetrics(ARIAL);
        FontMetrics fmBold   = c.getFontMetrics(ARIAL_BOLD);
        FontMetrics fmItalic = c.getFontMetrics(ARIAL_ITALIC);

        // Text lines (same styling as before)
        String title       = "dGemstoneCrabber";
        String lineXpG     = "XP gained: " + totalXpText;
        String lineXpR     = "XP rate: " + xpPerHourText;
        String lineBreak   = "Can break: " + canBreakNow + "  Time to break: " + getProfileManager().isDueToBreak();
        String lineHop     = "Can hop: " + canHopNow + "  Time to hop: " + getProfileManager().isDueToHop();
        String lineEat     = "Should eat: " + shouldEat;
        String linePot     = "Next pot drink: " + formatBoost(nextPotAt);
        String lineAxe     = "Next axe spec: " + formatBoost(dbaNextBoostAt);
        String lineHeart   = "Next heart use: " + formatBoost(heartNextBoostAt);
        String lineTask    = "Task: " + task;
        String lineLastXP  = "Last XP gain: " + formatLastXpGain();
        String lineVersion = "Version: " + scriptVersion;

        // ---- Measure max width ----
        int maxWidth = 0;
        maxWidth = Math.max(maxWidth, fm.stringWidth(lineXpG));
        maxWidth = Math.max(maxWidth, fm.stringWidth(lineXpR));
        maxWidth = Math.max(maxWidth, fm.stringWidth(lineBreak));
        maxWidth = Math.max(maxWidth, fm.stringWidth(lineHop));
        maxWidth = Math.max(maxWidth, fm.stringWidth(lineEat));
        if (usePot) {
            maxWidth = Math.max(maxWidth, fm.stringWidth(linePot));
        }
        if (useDBAXE) {
            maxWidth = Math.max(maxWidth, fm.stringWidth(lineAxe));
        }
        if (useHearts) {
            maxWidth = Math.max(maxWidth, fm.stringWidth(lineHeart));
        }
        maxWidth = Math.max(maxWidth, fmBold.stringWidth(lineTask));
        maxWidth = Math.max(maxWidth, fm.stringWidth(lineLastXP));
        maxWidth = Math.max(maxWidth, fmItalic.stringWidth(lineVersion));
        maxWidth = Math.max(maxWidth, fmBold.stringWidth(title)); // header title also constrains width

        // ---- Measure total height ----
        int totalHeight = 0;
        totalHeight += headerHeight + contentTopPad;
        totalHeight += fm.getHeight(); // xp gained
        totalHeight += fm.getHeight(); // xp rate
        totalHeight += groupGap;
        totalHeight += fm.getHeight(); // break
        totalHeight += fm.getHeight(); // hop
        totalHeight += groupGap;
        totalHeight += fm.getHeight(); // eat
        if (usePot) totalHeight += fm.getHeight(); // pot
        if (useDBAXE) totalHeight += fm.getHeight(); // pot
        if (useHearts) totalHeight += fm.getHeight(); // pot
        totalHeight += groupGap;
        totalHeight += fmBold.getHeight() + 5;     // task (kept a little extra spacing like before)
        totalHeight += fm.getHeight();       // last xp gained
        totalHeight += fmItalic.getHeight();       // version
        totalHeight += contentBottomPad;

        int innerWidth  = maxWidth + paddingLeft + paddingRight;
        int innerHeight = totalHeight;

        // ---- Outer white border highlight ----
        c.fillRect(x - borderThickness, yTop - borderThickness,
                innerWidth + (borderThickness * 2), innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);

        // ---- Black background box ----
        int innerX = x;
        int innerY = yTop;
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.BLACK.getRGB(), 1);

        // ---- White inner border ----
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        // ---- Gradient header ----
        for (int i = 0; i < headerHeight; i++) {
            int gradientColor = new Color(80 + (i * 3), 150 + (i * 3), 255, 255).getRGB();
            c.drawLine(innerX + 1, innerY + 1 + i, innerX + innerWidth - 2, innerY + 1 + i, gradientColor);
        }

        // Header bottom border
        int bottomBorderY = innerY + headerHeight + 1;
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(innerX + 1, bottomBorderY + i, innerX + innerWidth - 2, bottomBorderY + i, Color.WHITE.getRGB());
        }

        // ---- Script title (centered using FontMetrics) ----
        int titleWidth = fmBold.stringWidth(title);
        int titleX = innerX + (innerWidth / 2) - (titleWidth / 2);
        c.drawText(title, titleX, innerY + 18, Color.BLACK.getRGB(), ARIAL_BOLD);

        // ---- Content lines ----
        int cx = innerX + paddingLeft;
        int y = innerY + headerHeight + contentTopPad;

        // XP stats
        y += fm.getHeight();
        c.drawText(lineXpG, cx, y, new Color(144, 238, 144).getRGB(), ARIAL);
        y += fm.getHeight();
        c.drawText(lineXpR, cx, y, new Color(255, 215, 0).getRGB(), ARIAL);

        y += groupGap;

        // Break / Hop info
        y += fm.getHeight();
        c.drawText(lineBreak, cx, y, new Color(173, 216, 230).getRGB(), ARIAL);
        y += fm.getHeight();
        c.drawText(lineHop, cx, y, new Color(255, 182, 193).getRGB(), ARIAL);

        y += groupGap;

        // Consumables info
        y += fm.getHeight();
        c.drawText(lineEat, cx, y, new Color(173, 216, 230).getRGB(), ARIAL);
        if (usePot) {
            y += fm.getHeight();
            c.drawText(linePot, cx, y, new Color(255, 182, 193).getRGB(), ARIAL);
        }
        if (useDBAXE) {
            y += fm.getHeight();
            c.drawText(lineAxe, cx, y, new Color(255, 182, 193).getRGB(), ARIAL);
        }
        if (useHearts) {
            y += fm.getHeight();
            c.drawText(lineHeart, cx, y, new Color(255, 182, 193).getRGB(), ARIAL);
        }

        y += groupGap;

        // Task / Version
        y += fmBold.getHeight() + 5;
        c.drawText(lineTask, cx, y, new Color(0, 255, 255).getRGB(), ARIAL_BOLD);
        y += fm.getHeight();
        c.drawText(lineLastXP, cx, y, new Color(180, 180, 180).getRGB(), ARIAL);
        y += fmItalic.getHeight();
        c.drawText(lineVersion, cx, y, new Color(180, 180, 180).getRGB(), ARIAL_BOLD);
    }

    @Override
    public void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);
            double hours = Math.max(1e-9, elapsed / 3_600_000.0);

            // XP stats
            double xpPerHour = totalXp / hours;

            // Formatters
            java.text.DecimalFormat totalFmt = new java.text.DecimalFormat("#,###"); // grouped total (no decimals)
            String totalXpText = totalFmt.format(Math.round(totalXp));
            String xpPerHourText = formatRateKMB(xpPerHour) + "/hr";

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dGemstoneCrabber - ")
                    .append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")

                    // Accent color
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"XP gained\",\"value\":\"").append(totalXpText).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP rate\",\"value\":\"").append(xpPerHourText).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                        .append("],");
            } else {
                // Minimal description when stats hidden
                json.append("\"description\":\"")
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
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, secs);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
    }

    private String formatRateKMB(double value) {
        double abs = Math.abs(value);
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");
        if (abs >= 1_000_000_000d) {
            return df.format(value / 1_000_000_000d) + " b";
        } else if (abs >= 1_000_000d) {
            return df.format(value / 1_000_000d) + " m";
        } else if (abs >= 1_000d) {
            return df.format(value / 1_000d) + " k";
        } else {
            return df.format(value);
        }
    }

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dGemstoneCrabber/src/main/java/main/dGemstoneCrabber.java");
        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }
        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("dGemstoneCrabber.jar") || n.startsWith("dGemstoneCrabber-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dGemstoneCrabber-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dGemstoneCrabber/jar/dGemstoneCrabber.jar");

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

    public static List<Integer> getPotionVariantOrder(int potionId) {
        switch (potionId) {
            // Strength potion
            case ItemID.STRENGTH_POTION4:
            case ItemID.STRENGTH_POTION3:
            case ItemID.STRENGTH_POTION2:
            case ItemID.STRENGTH_POTION1:
                return List.of(ItemID.STRENGTH_POTION1, ItemID.STRENGTH_POTION2, ItemID.STRENGTH_POTION3, ItemID.STRENGTH_POTION4);

            // Combat potion
            case ItemID.COMBAT_POTION4:
            case ItemID.COMBAT_POTION3:
            case ItemID.COMBAT_POTION2:
            case ItemID.COMBAT_POTION1:
                return List.of(ItemID.COMBAT_POTION1, ItemID.COMBAT_POTION2, ItemID.COMBAT_POTION3, ItemID.COMBAT_POTION4);

            // Super strength
            case ItemID.SUPER_STRENGTH4:
            case ItemID.SUPER_STRENGTH3:
            case ItemID.SUPER_STRENGTH2:
            case ItemID.SUPER_STRENGTH1:
                return List.of(ItemID.SUPER_STRENGTH1, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH4);

            // Divine super strength
            case ItemID.DIVINE_SUPER_STRENGTH_POTION4:
            case ItemID.DIVINE_SUPER_STRENGTH_POTION3:
            case ItemID.DIVINE_SUPER_STRENGTH_POTION2:
            case ItemID.DIVINE_SUPER_STRENGTH_POTION1:
                return List.of(ItemID.DIVINE_SUPER_STRENGTH_POTION1, ItemID.DIVINE_SUPER_STRENGTH_POTION2, ItemID.DIVINE_SUPER_STRENGTH_POTION3, ItemID.DIVINE_SUPER_STRENGTH_POTION4);

            // Ranging
            case ItemID.RANGING_POTION4:
            case ItemID.RANGING_POTION3:
            case ItemID.RANGING_POTION2:
            case ItemID.RANGING_POTION1:
                return List.of(ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4);

            // Divine ranging
            case ItemID.DIVINE_RANGING_POTION4:
            case ItemID.DIVINE_RANGING_POTION3:
            case ItemID.DIVINE_RANGING_POTION2:
            case ItemID.DIVINE_RANGING_POTION1:
                return List.of(ItemID.DIVINE_RANGING_POTION1, ItemID.DIVINE_RANGING_POTION2, ItemID.DIVINE_RANGING_POTION3, ItemID.DIVINE_RANGING_POTION4);

            // Zamorak brew
            case ItemID.ZAMORAK_BREW4:
            case ItemID.ZAMORAK_BREW3:
            case ItemID.ZAMORAK_BREW2:
            case ItemID.ZAMORAK_BREW1:
                return List.of(ItemID.ZAMORAK_BREW1, ItemID.ZAMORAK_BREW2, ItemID.ZAMORAK_BREW3, ItemID.ZAMORAK_BREW4);

            // Bastion
            case ItemID.BASTION_POTION4:
            case ItemID.BASTION_POTION3:
            case ItemID.BASTION_POTION2:
            case ItemID.BASTION_POTION1:
                return List.of(ItemID.BASTION_POTION1, ItemID.BASTION_POTION2, ItemID.BASTION_POTION3, ItemID.BASTION_POTION4);

            // Divine bastion
            case ItemID.DIVINE_BASTION_POTION4:
            case ItemID.DIVINE_BASTION_POTION3:
            case ItemID.DIVINE_BASTION_POTION2:
            case ItemID.DIVINE_BASTION_POTION1:
                return List.of(ItemID.DIVINE_BASTION_POTION1, ItemID.DIVINE_BASTION_POTION2, ItemID.DIVINE_BASTION_POTION3, ItemID.DIVINE_BASTION_POTION4);

            // Super combat
            case ItemID.SUPER_COMBAT_POTION4:
            case ItemID.SUPER_COMBAT_POTION3:
            case ItemID.SUPER_COMBAT_POTION2:
            case ItemID.SUPER_COMBAT_POTION1:
                return List.of(ItemID.SUPER_COMBAT_POTION1, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION4);

            // Divine super combat
            case ItemID.DIVINE_SUPER_COMBAT_POTION4:
            case ItemID.DIVINE_SUPER_COMBAT_POTION3:
            case ItemID.DIVINE_SUPER_COMBAT_POTION2:
            case ItemID.DIVINE_SUPER_COMBAT_POTION1:
                return List.of(ItemID.DIVINE_SUPER_COMBAT_POTION1, ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION3, ItemID.DIVINE_SUPER_COMBAT_POTION4);

            default:
                // If it’s a single ID or unknown, just return the given one
                return List.of(potionId);
        }
    }

    public static int totalPotionAmount(ItemGroupResult inv, List<Integer> ids) {
        int sum = 0;
        for (int id : ids) sum += inv.getAmount(id);
        return sum;
    }

    public static List<Integer> getFoodVariantOrder(int foodId) {
        if (foodId == ItemID.CAKE) {
            return java.util.List.of(1895, 1893, 1891);
        }
        if (foodId == ItemID.PLAIN_PIZZA) {
            return java.util.List.of(2291, 2289);
        }

        // Default: single-ID food
        return java.util.List.of(foodId);
    }

    public static int totalAmount(ItemGroupResult inv, List<Integer> ids) {
        int sum = 0;
        for (int id : ids) sum += inv.getAmount(id);
        return sum;
    }

    private String formatBoost(long nextBoostAt) {
        if (nextBoostAt == 0L) {
            return "now";
        }
        long now = System.currentTimeMillis();
        long remaining = nextBoostAt - now;
        if (remaining <= 0) {
            return "now";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long sec = seconds % 60;

        return String.format("%02d:%02d", minutes, sec);
    }

    private String formatLastXpGain() {
        if (lastXpGainAt <= 0) {
            return "never";
        }

        long now = System.currentTimeMillis();
        long diffMs = now - lastXpGainAt;
        long diffSec = diffMs / 1000L;

        // Format local time hh:mm:ss
        java.time.LocalTime local =
                java.time.Instant.ofEpochMilli(lastXpGainAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime();

        String timeStr = local.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        return String.format("%s (%ds)", timeStr, diffSec);
    }
}
