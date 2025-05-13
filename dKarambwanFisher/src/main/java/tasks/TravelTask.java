package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Equipment;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dKarambwanFisher.*;

public class TravelTask extends Task {
    private final Area zanarisBankArea = new RectangleArea(2381, 4454, 7, 7, 0);
    private final Area craftingGuildBankArea = new RectangleArea(2928, 3275, 16, 17, 0);
    private final Area zanarisFairyRingArea = new RectangleArea(2408, 4431, 8, 6, 0);
    private final Area zanarisArea = new RectangleArea(2375, 4419, 64, 48, 0);
    private final Area legendsArea = new RectangleArea(2709, 3334, 40, 34, 0);
    private final Area legendsFairyArea = new RectangleArea(2736, 3347, 7, 7, 0);
    private final Area monasteryArea = new RectangleArea(2584, 3219, 87, 24, 0);
    private final Area monasteryFairyArea = new RectangleArea(2653, 3226, 10, 9, 0);

    public static final String[] FAIRY_NAMES = {"Fairy ring"};
    public static final String[] FAIRY_ACTIONS = {"zanaris", "configure"};
    public static final Predicate<RSObject> fairyRingQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(FAIRY_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(FAIRY_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    private ItemGroupResult inventorySnapshot;

    public TravelTask(Script script) {
        super(script);
    }

    public boolean activate() {
        inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        return (inventorySnapshot.isFull() && !withinBankArea()) || doneBanking || !withinFishArea() && !withinBankArea();
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        // Handle if we're within the fishing area still
        if (fishingArea.contains(currentPos)) {
            if (bankOption.equals("Zanaris")) {
                task = "Travel to zanaris";
                script.log(getClass().getSimpleName(), "Traveling to zanaris with fairy ring");
                return handleFishingFairyRing();
            }
            if (bankOption.equals("Crafting Guild")) {
                task = "Travel to craft guild";
                script.log(getClass().getSimpleName(), "Traveling to Crafting Guild with " + script.getItemManager().getItemName(teleportCapeId));
                return handleFishingCraftingCape();
            } else {
                script.log(getClass().getSimpleName(), "Invalid bank: " + bankOption);
            }
        }

        // Handle if we're within a bank and have teleport cloak options
        if (withinBankArea()) {
            if (fairyOption.equals("Quest cape") || fairyOption.equals("Ardougne cloak")) {
                task = "Use cape teleport";
                Equipment equipment = script.getWidgetManager().getEquipment();
                String menuOption = fairyOption.equals("Ardougne cloak") ? "Kandarin Monastery" : "Teleport";
                Area destinationArea = fairyOption.equals("Ardougne cloak") ? monasteryArea : legendsArea;

                if (equipment.interact(equippedCloakId, menuOption)) {
                    script.log(getClass().getSimpleName(), "Teleporting using " + script.getItemManager().getItemName(equippedCloakId));

                    if (arrivedAtArea(destinationArea)) {
                        script.log(getClass().getSimpleName(), "Teleport was successful");
                        doneBanking = false;
                        script.log(getClass().getSimpleName(), "Marked banking flag to false.");
                    }
                }

                return false;
            } else {
                script.log(getClass().getSimpleName(), "Not using an equipped teleport cape during this run...");
            }
        }

        // Handle if we're at the zanaris bank (we can only get here with the doneBanking flag)
        if (zanarisBankArea.contains(currentPos)) {
            task = "Travel to zanaris fairy";
            script.log(getClass().getSimpleName(), "Walking to zanaris fairy ring area from bank area");

            if (script.getWalker().walkTo(zanarisFairyRingArea.getRandomPosition())) {
                doneBanking = false;
            }
            return false;
        }

        // Handle if we're at the zanaris fairy ring area
        if (zanarisFairyRingArea.contains(currentPos) && !inventorySnapshot.isFull()) {
            task = "Fairy to fishing area";
            script.log(getClass().getSimpleName(), "Traveling to Fishing area from Zanaris Fairy Ring");
            return handleOtherFairyRing();
        }

        // Handle if we're within the zanaris area
        if (zanarisArea.contains(currentPos) ) {
            if (inventorySnapshot.isFull()) {
                task = "Travel to bank";
                script.log(getClass().getSimpleName(), "Walking to zanaris bank area");
                return script.getWalker().walkTo(zanarisBankArea.getRandomPosition());
            } else {
                task = "Travel to fairy ring";
                script.log(getClass().getSimpleName(), "Walking to zanaris fairy ring area");
                return script.getWalker().walkTo(zanarisFairyRingArea.getRandomPosition());
            }
        }

        // Handle if we're at the Legends guild fairy ring area
        if (legendsFairyArea.contains(currentPos)) {
            task = "Travel to fishing area";
            script.log(getClass().getSimpleName(), "Traveling to Fishing area from Legends Fairy Ring");
            return handleOtherFairyRing();
        }

        // Handle if we're at the Legends guild area
        if (legendsArea.contains(currentPos)) {
            task = "Travel to fairy ring";
            script.log(getClass().getSimpleName(), "Traveling to Legends Fairy Ring");
            return script.getWalker().walkTo(legendsFairyArea.getRandomPosition());
        }

        // Handle if we're at the monastery fairy ring area
        if (monasteryFairyArea.contains(currentPos)) {
            task = "Travel to fishing area";
            script.log(getClass().getSimpleName(), "Traveling to Fishing area from Monastery Fairy Ring");
            return handleOtherFairyRing();
        }

        // Handle if we're at the monastery area
        if (monasteryArea.contains(currentPos)) {
            task = "Travel to fairy ring";
            script.log(getClass().getSimpleName(), "Traveling to Monastery Fairy Ring");
            return script.getWalker().walkTo(monasteryFairyArea.getRandomPosition());
        }

        return false;
    }

    private boolean arrivedAtArea(Area destination) {
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.submitHumanTask(() -> {
            currentPos = script.getWorldPosition();
            if (currentPos == null) return false;

            if (!Objects.equals(currentPos, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(currentPos);
            }

            return destination.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 10000;
        }, script.random(14000, 16000));
        return destination.contains(currentPos);
    }

    private boolean handleFishingFairyRing() {
        // Get the fairy ring object
        List<RSObject> ringsFound = script.getObjectManager().getObjects(fairyRingQuery);
        if (ringsFound.isEmpty()) {
            script.log(getClass().getSimpleName(), "No fairy ring objects found.");
            return false;
        }

        RSObject ring = (RSObject) script.getUtils().getClosest(ringsFound);
        if (!ring.interact("zanaris")) {
            script.log(getClass().getSimpleName(), "Failed to interact with fairy ring object.");
            return false;
        }

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.submitHumanTask(() -> {
            currentPos = script.getWorldPosition();
            if (currentPos == null) return false;

            if (!Objects.equals(currentPos, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(currentPos);
            }

            return zanarisFairyRingArea.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 10000;
        }, script.random(14000, 16000));
        return zanarisFairyRingArea.contains(currentPos);
    }

    private boolean handleFishingCraftingCape() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (inventorySnapshot.contains(teleportCapeId)) {
            if (!inventorySnapshot.getItem(teleportCapeId).interact("Teleport")) {
                script.log(getClass().getSimpleName(), "Failed to teleport using the crafting cape in our inventory.");
                return false;
            }

            // Interaction seems successful, wait till we arrive at the guild
            AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);
            script.submitHumanTask(() -> {
                currentPos = script.getWorldPosition();
                if (currentPos == null) return false;

                if (!Objects.equals(currentPos, previousPosition.get())) {
                    positionChangeTimer.get().reset();
                    previousPosition.set(currentPos);
                }

                return craftingGuildBankArea.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 10000;
            }, script.random(14000, 16000));
        } else {
            script.log(getClass().getSimpleName(), "It seems the crafting cape is not in our inventory? Stopping script!");
            script.stop();
            return false;
        }

        // Return if we are within the guild or not at last
        return craftingGuildBankArea.contains(currentPos);
    }

    private boolean handleOtherFairyRing() {
        // Get the fairy ring object
        List<RSObject> ringsFound = script.getObjectManager().getObjects(fairyRingQuery);
        if (ringsFound.isEmpty()) {
            script.log(getClass().getSimpleName(), "No fairy ring objects found.");
            return false;
        }

        RSObject ring = (RSObject) script.getUtils().getClosest(ringsFound);
        if (!ring.interact("last-destination (dkp)")) {
            script.log(getClass().getSimpleName(), "Failed to interact with fairy ring object.");
            return false;
        }

        // Reset afk timer
        switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.submitHumanTask(() -> {
            currentPos = script.getWorldPosition();
            if (currentPos == null) return false;

            if (!Objects.equals(currentPos, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(currentPos);
            }

            return fishingArea.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 15000;
        }, script.random(20000, 25000));
        return fishingArea.contains(currentPos);
    }

    private boolean withinBankArea() {
        // Get our position
        currentPos = script.getWorldPosition();

        // Check Zanaris bank
        if (zanarisBankArea.contains(currentPos)) {
            return true;
        }

        // Check Crafting Guild bank
        if (craftingGuildBankArea.contains(currentPos)) {
            return true;
        }

        // If no matches, return false as we are not within a bank area
        return false;
    }

    private boolean withinFishArea() {
        // Get our position
        currentPos = script.getWorldPosition();

        return fishingArea.contains(currentPos);
    }
}
