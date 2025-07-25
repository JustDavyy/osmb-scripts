package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static main.dCannonballSmelter.*;

public class ProcessTask extends Task {
    private final long startTime;

    public ProcessTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD, ItemID.STEEL_BAR));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        return inventorySnapshot.containsAny(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD)) && inventorySnapshot.contains(ItemID.STEEL_BAR);
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.STEEL_BAR));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        task = "Get furnace object";
        RSObject furnace = getClosestFurnace();
        if (furnace == null) {
            script.log(getClass(), "No furnace found nearby.");
            return false;
        }

        task = "Interact furnace object";
        if (!furnace.interact("Smelt")) {
            script.log(getClass(), "Failed to interact with furnace. Retrying...");
            if (!furnace.interact("Smelt")) {
                return false;
            }
        }

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        task = "Wait for dialogue";
        script.submitHumanTask(condition, script.random(4000, 6000));

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(ItemID.CANNONBALL);
            if (!selected) {
                task = "Retry interaction";
                script.log(getClass(), "Initial cannonball selection failed, retrying...");
                script.submitHumanTask(() -> false, script.random(150, 300));
                selected = script.getWidgetManager().getDialogue().selectItem(ItemID.CANNONBALL);
            }

            if (!selected) {
                script.log(getClass(), "Failed to select cannonballs in dialogue after retry.");
                return false;
            }
            script.log(getClass(), "Selected cannonballs to smelt.");

            task = "Wait until finished";
            waitUntilFinishedSmelting();

            task = "Update stats";
            int smeltedNow = inventorySnapshot.getAmount(ItemID.STEEL_BAR);
            smeltCount += smeltedNow;
            totalXpGained += smeltedNow * getXpForCannonball();
            printStats();
        }

        return false;
    }

    private RSObject getClosestFurnace() {
        List<RSObject> objects = script.getObjectManager().getObjects(obj ->
                obj.getName() != null &&
                        (obj.getName().equalsIgnoreCase("Furnace") || obj.getName().equalsIgnoreCase("Clay forge")
                      || obj.getName().equalsIgnoreCase("Lava forge") || obj.getName().equalsIgnoreCase("Volcanic Furnace")) &&
                        obj.canReach()
        );

        if (objects.isEmpty()) {
            script.log(ProcessTask.class, "No reachable furnaces found.");
            return null;
        }

        return (RSObject) script.getUtils().getClosest(objects);
    }

    private void waitUntilFinishedSmelting() {
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            // This is the level level up check
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log(getClass().getSimpleName(), "Dialogue detected, leveled up?");
                script.submitHumanTask(() -> false, script.random(1000, 3000));
                return true;
            }

            // Force AFK during these checks if we're due a AFK
            if (script.getProfileManager().isDueToAFK()) {
                script.log(getClass().getSimpleName(), "We are due to AFK during processing, forcing AFK now.");
                script.getProfileManager().forceAFK();
            }

            // A timer to timeout
            if (amountChangeTimer.timeElapsed() > script.random(162500, 166000)) {
                return true;
            }

            // Check if we ran out of items
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.STEEL_BAR));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.contains(ItemID.STEEL_BAR);
        };

        script.log(getClass(), "Using human task to wait until smelting finishes.");
        script.submitHumanTask(condition, script.random(162500, 166000));
    }

    private void printStats() {
        task = "Print stats";
        long elapsed = System.currentTimeMillis() - startTime;
        int smeltsPerHour = (int) ((smeltCount * 3600000L) / elapsed);
        int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);
        int ballsSmelted = smeltCount * 4;
        int ballsPerHour = (int) ((ballsSmelted * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Cballs smelted: %,d | Smelts/hr: %,d | Cballs/hr: %,d | XP gained: %,d | XP/hr: %,d",
                ballsSmelted, smeltsPerHour, ballsPerHour, (int) totalXpGained, xpPerHour
        ));
    }

    private double getXpForCannonball() {
        return 25.6;
    }
}