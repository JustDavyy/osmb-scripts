package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.MineTask;
import tasks.Setup;
import tasks.CraftTask;
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
import java.util.concurrent.atomic.AtomicInteger;

@ScriptDefinition(
        name = "dAmethystMiner",
        description = "Mines and crafts/banks amethyst in the mining guild",
        skillCategory = SkillCategory.MINING,
        version = 1.9,
        author = "JustDavyy"
)
public class dAmethystMiner extends Script {
    public static final String scriptVersion = "1.9";

    public static boolean bankMode = false;
    public static boolean craftMode = false;
    public static boolean normalAreaMode = false;
    public static boolean diaryAreaMode = false;
    public static boolean setupDone = false;
    public static int selectedAmethystItemId = -1;

    public static int amethystMined = 0;
    public static int amethystCrafted = 0;

    public static int miningXpGained = 0;
    public static int craftingXpGained = 0;

    // Area
    public static final Area normalMiningArea = new RectangleArea(3015, 9698, 16, 11, 0);
    public static final Area diaryMiningArea = new RectangleArea(2999, 9705, 13, 23, 0);
    public static final Area normalMiningWalkArea = new RectangleArea(3022, 9707, 3, 2, 0);
    public static final Area diaryMiningWalkArea = new RectangleArea(3004, 9707, 4, 1, 0);

    public static String task = "Initializing...";
    public static long startTime = System.currentTimeMillis();

    private List<Task> tasks;

    // Fixed-size, shared fonts (prevents layout jitter)
    private static final Font ARIAL        = new Font("Arial", Font.PLAIN, 14);
    private static final Font ARIAL_BOLD   = new Font("Arial", Font.BOLD, 14);
    private static final Font ARIAL_ITALIC = new Font("Arial", Font.ITALIC, 14);

    // Webhook
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    public dAmethystMiner(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12183, 12184, 11927};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dAmethystMiner v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Amethyst Options", false);

        String mode = ui.getMode();
        String area = ui.getSelectedAreaMode();
        bankMode = mode.equals("Bank");
        craftMode = mode.equals("Craft");
        normalAreaMode = area.equals("Normal");
        diaryAreaMode = area.equals("Diary");
        selectedAmethystItemId = ui.getSelectedAmethystProductId();

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
                new BankTask(this),
                new CraftTask(this),
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
        double hours = Math.max(1e-9, elapsed / 3_600_000.0); // avoid div-by-zero

        // ---- Totals & rates ----
        int amethystHr     = (int) Math.round(amethystMined / hours);
        int miningXpHr     = (int) Math.round(miningXpGained / hours);

        int itemsCrafted   = calculateCraftedItems(amethystCrafted);
        int itemsCraftedHr = (int) Math.round(itemsCrafted / hours);
        int craftingXpHr   = (int) Math.round(craftingXpGained / hours);

        // ---- Formatters ----
        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
        java.text.DecimalFormatSymbols sy = new java.text.DecimalFormatSymbols();
        sy.setGroupingSeparator('.');
        fmt.setDecimalFormatSymbols(sy);

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

        // ---- Lines ----
        String title         = "dAmethystMiner";
        String lineMined     = "Amethyst mined: " + fmt.format(amethystMined);
        String lineMinedHr   = "Amethyst/hr: " + fmt.format(amethystHr);
        String lineMXPG      = "Mining XP gained: " + fmt.format(miningXpGained);
        String lineMXPHr     = "Mining XP/hr: " + fmt.format(miningXpHr);

        String lineCXPG      = craftMode ? ("Crafting XP gained: " + fmt.format(craftingXpGained)) : null;
        String lineCXPHr     = craftMode ? ("Crafting XP/hr: " + fmt.format(craftingXpHr)) : null;
        String lineCrafted   = craftMode ? ("Crafted items: " + fmt.format(itemsCrafted)) : null;
        String lineCraftedHr = craftMode ? ("Items/hr: " + fmt.format(itemsCraftedHr)) : null;

        String lineTask      = "Task: " + task;
        String lineVersion   = "Version: " + scriptVersion;

        // ---- Measure max width ----
        AtomicInteger maxWidth = new AtomicInteger(fmBold.stringWidth(title));
        java.util.function.Consumer<String> widen = s -> { if (s != null) maxWidth.set(Math.max(maxWidth.get(), fm.stringWidth(s))); };

        widen.accept(lineMined);
        widen.accept(lineMinedHr);
        widen.accept(lineMXPG);
        widen.accept(lineMXPHr);
        if (craftMode) {
            widen.accept(lineCXPG);
            widen.accept(lineCXPHr);
            widen.accept(lineCrafted);
            widen.accept(lineCraftedHr);
        }
        maxWidth.set(Math.max(maxWidth.get(), fmBold.stringWidth(lineTask)));
        maxWidth.set(Math.max(maxWidth.get(), fmItalic.stringWidth(lineVersion)));

        // ---- Measure total height ----
        int totalHeight = 0;
        totalHeight += headerHeight + contentTopPad;

        totalHeight += fm.getHeight(); // mined
        totalHeight += fm.getHeight(); // mined/hr
        totalHeight += groupGap;

        totalHeight += fm.getHeight(); // mining xp gained
        totalHeight += fm.getHeight(); // mining xp/hr

        if (craftMode) {
            totalHeight += groupGap;
            totalHeight += fm.getHeight(); // crafting xp gained
            totalHeight += fm.getHeight(); // crafting xp/hr
            totalHeight += fm.getHeight(); // crafted
            totalHeight += fm.getHeight(); // crafted/hr
        }

        totalHeight += groupGap;
        totalHeight += fmBold.getHeight();   // task
        totalHeight += fmItalic.getHeight(); // version
        totalHeight += contentBottomPad;

        int innerWidth  = maxWidth.get() + paddingLeft + paddingRight;
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
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(innerX + 1, innerY + headerHeight + i + 1, innerX + innerWidth - 2, innerY + headerHeight + i + 1, Color.WHITE.getRGB());
        }

        // ---- Title ----
        int titleWidth = fmBold.stringWidth(title);
        int titleX = innerX + (innerWidth / 2) - (titleWidth / 2);
        c.drawText(title, titleX, innerY + 18, Color.BLACK.getRGB(), ARIAL_BOLD);

        // ---- Content ----
        int cx = innerX + paddingLeft;
        int y  = innerY + headerHeight + contentTopPad;

        // mined
        y += fm.getHeight();
        c.drawText(lineMined,   cx, y, Color.WHITE.getRGB(), ARIAL);
        y += fm.getHeight();
        c.drawText(lineMinedHr, cx, y, Color.WHITE.getRGB(), ARIAL);

        y += groupGap;

        // mining xp
        y += fm.getHeight();
        c.drawText(lineMXPG,  cx, y, new Color(144, 238, 144).getRGB(), ARIAL); // light green
        y += fm.getHeight();
        c.drawText(lineMXPHr, cx, y, new Color(255, 215, 0).getRGB(),   ARIAL); // gold

        if (craftMode) {
            y += groupGap;
            y += fm.getHeight();
            c.drawText(lineCXPG,      cx, y, new Color(255, 182, 193).getRGB(), ARIAL); // pink
            y += fm.getHeight();
            c.drawText(lineCXPHr,     cx, y, new Color(255, 182, 193).getRGB(), ARIAL);
            y += fm.getHeight();
            c.drawText(lineCrafted,   cx, y, Color.WHITE.getRGB(), ARIAL);
            y += fm.getHeight();
            c.drawText(lineCraftedHr, cx, y, Color.WHITE.getRGB(), ARIAL);
        }

        y += groupGap;

        // footer
        y += fmBold.getHeight();
        c.drawText(lineTask,    cx, y, new Color(0, 255, 255).getRGB(), ARIAL_BOLD); // cyan
        y += fmItalic.getHeight();
        c.drawText(lineVersion, cx, y, new Color(180, 180, 180).getRGB(), ARIAL_ITALIC);
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            double hours = elapsed / 3600000.0;

            int amethystHr = (int) (amethystMined / hours);
            int miningXpHr = (int) (miningXpGained / hours);

            int itemsCrafted = calculateCraftedItems(amethystCrafted);
            int craftingXpHr = (int) (craftingXpGained / hours);
            int itemsCraftedHr = (int) (itemsCrafted / hours);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            String runtime = formatRuntime(elapsed);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dAmethystMiner Stats - ").append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"Amethyst Mined\",\"value\":\"").append(f.format(amethystMined)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Amethyst/hr\",\"value\":\"").append(f.format(amethystHr)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Mining XP\",\"value\":\"").append(f.format(miningXpGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Mining XP/hr\",\"value\":\"").append(f.format(miningXpHr)).append("\",\"inline\":true},");

                if (craftMode) {
                    json.append("{\"name\":\"Crafting XP\",\"value\":\"").append(f.format(craftingXpGained)).append("\",\"inline\":true},")
                            .append("{\"name\":\"Crafting XP/hr\",\"value\":\"").append(f.format(craftingXpHr)).append("\",\"inline\":true},")
                            .append("{\"name\":\"Crafted Items\",\"value\":\"").append(f.format(itemsCrafted)).append("\",\"inline\":true},")
                            .append("{\"name\":\"Items/hr\",\"value\":\"").append(f.format(itemsCraftedHr)).append("\",\"inline\":true},");
                }

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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dAmethystMiner/src/main/java/main/dAmethystMiner.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                File[] old = dir.listFiles((d, n) -> n.equals("dAmethystMiner.jar") || n.startsWith("dAmethystMiner-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dAmethystMiner-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dAmethystMiner/jar/dAmethyst.jar");
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

    public static Area getMiningArea() {
        if (diaryAreaMode) {
            return diaryMiningArea;
        } else {
            return normalMiningArea;
        }
    }

    public static Area getMiningWalkArea() {
        if (diaryAreaMode) {
            return diaryMiningWalkArea;
        } else {
            return normalMiningWalkArea;
        }
    }

    private int calculateCraftedItems(int amethystUsed) {
        return switch (selectedAmethystItemId) {
            case ItemID.AMETHYST_ARROWTIPS, ItemID.AMETHYST_BOLT_TIPS -> amethystUsed * 15;
            case ItemID.AMETHYST_DART_TIP -> amethystUsed * 8;
            case ItemID.AMETHYST_JAVELIN_HEADS -> amethystUsed * 5;
            default -> 0;
        };
    }
}
