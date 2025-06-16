package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import data.FishingSpot;
import utils.Task;
import com.osmb.api.script.Script;

import java.awt.Color;
import java.util.*;
import java.util.List;

import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class Fish extends Task {
    private static final SearchablePixel[] FISHING_SPOT_PIXELS = new SearchablePixel[]{
            new SearchablePixel(-11366999, new SingleThresholdComparator(2), ColorModel.HSL),
    };
    private FishingSpot lastFishingSpot = null;

    public Fish(Script script) {
        super(script);
    }

    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getRequiredTools()));
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        if (!inventorySnapshot.containsAll(Set.copyOf(fishingMethod.getRequiredTools()))) {
            script.log(getClass().getSimpleName(), "Not all required tools (rods/feathers etc) could be located in inventory, stopping script!");
            script.stop();
            return false;
        }

        WorldPosition myPos = script.getWorldPosition();

        if (myPos != null) {
            return !inventorySnapshot.isFull()
                    && fishingLocation.getFishingArea().contains(script.getWorldPosition())
                    && inventorySnapshot.containsAll(Set.copyOf(fishingMethod.getRequiredTools()));
        }

        return false;
    }

    public boolean execute() {
        task = getClass().getSimpleName();

        if (alreadyCountedFish) {
            alreadyCountedFish = false;
        }

        if (!isCurrentlyFishing()) {
            return initiateFishingAction();
        }

        // We're currently fishing; monitor exit conditions
        script.submitHumanTask(this::earlyExitCheck, script.random(6000, 7000));

        return false;
    }

    private boolean isCurrentlyFishing() {
        task = "Check for level-up";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return false;
        }

        if (!readyToReadFishingXP) {
            List<Integer> fishIds = fishingMethod.getCatchableFish();
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getCatchableFish()));
            if (inv != null) {
                for (int id : fishIds) {
                    if (inv.getAmount(id) > 0) {
                        script.log(getClass(), "Initial fish catch detected in inventory, setting readyToReadFishingXP = true");
                        readyToReadFishingXP = true;
                        break;
                    }
                }
            }
        }

        task = "Check last fishing spot";
        if (lastFishingSpot == null) return false;

        boolean stillValid = isValidFishingSpot(lastFishingSpot.getPosition()) != null;
        task = "Check XP Gained";
        boolean xpRecently = !readyToReadFishingXP || System.currentTimeMillis() - lastXpGained <= 20000;

        if (readyToReadFishingXP) {
            readFishingXp();
        }

        task = "Check if currently fishing";
        return stillValid && xpRecently;
    }

    private boolean earlyExitCheck() {
        task = "Check for level-up";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return true;
        }

        task = "Check AFK timer";
        if (switchTabTimer.hasFinished()) {
            script.log("PREVENT-LOG", "Switching tabs...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.values()[script.random(Tab.Type.values().length)]);
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        }

        if (!readyToReadFishingXP) {
            List<Integer> fishIds = fishingMethod.getCatchableFish();
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getCatchableFish()));
            if (inv != null) {
                for (int id : fishIds) {
                    if (inv.getAmount(id) > 0) {
                        script.log(getClass(), "Initial fish catch detected in inventory, setting readyToReadFishingXP = true");
                        readyToReadFishingXP = true;
                        break;
                    }
                }
            }
        }

        task = "Check inventory";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        if (inventorySnapshot.isFull()) {
            script.log(getClass().getSimpleName(), "Early exit, Inventory is full");
            return true;
        }

        task = "Check last fishing spot";
        if (lastFishingSpot != null) {
            FishingSpot current = isValidFishingSpot(lastFishingSpot.getPosition());
            if (current == null) {
                script.log(getClass(), "Fishing spot no longer valid — early exit.");
                return true;
            }
        }

        task = "Check XP gained";
        long idleSinceXp = System.currentTimeMillis() - lastXpGained;
        if (readyToReadFishingXP && idleSinceXp > 20000) {
            script.log(getClass(), "No XP gained in last 20s — early exit.");
            return true;
        }

        if (readyToReadFishingXP) {
            readFishingXp();
        }
        task = "Check if currently fishing";
        return false;
    }

    private boolean initiateFishingAction() {
        task = "Initiate fishing action";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            return false;
        }

        FishingSpot fishingSpot = getFishingSpot(myPosition);
        if (fishingSpot != null) {
            lastFishingSpot = fishingSpot;
            lastXpGained = System.currentTimeMillis();
            return attemptFishing(fishingSpot);
        }

        script.log(getClass(), "No visible fishing spots, walking to another area...");
        script.getWalker().walkTo(getDestination(), new WalkConfig.Builder().disableWalkScreen(true).breakDistance(5).build());

        fishingSpot = getFishingSpot(script.getWorldPosition());
        if (fishingSpot != null) {
            lastFishingSpot = fishingSpot;
            lastXpGained = System.currentTimeMillis();
            return attemptFishing(fishingSpot);
        } else {
            script.log(getClass(), "Still no fishing spots after relocating.");
        }

        return false;
    }

    private boolean interactWithFishingSpot(FishingSpot fishingSpot) {
        script.log(getClass(), "Interacting with fishing spot at " + fishingSpot.getPosition());

        boolean clicked = script.getFinger().tap(fishingSpot.getFishingSpotPoly(), menuHook);
        if (!clicked) {
            script.log(getClass(), "Failed to click fishing spot.");
            lastFishingSpot = null;
            return false;
        }

        lastXpGained = System.currentTimeMillis();
        return true;
    }

    private boolean attemptFishing(FishingSpot fishingSpot) {
        RSTile spotTile = script.getSceneManager().getTile(fishingSpot.getPosition());

        if (spotTile != null && spotTile.isOnGameScreen()) {
            script.log(getClass(), "Fishing spot is on game screen, interacting directly.");
            return interactWithFishingSpot(fishingSpot);
        } else {
            script.log(getClass(), "Fishing spot is not on screen, walking to it...");
            WalkConfig.Builder walkConfig = new WalkConfig.Builder();
            walkConfig.breakCondition(() -> {
                RSTile t = script.getSceneManager().getTile(fishingSpot.getPosition());
                return t != null && t.isOnGameScreen();
            });
            script.getWalker().walkTo(fishingSpot.getPosition(), walkConfig.build());
            lastXpGained = System.currentTimeMillis() - 21000;
        }

        return false;
    }

    private FishingSpot getFishingSpot(WorldPosition worldPosition) {
        long start = System.currentTimeMillis();

        // Get the pre-defined fishing spots for our location
        Area spotArea = fishingLocation.getFishingArea();
        Set<WorldPosition> fishingSpotPositions = fishingMethod.getFishingSpots();

        List<FishingSpot> activeFishingSpotsOnScreen = getFishingSpots(fishingSpotPositions);
        if (activeFishingSpotsOnScreen.isEmpty()) {
            return null;
        }

        long end = System.currentTimeMillis();
        script.log(getClass(), "Fishing spots found in: " + (end - start) + "ms");

        FishingSpot closest = activeFishingSpotsOnScreen.get(0);
        for (FishingSpot spot : activeFishingSpotsOnScreen) {
            if (spot.getPosition().distanceTo(worldPosition) < closest.getPosition().distanceTo(worldPosition)) {
                closest = spot;
            }
        }

        FishingSpot finalClosest = closest;
        script.getScreen().queueCanvasDrawable("activeFishingSpots", canvas -> {
            for (FishingSpot spot : activeFishingSpotsOnScreen) {
                Polygon poly = script.getSceneProjector().getTilePoly(spot.getPosition(), true);
                if (poly != null) {
                    canvas.fillPolygon(poly, spot == finalClosest ? Color.GREEN.getRGB() : Color.RED.getRGB(), 0.3);
                }
            }
        });

        return closest;
    }

    private List<FishingSpot> getFishingSpots(Set<WorldPosition> fishingSpotPositions) {
        List<FishingSpot> validFishingSpots = new ArrayList<>();
        for (WorldPosition fishingSpotPosition : fishingSpotPositions) {
            FishingSpot fishingSpot = isValidFishingSpot(fishingSpotPosition);
            if (fishingSpot != null) {
                validFishingSpots.add(fishingSpot);
            }
        }
        return validFishingSpots;
    }

    private FishingSpot isValidFishingSpot(WorldPosition worldPosition) {
        Polygon fishingSpotPoly = getFishingSpotPoly(worldPosition);
        if (fishingSpotPoly == null) {
            return null;
        }
        fishingSpotPoly.getResized(0.8);
        boolean containsFishingPixel = script.getPixelAnalyzer().findPixel(fishingSpotPoly, FISHING_SPOT_PIXELS) != null;
        if (!containsFishingPixel) {
            return null;
        }
        return new FishingSpot(worldPosition, fishingSpotPoly);
    }

    private Polygon getFishingSpotPoly(WorldPosition worldPosition) {
        Polygon tilePoly = script.getSceneProjector().getTilePoly(worldPosition, true);
        if (tilePoly == null) {
            return null;
        }
        if (!script.getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            return null;
        }
        return tilePoly;
    }

    private void walkToFishingSpot() {
        script.log(getClass(), "No fishing spots visible on screen, walking to one...");
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            // keep polling for fishing spots and break out as soon as one is visible
            FishingSpot fishingSpot1 = getFishingSpot(myPosition);
            return fishingSpot1 != null;
        });
        script.getWalker().walkTo(getDestination(), walkConfig.build());
    }

    private WorldPosition getDestination() {
        WorldPosition myPosition = script.getWorldPosition();

        if (myPosition == null) {
            script.log(getClass(), "Failed to get position, as it is null");
            return new WorldPosition(1, 1, 0);
        }

        List<Area> fishingSpots = fishingLocation.getFishingSpotAreas();
        if (fishingSpots.isEmpty()) {
            script.log(getClass(), "No fishing spot areas defined.");
            return myPosition;
        }

        // Determine if we're currently in any of the fishing areas
        Area currentArea = fishingSpots.stream()
                .filter(area -> area.contains(myPosition))
                .findFirst()
                .orElse(null);

        List<Area> candidates = fishingSpots.stream()
                .filter(area -> area != currentArea)
                .toList();

        if (candidates.isEmpty()) {
            // We're not inside any, or only one area is defined
            candidates = fishingSpots;
        }

        Area chosen = candidates.get(script.random(candidates.size()));
        return chosen.getRandomPosition();
    }

    private void readFishingXp() {
        task = "Read Fishing XP";
        XPDropsComponent xpComponent = (XPDropsComponent) script.getWidgetManager().getComponent(XPDropsComponent.class);

        if (xpComponent == null) {
            script.log(getClass(), "XP button component not found.");
            return;
        }

        ComponentSearchResult<Integer> result = xpComponent.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) return;

        Rectangle componentBounds = result.getBounds();
        Rectangle xpTextRect = new Rectangle(componentBounds.x - 140, componentBounds.y - 1, 119, 38);

        script.submitTask(() -> false, script.random(200, 400));
        String xpText = script.getOCR().getText(Font.SMALL_FONT, xpTextRect, Color.WHITE.getRGB());

        if (xpText == null || xpText.isBlank()) return;
        xpText = xpText.replaceAll("[^\\d]", "");
        if (xpText.isEmpty()) return;

        try {
            double currentXp = Double.parseDouble(xpText);
            if (currentXp <= 0) return;

            if (previousFishingXpRead < 0) {
                previousFishingXpRead = currentXp;
                return;
            }

            double xpGained = currentXp - previousFishingXpRead;
            if (xpGained > 0 && xpGained <= 15000) {
                fishingXp += xpGained;
                script.log(getClass(), "Fishing XP gained: " + xpGained + " (" + fishingXp + ")");
                previousFishingXpRead = currentXp;
                lastXpGained = System.currentTimeMillis();
            }

        } catch (NumberFormatException e) {
            script.log(getClass(), "Failed to parse Fishing XP text: " + xpText);
        }
    }
}
