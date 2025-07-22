package main;

import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import component.TargetView;
import javafx.scene.Scene;
import tasks.*;
import utils.Task;

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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;

import javax.imageio.ImageIO;

@ScriptDefinition(
        name = "dRangingGuild",
        description = "Trains ranged by doing the ranging guild minigame",
        skillCategory = SkillCategory.COMBAT,
        version = 1.8,
        author = "JustDavyy"
)
public class dRangingGuild extends Script {
    public static final String scriptVersion = "1.8";
    public static boolean setupDone = false;
    public static boolean failSafeNeeded = false;
    public static boolean needsToSwitchGear = false;
    public static int rangedLevel = 0;

    // Webhook settings
    private static final Stopwatch webhookTimer = new Stopwatch();
    public static boolean webhookEnabled;
    public static String webhookUrl;
    public static int webhookInterval;
    public static boolean webhookIncludeUser;
    public static boolean webhookIncludeStats;
    private static String user = "";

    // Ranging Guild stuff
    public static int totalRounds = 0;
    public static String task = "Initializing";
    public static boolean readyToShoot = false;
    public static int currentScore = 0;
    public static int totalScore = 0;
    public static int shotsLeft = 0;
    public static int missedShots = 0;
    public static int blackShots = 0;
    public static int blueShots = 0;
    public static int redShots = 0;
    public static int yellowShots = 0;
    public static int bullShots = 0;

    // onPaint stuff
    private static final Font FONT = Font.getFont("Arial");

    // Failsafes
    public static long lastTaskRanAt = System.currentTimeMillis() + 120000;
    public static int competitionDialogueCounter = 0;

    public static TargetView targetInterface;
    private List<Task> tasks;

    public dRangingGuild(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                10549,  // Ranging Guild
        };
    }

    @Override
    public void onPaint(Canvas c) {
        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        String runtime = formatTime(elapsed);

        // Calculate XP and tickets
        int scoreTotal = totalScore + currentScore;
        int ticketsEarned = scoreTotal / 10;
        int xpGained = scoreTotal / 2;
        int xpPerHour = (int) ((xpGained * 3600000.0) / elapsed);

        // Calculate shot statistics
        int totalShots = bullShots + yellowShots + redShots + blueShots + blackShots + missedShots;

        int y = 40;
        c.fillRect(5, y, 250, 325, Color.BLACK.getRGB(), 1);
        c.drawRect(5, y, 250, 325, Color.BLACK.getRGB());

        c.drawText("Tickets earned: " + f.format(ticketsEarned), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Rounds completed: " + f.format(totalRounds), 10, y += 20, Color.WHITE.getRGB(), FONT);

        c.drawText("Ranged XP gained: " + f.format(xpGained), 10, y += 30, Color.WHITE.getRGB(), FONT);
        c.drawText("XP per hour: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), FONT);

        // Shot breakdown
        c.drawText("Shot Stats: (" + totalShots + " shots)", 10, y += 30, Color.WHITE.getRGB(), FONT);
        c.drawText("• Bulls-eye: " + bullShots + " (" + percent(bullShots, totalShots) + ")", 15, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("• Yellow: " + yellowShots + " (" + percent(yellowShots, totalShots) + ")", 15, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("• Red: " + redShots + " (" + percent(redShots, totalShots) + ")", 15, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("• Blue: " + blueShots + " (" + percent(blueShots, totalShots) + ")", 15, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("• Black: " + blackShots + " (" + percent(blackShots, totalShots) + ")", 15, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("• Missed: " + missedShots + " (" + percent(missedShots, totalShots) + ")", 15, y += 20, Color.WHITE.getRGB(), FONT);

        c.drawText("Task: " + task, 10, y += 35, Color.WHITE.getRGB(), FONT);
        c.drawText("Runtime: " + runtime, 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Version: " + scriptVersion, 10, y + 20, Color.WHITE.getRGB(), FONT);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Launching dRangingGuild v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Ranging Guild Options", false);

        // WEBHOOKS
        webhookEnabled = ui.isWebhookEnabled();
        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            webhookUrl = ui.getWebhookUrl();
            webhookInterval = ui.getWebhookInterval();
            webhookIncludeUser = ui.isUsernameIncluded();
            webhookIncludeStats = ui.isStatsIncluded();
            webhookTimer.reset(webhookInterval * 60_000L);
            log("WEBHOOK", "Webhook enabled, sending every " + webhookInterval + " minutes.");
        }

        // Initialize targetview component
        targetInterface = new TargetView(this);

        // Check for script updates
        checkForUpdates();

        tasks = Arrays.asList(
                new Setup(this),
                new FailSafe(this),
                new SwitchGear(this),
                new RangeTask(this),
                new TalkTask(this)
        );
    }

    @Override
    public int poll() {
        if (webhookEnabled && webhookTimer.hasFinished()) {
            sendWebhook();
            webhookTimer.reset(webhookInterval * 60_000L);
        }

        var dialogue = getWidgetManager().getDialogue();
        DialogueType type = dialogue.getDialogueType();

        if (type != null && type.equals(DialogueType.TAP_HERE_TO_CONTINUE)) {
            UIResult<String> textResult = dialogue.getText();

            if (textResult != null && !textResult.isNotFound() && !textResult.isNotVisible()) {
                String text = textResult.get().toLowerCase();

                if (text.contains("level is now")) {
                    log("LEVELUP", text);

                    // Extract number from text
                    Matcher matcher = Pattern.compile("\\d+").matcher(text);
                    if (matcher.find()) {
                        try {
                            rangedLevel = Integer.parseInt(matcher.group());
                            log("RANGED_LEVEL", "Updated to " + rangedLevel);

                            if (rangedLevel == 50 || rangedLevel == 60 || rangedLevel == 70 || rangedLevel == 77) {
                                needsToSwitchGear = true;
                                log("GEAR_SWITCH", "Ranged milestone reached: " + rangedLevel);
                            }

                        } catch (NumberFormatException ignored) {
                            log("ERROR", "Failed to parse ranged level.");
                        }
                    }

                    dialogue.continueChatDialogue();
                    submitHumanTask(() -> false, random(500, 750));
                    return 0;
                }

                if (text.contains("can now")) {
                    dialogue.continueChatDialogue();
                    submitTask(() -> false, random(500, 750));
                    return 0;
                }
            }
        }

        // Check for inactivity
        long idleTime = System.currentTimeMillis() - lastTaskRanAt;
        if (idleTime > 15_000) {  // 15 seconds of nothing
            log("FailSafe", "No tasks ran in the last " + idleTime + "ms, triggering failsafe...");
            failSafeNeeded = true;
        }

        if (tasks != null) {
            for (Task task : tasks) {
                if (task.activate()) {
                    task.execute();
                    return 0;
                }
            }
        } else {
            log(getClass(), "Tasks is null?");
        }

        return 0;
    }

    private void checkForUpdates() {
        try {
            String urlRaw = "https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dRangingGuild/src/main/java/main/dRangingGuild.java";
            String latest = getLatestVersion(urlRaw);
            if (latest == null) {
                log("UPDATE", "⚠ Could not fetch latest version info.");
                return;
            }

            if (compareVersions(latest) < 0) {
                log("UPDATE", "⏬ New version v" + latest + " found! Updating...");
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                for (File f : Objects.requireNonNull(dir.listFiles((d, n) -> n.startsWith("dRangingGuild")))) {
                    if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());
                }

                File out = new File(dir, "dRangingGuild-" + latest + ".jar");
                URL jarUrl = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dRangingGuild/jar/dRangingGuild.jar");
                try (InputStream in = jarUrl.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }
                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();
            } else {
                log("SCRIPTVERSION", "✅ You are running the latest version (v" + scriptVersion + ").");
            }
        } catch (Exception e) {
            log("UPDATE", "❌ Error updating: " + e.getMessage());
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
        } catch (Exception ignored) {}
        return null;
    }

    private int compareVersions(String v2) {
        String[] a = dRangingGuild.scriptVersion.split("\\.");
        String[] b = v2.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private void sendWebhook() {
        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) return;

            com.osmb.api.visual.image.Image screenImage = getScreen().getImage();
            BufferedImage bufferedImage = screenImage.toBufferedImage();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String boundary = "----WebhookBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            long now = System.currentTimeMillis();
            long elapsed = now - startTime;
            String formattedRuntime = formatTime(elapsed);

            // ✅ Calculate values
            int scoreTotal = totalScore + currentScore;
            int ticketsEarned = scoreTotal / 10;
            int xpGained = scoreTotal / 2;
            int xpPerHour = (int) ((xpGained * 3600000.0) / elapsed);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            DecimalFormat f = new DecimalFormat("#,###");
            f.setDecimalFormatSymbols(symbols);

            StringBuilder payload = new StringBuilder();
            payload.append("{\"embeds\": [{");
            payload.append("\"title\": \"🏹 dRangingGuild Stats - ")
                    .append(webhookIncludeUser && user != null ? escapeJson(user) : "anonymous").append("\",");
            payload.append("\"color\": 15844367,");
            payload.append("\"fields\": [")
                    .append("{\"name\": \"Tickets\", \"value\": \"").append(f.format(ticketsEarned)).append("\", \"inline\": true},")
                    .append("{\"name\": \"Rounds\", \"value\": \"").append(f.format(totalRounds)).append("\", \"inline\": true},")
                    .append("{\"name\": \"Ranged XP\", \"value\": \"").append(f.format(xpGained)).append("\", \"inline\": true},")
                    .append("{\"name\": \"XP/hr\", \"value\": \"").append(f.format(xpPerHour)).append("\", \"inline\": true},")
                    .append("{\"name\": \"Current Task\", \"value\": \"").append(escapeJson(task)).append("\", \"inline\": true},")
                    .append("{\"name\": \"Runtime\", \"value\": \"").append(formattedRuntime).append("\", \"inline\": true},")
                    .append("{\"name\": \"Version\", \"value\": \"").append(scriptVersion).append("\", \"inline\": true}")
                    .append("],");
            payload.append("\"image\": {\"url\": \"attachment://screen.png\"}}]}");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\n".getBytes());
                out.write("Content-Type: image/png\r\n\r\n".getBytes());
                out.write(imageBytes);
                out.write("\r\n".getBytes());
                out.write(("--" + boundary + "--\r\n").getBytes());
                out.flush();
            }

            int response = conn.getResponseCode();
            log("WEBHOOK", response == 204 || response == 200 ? "✅ Sent webhook" : "⚠ Failed with HTTP " + response);
        } catch (Exception e) {
            log("WEBHOOK", "❌ Failed to send webhook: " + e.getMessage());
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String formatTime(long millis) {
        long seconds = Math.max(millis / 1000, 0);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String percent(int count, int totalShots) {
        return totalShots == 0 ? "0%" : String.format("%.1f%%", (count * 100.0) / totalShots);
    }

}
