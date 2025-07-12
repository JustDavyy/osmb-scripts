package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static main.dFossilWCer.*;

public class Chop extends Task {
    private int clusterFailCount = 0;

    private static final SearchablePixel[] MAHOGANY_PIXEL_CLUSTER = new SearchablePixel[]{
            new SearchablePixel(-11443436, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-15527164, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12432618, new SingleThresholdComparator(0), ColorModel.RGB),
            new SearchablePixel(-14603515, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-14342889, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-14605039, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-13946602, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10590171, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12563192, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12761064, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10063308, new SingleThresholdComparator(2), ColorModel.RGB)
    };

    private static final SearchablePixel[] TEAK_PIXEL_CLUSTER = new SearchablePixel[]{
            new SearchablePixel(-4794765, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5913750, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-4596873, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5189776, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8218541, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-6703774, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7362468, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-6308761, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5453202, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9205686, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8679345, new SingleThresholdComparator(2), ColorModel.HSL)
    };

    private static final Area choppingArea = new RectangleArea(3699, 3830, 20, 10, 0);
    private static final Area bankingArea = new RectangleArea(3708, 3797, 42, 21, 0);

    public Chop(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public boolean execute() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos != null && !choppingArea.contains(myPos)) {
            task = "Walk to chopping area";
            if (useShortcut) {
                return walkWithShortcut();
            } else {
                return walkWithoutShortcut();
            }
        }

        usedBasketAlready = false;

        SearchablePixel[] clusterToUse;

        task = "Select correct pixel cluster";
        // Choose cluster based on logsId
        if (logsId == ItemID.TEAK_LOGS) {
            clusterToUse = TEAK_PIXEL_CLUSTER;
        } else if (logsId == ItemID.MAHOGANY_LOGS) {
            clusterToUse = MAHOGANY_PIXEL_CLUSTER;
        } else {
            script.log(getClass(), "Invalid tree ID selected: " + logsId);
            return false;
        }

        task = "Build cluster query";
        // Build cluster query
        PixelCluster.ClusterQuery query = new PixelCluster.ClusterQuery(
                10,
                400,
                clusterToUse
        );

        long startTime = System.currentTimeMillis();

        task = "Perform cluster search";
        // Perform cluster search
        PixelCluster.ClusterSearchResult result = script.getPixelAnalyzer().findClusters(query);
        long elapsed = System.currentTimeMillis() - startTime;

        if (result == null || result.getClusters() == null) {
            script.log(getClass(), "No cluster search result returned. Time taken: " + elapsed + " ms.");
            return false;
        }

        List<PixelCluster> clusters = result.getClusters();

        if (clusters.isEmpty()) {
            script.log(getClass(), "No tree clusters found. Time taken: " + elapsed + " ms.");
            clusterFailCount++;

            if (clusterFailCount >= 3) {
                script.log(getClass(), "No clusters found 3 times in a row, closing inventory and retrying.");

                // Close inventory if open
                if (script.getWidgetManager().getInventory().isVisible()) {
                    if (script.getWidgetManager().getInventory().close()) {
                        script.log(getClass(), "Inventory closed to refresh view.");
                    } else {
                        script.log(getClass(), "Failed to close inventory.");
                    }
                }

                // Reset fail count after action
                clusterFailCount = 0;

                // Optional: wait a short random time before re-checking
                script.submitHumanTask(() -> false, script.random(400, 800));
            }

            return false;
        } else {
            clusterFailCount = 0;
            script.log(getClass(), "Found " + clusters.size() + " tree cluster(s). Time taken: " + elapsed + " ms.");

            // Find closest cluster to center of screen
            PixelCluster closest = clusters.stream()
                    .min((a, b) -> {
                        Point ca = a.getCenter();
                        Point cb = b.getCenter();
                        double da = Math.hypot(ca.x - centerX, ca.y - centerY);
                        double db = Math.hypot(cb.x - centerX, cb.y - centerY);
                        return Double.compare(da, db);
                    })
                    .orElse(null);

            if (closest != null) {
                Point center = closest.getCenter();
                script.log(getClass(), "Closest cluster center at: (" + center.x + ", " + center.y + ")");

                Rectangle bounds = closest.getBounds();
                if (bounds != null) {
                    script.log(getClass(), "Closest cluster original bounds: " + bounds);

                    // Calculate 20% trim
                    int trimX = (int) (bounds.width * 0.2);
                    int trimY = (int) (bounds.height * 0.2);

                    // Calculate new width and height
                    int newWidth = bounds.width - (trimX * 2);
                    int newHeight = bounds.height - (trimY * 2);

                    // Ensure width and height remain positive
                    if (newWidth <= 0 || newHeight <= 0) {
                        script.log(getClass(), "Trimmed bounds invalid (too small). Using original bounds.");
                    } else {
                        bounds = new Rectangle(
                                bounds.x + trimX,
                                bounds.y + trimY,
                                newWidth,
                                newHeight
                        );
                        script.log(getClass(), "Trimmed cluster bounds: " + bounds);
                    }

                    task = "Initiate chop action";
                    if (!script.getFinger().tap(bounds, "Chop")) {
                        return false;
                    }
                    waitUntilFinishedChopping();
                } else {
                    script.log(getClass(), "Closest cluster has no bounds.");
                }
            }
        }

        script.submitHumanTask(() -> false, script.random(400, 800));
        return true;
    }

    private void waitUntilFinishedChopping() {

        int maxChopDuration = script.random(240_000, 270_000);

        ItemGroupResult startSnapshot = script.getWidgetManager().getInventory().search(Set.of(logsId));
        if (startSnapshot == null) {
            script.log(getClass(), "Aborting chop check: could not read starting inventory.");
            return;
        }

        AtomicInteger previousCount = new AtomicInteger(startSnapshot.getAmount(logsId));
        Timer lastXpGain = new Timer();
        long start = System.currentTimeMillis();

        // === INITIAL: wait to let user start chopping ===
        script.submitHumanTask(() -> false, script.random(2750, 4000));

        // === Main monitoring loop ===
        script.submitHumanTask(() -> {
            boolean gainedXP = false;
            if (readyToReadXP) {
                gainedXP = readXp();
            }

            // === Inventory check ===
            ItemGroupResult currentInv = script.getWidgetManager().getInventory().search(Set.of(logsId));
            if (currentInv == null) {
                script.log(getClass(), "Chop stopped: inventory became inaccessible.");
                return true;
            }
            if (currentInv.isFull()) {
                script.log(getClass(), "Chop stopped: inventory is full.");
                return true;
            }

            // === Dialogue check for level up ===
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log(getClass(), "Dialogue detected, leveled up?");
                script.submitHumanTask(() -> false, script.random(1000, 3000));
                return true;
            }

            // === Inventory count tracking ===
            int currentCount = currentInv.getAmount(logsId);
            int lastCount = previousCount.get();
            if (currentCount > lastCount) {
                int gained = currentCount - lastCount;
                previousCount.set(currentCount);
                logsChopped += gained;
                if (gainedXP) lastXpGain.reset();
                script.log(getClass(), "+" + gained + " logs chopped! (" + logsChopped + " total)");
                if (!readyToReadXP) readyToReadXP = true;
            } else if (currentCount < lastCount) {
                script.log(getClass(), "Detected log count drop (from " + lastCount + " to " + currentCount + "). Syncing.");
                previousCount.set(currentCount);
            } else {
                if (gainedXP) lastXpGain.reset();

                // === If using log basket and readyToReadXP is false, schedule it after 29s ===
                if (useLogBasket && !readyToReadXP) {
                    script.submitHumanTask(() -> {
                        readyToReadXP = true;
                        return false;
                    }, script.random(12000, 15000));
                }
            }

            // === Check for nearby tree cluster presence ===
            PixelCluster.ClusterQuery query = new PixelCluster.ClusterQuery(10, 400,
                    logsId == ItemID.TEAK_LOGS ? TEAK_PIXEL_CLUSTER : MAHOGANY_PIXEL_CLUSTER);

            PixelCluster.ClusterSearchResult clusterResult = script.getPixelAnalyzer().findClusters(query);

            if (clusterResult == null || clusterResult.getClusters() == null) {
                return false;
            }

            boolean clusterNearby = clusterResult.getClusters().stream()
                    .anyMatch(c -> {
                        Point p = c.getCenter();
                        double distance = Math.hypot(p.x - centerX, p.y - centerY);
                        return distance <= 125;
                    });

            if (!clusterNearby) {
                script.log(getClass(), "Screen center: (" + centerX + ", " + centerY + ")");
                script.log(getClass(), "Chop stopped: no tree cluster detected within 125px of screen center.");
                return true;
            }

            // === Duration / XP timeout check ===
            long elapsed = System.currentTimeMillis() - start;
            boolean noXpTooLong = lastXpGain.timeElapsed() > 30_000;

            if (elapsed > maxChopDuration) {
                script.log(getClass(), "Chop stopped: exceeded max chop duration.");
                return true;
            }

            if (noXpTooLong) {
                script.log(getClass(), "Chop stopped: no XP gain for " + lastXpGain.timeElapsed() + "ms.");
                return true;
            }

            return false;
        }, maxChopDuration);

        script.submitHumanTask(() -> false, script.random(300, 800));
    }

    private boolean readXp() {
        task = "Read XP";
        XPDropsComponent xpComponent = (XPDropsComponent) script.getWidgetManager().getComponent(XPDropsComponent.class);

        if (xpComponent == null) {
            script.log(getClass(), "XP button component not found.");
            return false;
        }

        ComponentSearchResult<Integer> result = xpComponent.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) return false;

        Rectangle componentBounds = result.getBounds();
        Rectangle xpTextRect = new Rectangle(componentBounds.x - 140, componentBounds.y - 1, 119, 38);

        script.submitTask(() -> false, script.random(200, 400));
        String xpText = script.getOCR().getText(Font.SMALL_FONT, xpTextRect, Color.WHITE.getRGB());

        if (xpText == null || xpText.isBlank()) return false;
        xpText = xpText.replaceAll("[^\\d]", "");
        if (xpText.isEmpty()) return false;

        try {
            double currentXp = Double.parseDouble(xpText);
            if (currentXp <= 0) return false;

            if (previousXPRead < 0) {
                previousXPRead = currentXp;
                return false;
            }

            double xpGained = currentXp - previousXPRead;
            if (xpGained > 0 && xpGained <= 15000) {
                totalXPGained += xpGained;
                script.log(getClass(), "Woodcutting XP gained: " + xpGained + " (" + totalXPGained + ")");
                previousXPRead = currentXp;
                return true;
            }

        } catch (NumberFormatException e) {
            script.log(getClass(), "Failed to parse Fishing XP text: " + xpText);
        }

        return false;
    }

    private boolean walkWithShortcut() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        if (bankingArea.contains(myPos)) {
            task = "Use shortcut";

            // Search for Hole object at base tile 3712, 3828 with action "Climb-through"
            List<RSObject> holes = script.getObjectManager().getObjects(obj -> {
                if (obj.getName() == null || obj.getActions() == null) {
                    return false;
                }
                return obj.getName().equals("Hole")
                        && Arrays.asList(obj.getActions()).contains("Climb through")
                        && obj.getWorldPosition().getX() == 3714
                        && obj.getWorldPosition().getY() == 3816;
            });

            if (holes.isEmpty()) {
                script.log(getClass(), "No Hole object found at base tile (3714,3816).");
                return false;
            }

            RSObject hole = (RSObject) script.getUtils().getClosest(holes);
            if (hole == null) {
                script.log(getClass(), "Closest Hole object is null.");
                return false;
            }

            // Walk to hole if not interactable on screen
            if (!hole.isInteractableOnScreen()) {
                script.log(getClass(), "Hole not on screen, walking closer...");
                WalkConfig config = new WalkConfig.Builder()
                        .breakCondition(hole::isInteractableOnScreen)
                        .enableRun(true)
                        .build();
                script.getWalker().walkTo(hole.getWorldPosition(), config);
                return false; // Re-poll once it's on screen
            }

            // Interact with Hole
            task = "Climbing through hole";
            if (!hole.interact("Climb through")) {
                script.log(getClass(), "Failed to climb through Hole.");
                return false;
            }

            return script.submitHumanTask(() -> {
                WorldPosition currentPos = script.getWorldPosition();
                return currentPos != null && !bankingArea.contains(currentPos);
            }, script.random(7000, 12000));
        } else {
            return walkWithoutShortcut();
        }
    }

    private boolean walkWithoutShortcut() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        task = "Walk to chopping area";
        script.getWalker().walkTo(choppingArea.getRandomPosition());

        WorldPosition currentPos = script.getWorldPosition();

        script.submitHumanTask(() -> currentPos != null && choppingArea.contains(currentPos), script.random(600, 1200));
        return true;
    }
}