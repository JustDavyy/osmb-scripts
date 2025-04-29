package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.script.Script;
import main.dBattlestaffCrafter;
import utils.Task;

import java.util.function.BooleanSupplier;

import static main.dBattlestaffCrafter.*;

public class ProcessTask extends Task {
    private long startTime = 0;
    private int craftCount = 0;

    public ProcessTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean activate() {
        return hasReqs && !shouldBank;
    }

    @Override
    public boolean execute() {
        int orbId = getOrbIdForStaff(staffID);
        if (orbId == -1) {
            script.log(getClass(), "Unknown orb for staff ID: " + staffID + ". Stopping script.");
            script.stop();
            return false;
        }

        UIResultList<ItemSearchResult> orbResults = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), orbId);
        UIResultList<ItemSearchResult> staffResults = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ItemID.BATTLESTAFF);

        if (orbResults.isNotFound() || staffResults.isNotFound()) {
            script.log(dBattlestaffCrafter.class, "Missing battlestaffs or orbs. Flagging bank.");
            shouldBank = true;
            return false;
        }

        if (!script.getItemManager().unSelectItemIfSelected()) {
            return false;
        }

        boolean interacted = interactAndWaitForDialogue(orbResults.getRandom(), staffResults.getRandom());

        if (!interacted) {
            script.log(dBattlestaffCrafter.class, "Failed to interact with orb and staff.");
            return false;
        }

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(staffID);
            if (!selected) {
                script.log(dBattlestaffCrafter.class, "Initial staff selection failed, retrying...");
                script.submitTask(() -> false, script.random(150, 300));
                selected = script.getWidgetManager().getDialogue().selectItem(staffID);
            }

            if (!selected) {
                script.log(dBattlestaffCrafter.class, "Failed to select staff in dialogue after retry.");
                return false;
            }
            script.log(dBattlestaffCrafter.class, "Selected battlestaff to craft.");

            waitUntilFinishedProducing(orbId, ItemID.BATTLESTAFF);
            craftCount += 14;
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

        script.submitTask(() -> false, script.random(150, 300));

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

        return script.random(10) < 3
                ? script.submitHumanTask(condition, 3000)
                : script.submitTask(condition, 3000);
    }

    private void waitUntilFinishedProducing(int... resources) {
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.submitTask(() -> false, script.random(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 18000) {
                return true;
            }

            for (int id : resources) {
                UIResultList<ItemSearchResult> result = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), id);
                if (result.isNotVisible()) return false;
                if (result.isEmpty()) return true;
            }

            return false;
        };

        if (script.random(10) < 3) {
            script.log(getClass(), "Using human task to wait until crafting finishes.");
            script.submitHumanTask(condition, 60000, true, false, true);
        } else {
            script.log(getClass(), "Using regular task to wait until crafting finishes.");
            script.submitTask(condition, 60000, true, false, true);
        }
    }

    private void printStats() {
        long elapsed = System.currentTimeMillis() - startTime;
        int craftsPerHour = (int) ((craftCount * 3600000L) / elapsed);

        double xpPerCraft = getXPForStaff(staffID);
        int totalXp = (int) (craftCount * xpPerCraft);
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Staffs crafted: %d | Staffs/hr: %,d | XP gained: %,d | XP/hr: %,d",
                craftCount, craftsPerHour, totalXp, xpPerHour
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

    private int getOrbIdForStaff(int staffId) {
        return switch (staffId) {
            case ItemID.AIR_BATTLESTAFF -> ItemID.AIR_ORB;
            case ItemID.WATER_BATTLESTAFF -> ItemID.WATER_ORB;
            case ItemID.EARTH_BATTLESTAFF -> ItemID.EARTH_ORB;
            case ItemID.FIRE_BATTLESTAFF -> ItemID.FIRE_ORB;
            default -> -1;
        };
    }
}