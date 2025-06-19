package main;

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.FirstBank;
import tasks.ProcessTask;
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
import java.util.function.Predicate;

@ScriptDefinition(
        name = "dCannonballSmelter",
        description = "Turns steel bars into cannonballs",
        skillCategory = SkillCategory.SMITHING,
        version = 2.2,
        author = "JustDavyy"
)
public class dCannonballSmelter extends Script {
    public static final String scriptVersion = "2.2";
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

    public dCannonballSmelter(Object scriptCore) {
        super(scriptCore);
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
        int ballsSmelted = smeltCount * 4;
        int smeltsPerHour = (int) ((smeltCount * 3600000L) / elapsed);
        int ballsPerHour = (int) ((ballsSmelted * 3600000L) / elapsed);
        int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 220, 160, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 220, 160, Color.BLACK.getRGB());

        c.drawText("Cballs smelted: " + f.format(ballsSmelted), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Smelts/hr: " + f.format(smeltsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Cballs/hr: " + f.format(ballsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + f.format(totalXpGained), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Current task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
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
            int ballsSmelted = smeltCount * 4;
            int smeltsPerHour = (int) ((smeltCount * 3600000L) / elapsed);
            int ballsPerHour = (int) ((ballsSmelted * 3600000L) / elapsed);
            int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);

            String runtime = formatRuntime(elapsed);
            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{ \"embeds\": [ {")
                    .append("\"title\": \"\\uD83D\\uDCCA dCannonballSmelter Stats - ").append(webhookShowUser && user != null ? user : "anonymous").append("\",")
                    .append("\"color\": 4620980,");

            if (webhookShowStats) {
                json.append("\"fields\": [")
                        .append("{\"name\":\"Cballs smelted\",\"value\":\"").append(f.format(ballsSmelted)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Smelts/hr\",\"value\":\"").append(f.format(smeltsPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Cballs/hr\",\"value\":\"").append(f.format(ballsPerHour)).append("\",\"inline\":true},")
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
