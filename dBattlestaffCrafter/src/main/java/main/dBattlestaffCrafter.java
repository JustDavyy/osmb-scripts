package main;

// GENERAL JAVA IMPORTS
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;

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
import java.util.function.Predicate;

import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;

import javax.imageio.ImageIO;


// Script manifest (displays in script overview)
@ScriptDefinition(
        name = "dBattlestaffCrafter",
        description = "Attaches orbs to battlestaves for quick crafting experience",
        skillCategory = SkillCategory.CRAFTING,
        version = 1.3,
        author = "JustDavyy"
)

public class dBattlestaffCrafter extends Script {
    public static final String scriptVersion = "1.3";
    public static boolean setupDone = false;
    public static int staffID;

    public static boolean shouldBank = false;

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public static int craftedCount = 0;
    public static double totalXpGained = 0.0;
    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();
    private static final Font ARIEL = Font.getFont("Ariel");

    private List<Task> tasks;

    // Webhook vars
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    public dBattlestaffCrafter(Object scriptCore) {
        super(scriptCore);
    }

    // Override regions to prioritise to prevent or limit global searches
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
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        int perHour = (int) ((craftedCount * 3600000.0) / elapsed);
        int xpPerHour = (int) ((totalXpGained * 3600000.0) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 220, 140, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 220, 140, Color.BLACK.getRGB());

        c.drawText("Staffs crafted: " + f.format(craftedCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Crafts/hr: " + f.format(perHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + f.format(totalXpGained), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Current task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dBattlestaffCrafter v1.2");

        // Build and show UI
        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Battlestaff Crafter Options", false);

        staffID = ui.getSelectedStaffId();

        log(getClass().getSimpleName(), "We're making " + getItemManager().getItemName(staffID) + " during this run, enjoy!");

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

        // Build task list
        tasks = Arrays.asList(
                new Setup(this),
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

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{ \"embeds\": [ {")
                    .append("\"title\": \"\\uD83D\\uDCCA dBattlestaffCrafter Stats - ").append(webhookShowUser && user != null ? user : "anonymous").append("\",")
                    .append("\"color\": 4620980,");

            if (webhookShowStats) {
                int perHour = (int) ((craftedCount * 3600000.0) / elapsed);
                int xpPerHour = (int) ((totalXpGained * 3600000.0) / elapsed);

                json.append("\"fields\": [")
                        .append("{\"name\":\"Crafted\",\"value\":\"").append(f.format(craftedCount)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Crafts/hr\",\"value\":\"").append(f.format(perHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP Gained\",\"value\":\"").append(f.format(totalXpGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP/hr\",\"value\":\"").append(f.format(xpPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                        .append("{\"name\":\"Task\",\"value\":\"").append(task).append("\",\"inline\":true},")
                        .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                        .append("],");
            } else {
                json.append("\"description\": \"Currently on task: ").append(task).append("\",");
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

    private String formatRuntime(long ms) {
        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return (d > 0 ? String.format("%02d:", d) : "") + String.format("%02d:%02d:%02d", h, m, sec);
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dBattlestaffCrafter/src/main/java/main/dBattlestaffCrafter.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");

            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                if (!dir.exists()) dir.mkdirs();

                File[] old = dir.listFiles((d, n) -> n.equals("dBattlestaffCrafter.jar") || n.startsWith("dBattlestaffCrafter-"));
                if (old != null) {
                    for (File f : old) {
                        if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());
                    }
                }

                File out = new File(dir, "dBattlestaffCrafter-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dBattlestaffCrafter/jar/dBattlestaffCrafter.jar");

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
