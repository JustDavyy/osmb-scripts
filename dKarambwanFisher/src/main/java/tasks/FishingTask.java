package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;
import com.osmb.api.script.Script;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import com.osmb.api.visual.PixelAnalyzer;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static main.dKarambwanFisher.*;

public class FishingTask extends Task {
    private final WorldPosition fishingTile = new WorldPosition(2899, 3119, 0);
    private long lastAnimationDetected = System.currentTimeMillis();
    private long currentIdleThreshold = getRandomIdleThreshold();
    private final PixelAnalyzer pixelAnalyzer = script.getPixelAnalyzer();

    public FishingTask(Script script) {
        super(script);
    }

    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.RAW_KARAMBWANJI, ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159));
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        // Activate if 1. Our inventory is not full and 2. If we're within the fishing area
        return !inventorySnapshot.isFull() && fishingArea.contains(script.getWorldPosition()) && inventorySnapshot.contains(ItemID.RAW_KARAMBWANJI) && inventorySnapshot.containsAny(ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159);
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        // Get player tile to use for animation check
        Polygon playerTile = script.getSceneProjector().getTileCube(script.getWorldPosition(), 120);
        if (playerTile == null) {
            script.log(getClass().getSimpleName(), "Player tile is invalid/null");
            return false;
        }
        task = "Check animation";
        // Check for animation around our current tile, scaled by factor 0.7 (same ad debug tool)
        Polygon playerTileResized = playerTile.getResized(0.7);
        boolean isAnimating = pixelAnalyzer.isAnimating(0.4, playerTileResized);

        if (isAnimating) {
            lastAnimationDetected = System.currentTimeMillis(); // reset animation timer
            script.submitHumanTask(this::earlyExitCheck, script.random(10000, 15000));
            return false;
        }

        long idleTime = System.currentTimeMillis() - lastAnimationDetected;

        if (idleTime >= currentIdleThreshold) {
            script.log(getClass().getSimpleName(), "No animation detected for " + idleTime + "ms. Re-initiating fishing.");
            boolean initiated = initiateFishingAction();
            if (initiated) {
                lastAnimationDetected = System.currentTimeMillis(); // reset timer
                currentIdleThreshold = getRandomIdleThreshold();    // randomize next threshold
            }
            return initiated;
        }

        // switch tabs randomly to prevent afk log
        if (switchTabTimer.hasFinished()) {
            script.log("PREVENT-LOG", "Switching tabs...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.values()[script.random(Tab.Type.values().length)]);
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        }

        return false;
    }

    private boolean earlyExitCheck() {
        task = "Monitor wait condition";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return true;
        }

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        if (inventorySnapshot.isFull()) {
            script.log(getClass().getSimpleName(), "Early exit, Inventory is full");
            return true;
        }

        // Check if the player hasn't animated in the configured idle threshold
        Polygon playerTile = script.getSceneProjector().getTileCube(script.getWorldPosition(), 120);
        if (playerTile == null) {
            script.log(getClass().getSimpleName(), "Player tile is invalid/null during early exit check");
            return false;
        }

        Polygon playerTileResized = playerTile.getResized(0.7);
        boolean isAnimating = pixelAnalyzer.isAnimating(0.4, playerTileResized);

        if (isAnimating) {
            lastAnimationDetected = System.currentTimeMillis(); // refresh animation timer
            return false;
        }

        long idleTime = System.currentTimeMillis() - lastAnimationDetected;
        if (idleTime >= currentIdleThreshold) {
            script.log(getClass().getSimpleName(), "Early exit due to inactivity for " + idleTime + "ms.");
            return true;
        }

        return false;
    }

    private boolean initiateFishingAction() {
        task = "Initiate fishing action";
        // Get the tile
        RSTile fishingRSTile = script.getSceneManager().getTile(fishingTile);
        if (fishingRSTile == null) {
            script.log(getClass().getSimpleName(), "Fishing tile is null, failed to interact with it.");
            return false;
        }

        // Check if the tile is on our game screen
        if (!fishingRSTile.isOnGameScreen()) {
            task = "Move to fishing tile";
            script.log(getClass().getSimpleName(), "Fishing tile not on screen, walking...");
            script.getWalker().walkTo(fishingTile);
            return false;
        }

        // Select the fish option on the tile and return if successful or not
        script.log(getClass(), "Interacting with fishing tile");
        if (!script.getFinger().tap(fishingRSTile.getTileCube(0).getResized(0.75), "Fish")) {
            script.log(getClass().getSimpleName(), "Failed to long-press 'Fish' on fishing tile.");
            return false;
        } else {
            script.submitHumanTask(() -> false, script.random(1500, 2500));
            return true;
        }
    }

    private long getRandomIdleThreshold() {
        return ThreadLocalRandom.current().nextLong(8_000, 13_000); // 8 to 13 sec
    }
}
