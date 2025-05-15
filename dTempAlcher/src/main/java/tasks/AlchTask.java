package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResult;
import main.dTempAlcher;
import utils.Task;

import java.util.Collections;

import static main.dTempAlcher.*;

public class AlchTask extends Task {
    private long lastAlchTime = 0;

    private boolean initializedSlotBounds = false;
    private UIResult<Rectangle> slotBounds;

    private boolean slotsInitialized = false;
    private int slotsFree = 0;

    public AlchTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastAlchTime >= getCooldownForSpell());
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!initializedSlotBounds) {
            script.log(dTempAlcher.class, "Opening inventory tab...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            slotBounds = script.getWidgetManager().getInventory().getBoundsForSlot(alchSlotID);

            if (slotBounds != null) {
                initializedSlotBounds = true;
                script.log("INFO", "Initialized slot bounds for slot " + alchSlotID + ".");
            } else {
                script.log("ERROR", "Failed to get slot bounds for slot " + alchSlotID + ". Stopping.");
                script.stop();
                return false;
            }
        }

        // If many alchs done, cache free slots (failsafe)
        if (alchCount > 2 && !slotsInitialized) {
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
            if (inventorySnapshot == null) {
                // inventory not visible
                return false;
            }
            slotsFree = inventorySnapshot.getFreeSlots();
            script.log("DEBUG", "Cached free inventory slots: " + slotsFree);
            slotsInitialized = true;
        }

        boolean success = false;
        try {
            task = "Cast spell";
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    Spellbook.ResultType.CHANGE_TAB
            );
        } catch (SpellNotFoundException e) {
            script.log(getClass().getSimpleName(), "Spell sprite not found for " + spellToCast.getName() + ". Stopping.");
            script.stop();
            return false;
        }

        if (success) {
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
            if (inventorySnapshot == null) {return false;}
            var currentFreeSlots = inventorySnapshot.getFreeSlots();
            if (slotsInitialized && currentFreeSlots > slotsFree) {
                script.log("FAILSAFE", "Inventory slots increased unexpectedly. Assuming out of items to alch.");
                moveToNextSlotOrStop();
                return false;
            }

            task = "Update stats";
            lastAlchTime = System.currentTimeMillis();
            alchCount++;

            task = "Tap item";
            script.getFinger().tap(slotBounds.get().getBounds());
            script.log(getClass().getSimpleName(), "Cast " + spellToCast.getName() + " on slot " + alchSlotID + ".");

            printStats();

            script.submitTask(() -> (System.currentTimeMillis() - lastAlchTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName() + ".");
        }

        return false;
    }

    private void moveToNextSlotOrStop() {
        if (multipleSlotsMode) {
            currentSlotIndex++;
            if (currentSlotIndex < slotsToAlch.size()) {
                alchSlotID = slotsToAlch.get(currentSlotIndex);
                task = "Switch alch slot " + alchSlotID;
                script.log("INFO", "Switching to next slot: " + alchSlotID + ". Remaining: " + (slotsToAlch.size() - currentSlotIndex));
                initializedSlotBounds = false;
                slotsInitialized = false;
            } else {
                task = "Stop script";
                script.log("FINISH", "Finished alching all selected slots. Stopping script.");
                script.stop();
            }
        } else {
            task = "Stop script";
            script.log("FINISH", "Finished alching (single slot mode). Stopping script.");
            script.stop();
        }
    }

    private void printStats() {
        task = "Print stats";
        long elapsed = System.currentTimeMillis() - startTime;
        int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);

        int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
        int totalXp = alchCount * xpPerCast;
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Alchs done: %d | Alchs/hr: %,d | XP gained: %,d | XP/hr: %,d",
                alchCount, alchsPerHour, totalXp, xpPerHour
        ));
    }

    private long getCooldownForSpell() {
        int roll = script.random(100);

        if (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) {
            if (roll < 50) {
                return script.random(3000, 3101);
            } else if (roll < 90) {
                return script.random(3050, 3201);
            } else {
                return script.random(3100, 3501);
            }
        } else {
            if (roll < 50) {
                return script.random(1800, 1901);
            } else if (roll < 90) {
                return script.random(1850, 2001);
            } else {
                return script.random(1900, 2301);
            }
        }
    }
}
