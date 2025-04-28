package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import com.osmb.api.ui.tabs.Inventory;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResult;
import main.dTempAlcher;
import utils.Task;

import static main.dTempAlcher.*;

public class AlchTask extends Task {
    private long lastAlchTime = 0;
    private long startTime = 0;
    private int alchCount = 0;

    private boolean initializedSlotBounds = false;
    private UIResult<Rectangle> slotBounds;

    private boolean slotsInitialized = false;
    private int slotsFree = 0;

    public AlchTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastAlchTime >= getCooldownForSpell());
    }

    @Override
    public boolean execute() {
        if (!initializedSlotBounds) {
            script.log(dTempAlcher.class, "Opening inventory tab...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

            Inventory inventory = script.getWidgetManager().getInventory();
            slotBounds = script.getItemManager().getBoundsForSlot(alchSlotID, inventory);

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
            var freeSlotsResult = script.getItemManager().getFreeSlots(script.getWidgetManager().getInventory());
            if (!freeSlotsResult.isEmpty()) {
                slotsFree = freeSlotsResult.size();
                script.log("DEBUG", "Cached free inventory slots: " + slotsFree);
            }
            slotsInitialized = true;
        }

        boolean success = false;
        try {
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    Spellbook.ResultType.CHANGE_TAB
            );
        } catch (SpellNotFoundException e) {
            script.log(dTempAlcher.class, "Spell sprite not found for " + spellToCast.getName() + ". Stopping.");
            script.stop();
            return false;
        }

        if (success) {
            var currentFreeSlots = script.getItemManager().getFreeSlots(script.getWidgetManager().getInventory());
            if (slotsInitialized && !currentFreeSlots.isEmpty() && currentFreeSlots.size() > slotsFree) {
                script.log("FAILSAFE", "Inventory slots increased unexpectedly. Assuming out of items to alch.");
                moveToNextSlotOrStop();
                return false;
            }

            long now = System.currentTimeMillis();
            lastAlchTime = now;
            alchCount++;

            script.getFinger().tap(slotBounds.get().getBounds());
            script.log(dTempAlcher.class, "Cast " + spellToCast.getName() + " on slot " + alchSlotID + ".");

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
                script.log("INFO", "Switching to next slot: " + alchSlotID + ". Remaining: " + (slotsToAlch.size() - currentSlotIndex));
                initializedSlotBounds = false;
                slotsInitialized = false;
            } else {
                script.log("FINISH", "Finished alching all selected slots. Stopping script.");
                script.stop();
            }
        } else {
            script.log("FINISH", "Finished alching (single slot mode). Stopping script.");
            script.stop();
        }
    }

    private void printStats() {
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

    private static long getCooldownForSpell() {
        return (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 3000 : 1800;
    }
}
