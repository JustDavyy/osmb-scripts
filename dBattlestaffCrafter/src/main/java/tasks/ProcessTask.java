package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;
import java.util.function.BooleanSupplier;

import static main.dBattlestaffCrafter.*;

public class ProcessTask extends Task {
    private long startTime = 0;
    private ItemGroupResult inventorySnapshot;

    public ProcessTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean activate() {
        return !shouldBank;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        int orbId = getOrbIdForStaff(staffID);
        if (orbId == -1) {
            script.log(getClass().getSimpleName(), "Unknown orb for staff ID: " + staffID + ". Stopping script.");
            script.stop();
            return false;
        }

        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(orbId, ItemID.BATTLESTAFF));

        task = "Check items";
        if (!inventorySnapshot.contains(orbId) || !inventorySnapshot.contains(ItemID.BATTLESTAFF)) {
            script.log(getClass().getSimpleName(), "Missing battlestaffs or orbs. Flagging bank.");
            shouldBank = true;
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        task = "Start crafting";
        boolean interacted = interactAndWaitForDialogue(inventorySnapshot.getRandomItem(orbId), inventorySnapshot.getRandomItem(ItemID.BATTLESTAFF));

        if (!interacted) {
            task = "Failed interaction";
            script.log(getClass().getSimpleName(), "Failed to interact with orb and staff.");
            return false;
        }

        task = "Wait for dialogue";
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(staffID);
            if (!selected) {
                script.log(getClass().getSimpleName(), "Initial staff selection failed, retrying...");
                script.submitHumanTask(() -> false, script.random(150, 300));
                selected = script.getWidgetManager().getDialogue().selectItem(staffID);
            }

            if (!selected) {
                script.log(getClass().getSimpleName(), "Failed to select staff in dialogue after retry.");
                return false;
            }
            script.log(getClass().getSimpleName(), "Selected battlestaff to craft.");

            task = "Wait until finished";
            waitUntilFinishedProducing();
            if (script.random(10) < 3) {
                script.log(getClass().getSimpleName(), "Adding extra randomized delay");
                script.submitHumanTask(() -> false, script.random(250, 1200));
            }
            craftedCount += 14;
            printStats();
        }

        return false;
    }

    private boolean interactAndWaitForDialogue(ItemSearchResult orb, ItemSearchResult staff) {
        if (!orb.interact()) {
            script.log(getClass(), "Orb interaction failed, retrying...");
            if (!orb.interact()) {
                return false;
            }
        }

        script.submitHumanTask(() -> false, script.random(25, 50));

        if (!staff.interact()) {
            script.log(getClass(), "Staff interaction failed, retrying...");
            if (!staff.interact()) {
                return false;
            }
        }

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        return script.submitHumanTask(condition, 3000);
    }

    private void waitUntilFinishedProducing() {
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.submitHumanTask(() -> false, script.random(500, 2500));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 18000) {
                return true;
            }

            inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(getOrbIdForStaff(staffID), ItemID.BATTLESTAFF));
            return !inventorySnapshot.containsAny(getOrbIdForStaff(staffID), ItemID.BATTLESTAFF);
        };

        script.log(getClass(), "Using human task to wait until crafting finishes.");
        script.submitHumanTask(condition, script.random(18000, 20000));
    }

    private void printStats() {
        task = "Print stats";
        long elapsed = System.currentTimeMillis() - startTime;
        int craftsPerHour = (int) ((craftedCount * 3600000L) / elapsed);

        double xpPerCraft = getXPForStaff(staffID);
        totalXpGained = (int) (craftedCount * xpPerCraft);
        int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Staffs crafted: %d | Staffs/hr: %,d | XP gained: %,d | XP/hr: %,d",
                craftedCount, craftsPerHour, (int) totalXpGained, xpPerHour
        ));
    }

    private double getXPForStaff(int staffId) {
        return switch (staffId) {
            case ItemID.AIR_BATTLESTAFF -> 137.5;
            case ItemID.WATER_BATTLESTAFF -> 100.0;
            case ItemID.EARTH_BATTLESTAFF -> 112.5;
            case ItemID.FIRE_BATTLESTAFF -> 125.0;
            default -> 0.0;
        };
    }

    public static int getOrbIdForStaff(int staffId) {
        return switch (staffId) {
            case ItemID.AIR_BATTLESTAFF -> ItemID.AIR_ORB;
            case ItemID.WATER_BATTLESTAFF -> ItemID.WATER_ORB;
            case ItemID.EARTH_BATTLESTAFF -> ItemID.EARTH_ORB;
            case ItemID.FIRE_BATTLESTAFF -> ItemID.FIRE_ORB;
            default -> -1;
        };
    }
}