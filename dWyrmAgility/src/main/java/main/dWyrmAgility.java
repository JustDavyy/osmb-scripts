package main;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(
        name = "dWyrmAgility",
        author = "JustDavyy",
        version = 1.6,
        description = "Does the Wyrm basic or advanced agility course.",
        skillCategory = SkillCategory.AGILITY
)
public class dWyrmAgility extends Script {
    public static final String scriptVersion = "1.6";
    private Course selectedCourse;
    private int nextRunActivate;
    public int noMovementTimeout = RandomUtils.weightedRandom(6000, 9000);
    public static double xpGained = 0;
    public static int lapCount = 0;
    private final long startTime = System.currentTimeMillis();
    private long lastStatsPrint = 0L;

    // Webhook config
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    public static String task = "Initialize";
    private final Font font = Font.getFont("Ariel");

    private int failThreshold = random(4, 6);
    private int failCount = 0;

    public dWyrmAgility(Object object) {
        super(object);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName The name of the obstacle
     * @param menuOption   The name of the menu option to select
     * @param end          The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param timeout      The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, 1, timeout);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, true, timeout);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param canReach         If {@code false} then this method will avoid using {@link RSObject#canReach()} when querying objects for the obstacle.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, boolean canReach, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, canReach, timeout, null);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param canReach         If {@code false} then this method will avoid using {@link RSObject#canReach()} when querying objects for the obstacle.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @param objectBaseTile   The base tile of the object. If null we avoid this check.
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, boolean canReach, int timeout, WorldPosition objectBaseTile) {
        // cache hp, we determine if we failed the obstacle via hp decrementing
        UIResult<Integer> hitpoints = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        Optional<RSObject> result = core.getObjectManager().getObject(gameObject -> {

            if (gameObject.getName() == null || gameObject.getActions() == null) return false;

            if (!gameObject.getName().equalsIgnoreCase(obstacleName)) {
                return false;
            }

            if (objectBaseTile != null) {
                if (!objectBaseTile.equals(gameObject.getWorldPosition())) {
                    return false;
                }
            }
            if (!canReach) {
                return true;
            }

            return gameObject.canReach(interactDistance);
        });
        if (result.isEmpty()) {
            core.log(dWyrmAgility.class.getSimpleName(), "ERROR: Obstacle (" + obstacleName + ") does not exist with criteria.");
            return ObstacleHandleResponse.OBJECT_NOT_IN_SCENE;
        }
        RSObject object = result.get();
        if (object.interact(menuOption)) {
            core.log(dWyrmAgility.class.getSimpleName(), "Interacted successfully, sleeping until conditions are met...");
            Timer noMovementTimer = new Timer();
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>();
            if (core.submitHumanTask(() -> {
                WorldPosition currentPos = core.getWorldPosition();
                if (currentPos == null) {
                    return false;
                }
                // check if we take damage
                if (hitpoints.isFound()) {
                    UIResult<Integer> newHitpointsResult = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
                    if (newHitpointsResult.isFound()) {
                        if (hitpoints.get() > newHitpointsResult.get()) {
                            return true;
                        }
                    }
                }
                // check for being stood still
                if (previousPosition.get() != null) {
                    if (currentPos.equals(previousPosition.get())) {
                        if (noMovementTimer.timeElapsed() > core.noMovementTimeout) {
                            core.noMovementTimeout = RandomUtils.weightedRandom(2500, 4000);
                            core.printFail();
                            core.failCount++;
                            return true;
                        }
                    } else {
                        noMovementTimer.reset();
                    }
                } else {
                    noMovementTimer.reset();
                }
                previousPosition.set(currentPos);

                RSTile tile = core.getSceneManager().getTile(core.getWorldPosition());
                Polygon poly = tile.getTileCube(120);
                if (core.getPixelAnalyzer().isAnimating(0.1, poly)) {
                    return false;
                }
                if (end instanceof Area area) {
                    if (area.contains(currentPos)) {
                        core.failThreshold = Utils.random(2, 3);
                        return true;
                    }
                } else if (end instanceof Position pos) {
                    if (currentPos.equals(pos)) {
                        core.failThreshold = Utils.random(2, 3);
                        return true;
                    }
                }
                return false;
            }, timeout)) {
                return ObstacleHandleResponse.SUCCESS;
            } else {
                core.failCount++;
                core.printFail();
                return ObstacleHandleResponse.TIMEOUT;
            }
        } else {
            core.log(dWyrmAgility.class.getSimpleName(), "ERROR: Failed interacting with obstacle (" + obstacleName + ").");
            core.failCount++;
            return ObstacleHandleResponse.FAILED_INTERACTION;
        }
    }

    private void printFail() {
        log(dWyrmAgility.class, "Failed to handle obstacle. Fail count: " + failCount + "/" + failThreshold);
    }

    @Override
    public void onStart() {
        UI ui = new UI();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dWyrmAgility Settings", false);

        this.selectedCourse = ui.selectedCourse();
        this.nextRunActivate = random(30, 70);

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

        checkForUpdates();
    }

    @Override
    public void onRelog() {
        failCount = 0;
    }

    @Override
    public int poll() {
        if (failCount > failThreshold) {
            log("ERROR", "Failed object multiple times. Relogging.");
            getWidgetManager().getLogoutTab().logout();
            return 0;
        }

        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            sendWebhook();
            lastWebhookSent = System.currentTimeMillis();
        }

        long now = System.currentTimeMillis();
        if (now - lastStatsPrint >= 30000) {
            printStats();
            lastStatsPrint = now;
        }

        UIResult<Boolean> runEnabled = getWidgetManager().getMinimapOrbs().isRunEnabled();
        if (runEnabled.isFound()) {
            int runEnergy = getWidgetManager().getMinimapOrbs().getRunEnergy().orElse(-1);
            if (!runEnabled.get() && runEnergy > nextRunActivate) {
                log("RUN", "Enabling run");
                getWidgetManager().getMinimapOrbs().setRun(true);
                nextRunActivate = random(30, 70);
            }
        }

        WorldPosition pos = getWorldPosition();
        if (pos == null) return 0;
        return selectedCourse.poll(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        if (selectedCourse == null) {
            return new int[0];
        }
        return selectedCourse.regions();
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        int xpPerHour = (int) ((xpGained * 3600000L) / elapsed);
        int lapsPerHour = (int) ((lapCount * 3600000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 220, 130, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 220, 130, Color.BLACK.getRGB());

        c.drawText("XP gained: " + f.format(xpGained), 10, y += 20, Color.WHITE.getRGB(), font);
        c.drawText("XP/hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), font);
        c.drawText("Laps done: " + f.format(lapCount), 10, y += 20, Color.WHITE.getRGB(), font);
        c.drawText("Laps/hr: " + f.format(lapsPerHour), 10, y += 20, Color.WHITE.getRGB(), font);
        c.drawText("Task: " + task, 10, y += 20, Color.WHITE.getRGB(), font);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), font);
    }

    public void printStats() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed == 0) return;

        int xpPerHour = (int) ((xpGained * 3600000L) / elapsed);
        int lapsPerHour = (int) ((lapCount * 3600000L) / elapsed);

        log("STATS", String.format(
                "XP gained: %,.1f | XP/hr: %,d | Laps done: %,d | Laps/hr: %,d",
                xpGained, xpPerHour, lapCount, lapsPerHour
        ));
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage img = getScreen().getImage().toBufferedImage();
            ImageIO.write(img, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);
            int xpPerHour = (int) ((xpGained * 3600000L) / elapsed);
            int lapsPerHour = (int) ((lapCount * 3600000L) / elapsed);

            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dWyrmAgility Stats - ").append(webhookShowUser && user != null ? escapeJson(user) : "anonymous").append("\",")
                    .append("\"color\":15844367,");

            if (webhookShowStats) {
                json.append("\"fields\":[")
                        .append("{\"name\":\"XP Gained\",\"value\":\"").append(f.format(xpGained)).append("\",\"inline\":true},")
                        .append("{\"name\":\"XP/hr\",\"value\":\"").append(f.format(xpPerHour)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Laps Done\",\"value\":\"").append(f.format(lapCount)).append("\",\"inline\":true},")
                        .append("{\"name\":\"Laps/hr\",\"value\":\"").append(f.format(lapsPerHour)).append("\",\"inline\":true},")
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
            log("WEBHOOK", (code == 200 || code == 204) ? "✅ Sent webhook successfully." : "⚠ Failed to send webhook: HTTP " + code);
        } catch (Exception e) {
            log("WEBHOOK", "❌ Error sending webhook: " + e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return text == null ? "" : text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatRuntime(long ms) {
        long s = ms / 1000;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    private void checkForUpdates() {
        try {
            String urlRaw = "https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dWyrmAgility/src/main/java/main/dWyrmAgility.java";
            String latest = getLatestVersion(urlRaw);
            if (latest == null) {
                log("UPDATE", "⚠ Could not fetch latest version info.");
                return;
            }
            if (compareVersions(scriptVersion, latest) < 0) {
                log("UPDATE", "⏬ New version v" + latest + " found! Updating...");
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                for (File f : dir.listFiles((d, n) -> n.startsWith("dWyrmAgility"))) {
                    if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());
                }

                File out = new File(dir, "dWyrmAgility-" + latest + ".jar");
                URL jarUrl = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dWyrmAgility/jar/dWyrmAgility.jar");
                try (InputStream in = jarUrl.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }
                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();
            } else {
                log("SCRIPTVERSION", "✅ You are on the latest version (v" + scriptVersion + ").");
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

    private int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}
