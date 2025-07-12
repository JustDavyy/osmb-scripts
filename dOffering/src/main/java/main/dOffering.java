package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.spellbook.Spell;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.Bank;
import tasks.Cast;
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
        name = "dOffering",
        description = "Performs the Sinister or Demonic offering spell for prayer gains",
        skillCategory = SkillCategory.PRAYER,
        version = 1.1,
        author = "JustDavyy"
)
public class dOffering extends Script {
    public static final String scriptVersion = "1.1";

    // Script state trackers
    public static boolean setupDone = false;
    public static int castsDone = 0;
    public static int castsThisInvent = 0;
    public static int castsPerInvent = 0;
    public static boolean needToBank = true;
    public static Spell spellToCast;

    // Offering selection
    public static String selectedSpell = "";
    public static int selectedItem = -1;
    public static int xpPerCast = 0;
    public static int xpPerItem = 0;

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

    public dOffering(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                13104, // Shantay Pass
                13105, // Al Kharid
                13363, // Duel Arena / PvP Arena
                12850, // Lumbridge Castle
                12338, // Draynor
                12853, // Varrock East
                12597, // Varrock West + Cooks Guild
                12598, // Grand Exchange
                12342, // Edgeville
                12084, // Falador East + Mining GUild
                11828, // Falador West
                11571, // Crafting Guild
                11319, // Warriors Guild
                11061, // Catherby
                10806, // Seers
                11310, // Shilo
                10284, // Corsair Cove
                9772,  // Myths Guild
                10288, // Yanille
                10545, // Port Khazard
                10547, // Ardougne East/South
                10292, // Ardougne East/North
                10293, // Fishing Guild
                10039, // Barbarian Assault
                9782,  // Grand Tree
                9781,  // Tree Gnome Stronghold
                9776,  // Castle Wars
                9265,  // Lletya
                8748,  // Soul Wars
                8253,  // Lunar Isle
                9275,  // Neitiznot
                9531,  // Jatiszo
                6461,  // Wintertodt
                7227,  // Port Piscarilius
                6458,  // Arceeus
                6457,  // Kourend Castle
                6968,  // Hosidius
                7223,  // Vinery
                6710,  // Sand Crabs Chest
                6198,  // Woodcutting Guild
                5941,  // Land's End
                5944,  // Shayzien
                5946,  // Lovakengj South
                5691,  // Lovekengj North
                4922,  // Farming Guild
                4919,  // Chambers of Xeric
                5938,  // Quetzacalli
                6448,  // Varlamore West
                6960,  // Varlamore East
                6191,  // Hunter Guild
                5421,  // Aldarin
                5420,  // Mistrock
                14638, // Mos'le Harmless
                14642, // TOB
                14646, // Port Phasmatys
                12344, // Ferox Enclave
                12895, // Priff North
                13150, // Priff South
                13907, // Museum Camp
                14908, // Fossil Bank Chest island
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dOffering v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Script Options", false);

        // Load UI selections
        selectedSpell = ui.getSelectedSpell();
        selectedItem = ui.getSelectedItem();
        xpPerItem = ui.getSelectedItemXP();
        xpPerCast = ui.getXpPerSpellCast();
        spellToCast = ui.getSpellToCast();

        log("Setup", "Selected spell: " + selectedSpell + ", Item: " + selectedItem);

        // Webhook setup
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
                new Cast(this)
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


        int castsPerHour = (int) (castsDone / hours);
        int prayerXpGained = (castsDone * 3) * xpPerItem;
        int magicXpGained = castsDone * xpPerCast;
        int prayerXpPerHour = (int) (prayerXpGained / hours);
        int magicXpPerHour = (int) (magicXpGained / hours);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int x = 5;
        int y = 40;
        int width = 280;
        int height = 240; // Adjusted for added lines
        int borderThickness = 2;

        // Draw outer white border as highlight
        c.fillRect(x - borderThickness, y - borderThickness, width + (borderThickness * 2), height + (borderThickness * 2), Color.WHITE.getRGB(), 1);

        // Draw inner black background within border
        int innerY = y;
        c.fillRect(x, innerY, width, height, Color.BLACK.getRGB(), 1);

        // Draw inner white border inside the outer border for clarity
        c.drawRect(x, innerY, width, height, Color.WHITE.getRGB());

        // Draw prayer-themed gradient header bar (white to yellow)
        int headerHeight = 25;
        for (int i = 0; i < headerHeight; i++) {
            int gradientColor = new Color(255, 255 - (i * 4), i * 10, 255).getRGB(); // From white to golden yellow
            c.drawLine(x + 1, innerY + 1 + i, x + width - 2, innerY + 1 + i, gradientColor);
        }

        // Draw bottom white border under the header with same thickness as outer border
        int bottomBorderYStart = innerY + headerHeight + 1;
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(x + 1, bottomBorderYStart + i, x + width - 2, bottomBorderYStart + i, Color.WHITE.getRGB());
        }

        // Draw script title centered in header with ✨ glistering star emojis, text in black
        String title = "✨ dOffering ✨";
        int approxCharWidth = 7;
        int titlePixelWidth = title.length() * approxCharWidth;
        int titleX = x + (width / 2) - (titlePixelWidth / 2);
        c.drawText(title, titleX, innerY + 18, Color.BLACK.getRGB(), ARIEL_BOLD);

        y = innerY + headerHeight + 5;

        // Draw casts section
        c.drawText("Casts done: " + f.format(castsDone), x + 10, y += 20, new Color(144, 238, 144).getRGB(), ARIEL);
        c.drawText("Casts/hr: " + f.format(castsPerHour), x + 10, y += 20, new Color(255, 215, 0).getRGB(), ARIEL);

        // Spacer
        y += 10;

        // Draw prayer XP section
        c.drawText("Prayer XP gained: " + f.format(prayerXpGained), x + 10, y += 20, new Color(173, 216, 230).getRGB(), ARIEL);
        c.drawText("Prayer XP/hr: " + f.format(prayerXpPerHour), x + 10, y += 20, new Color(255, 182, 193).getRGB(), ARIEL);

        // Draw magic XP section
        c.drawText("Magic XP gained: " + f.format(magicXpGained), x + 10, y += 20, new Color(173, 216, 230).getRGB(), ARIEL);
        c.drawText("Magic XP/hr: " + f.format(magicXpPerHour), x + 10, y += 20, new Color(255, 182, 193).getRGB(), ARIEL);

        // Spacer
        y += 10;

        // Draw current task in bright cyan
        c.drawText("Task: " + task, x + 10, y += 25, new Color(0, 255, 255).getRGB(), ARIEL_BOLD);

        // Draw selected spell + item in single line
        c.drawText("Using: " + selectedSpell + " + " + getItemManager().getItemName(selectedItem), x + 10, y += 20, Color.WHITE.getRGB(), ARIEL);

        // Draw version info with darker grey
        c.drawText("Version: " + scriptVersion, x + 10, y += 20, new Color(180, 180, 180).getRGB(), ARIEL_ITALIC);
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            double hours = elapsed / 3600000.0;

            int castsPerHour = (int) (castsDone / hours);
            int prayerXpGained = (castsDone * 3) * xpPerItem;
            int magicXpGained = castsDone * xpPerCast;
            int prayerXpPerHour = (int) (prayerXpGained / hours);
            int magicXpPerHour = (int) (magicXpGained / hours);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            String runtime = formatRuntime(elapsed);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dOffering Stats - ").append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        // Casts
                        .append("{\"name\":\"Casts done\",\"value\":\"").append(f.format(castsDone)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Casts/hr\",\"value\":\"").append(f.format(castsPerHour)).append("\",\"inline\":true},")
                        // Prayer XP
                        .append("{\"name\":\"Prayer XP gained\",\"value\":\"").append(f.format(prayerXpGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Prayer XP/hr\",\"value\":\"").append(f.format(prayerXpPerHour)).append("\",\"inline\":true},")
                        // Magic XP
                        .append("{\"name\":\"Magic XP gained\",\"value\":\"").append(f.format(magicXpGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Magic XP/hr\",\"value\":\"").append(f.format(magicXpPerHour)).append("\",\"inline\":true},")
                        // Task
                        .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                        // Runtime
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        // Version
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
        String latest = getLatestVersion();

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                File[] old = dir.listFiles((d, n) -> n.equals("dOffering.jar") || n.startsWith("dOffering-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dOffering-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dOffering/jar/dOffering.jar");
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

    private String getLatestVersion() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dOffering/src/main/java/main/dOffering.java").openConnection();
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
