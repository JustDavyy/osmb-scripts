package tasks;

// GENERAL JAVA IMPORTS
import com.osmb.api.ui.tabs.Tab;
import main.dTempAlcher;
import utils.Task;
import static main.dTempAlcher.*;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.tabs.Inventory;
import com.osmb.api.utils.UIResult;

public class AlchTask extends Task {
    private long lastAlchTime = 0;
    private long startTime = 0;
    private int alchCount = 0;
    private int slotsFree = 0;
    private UIResult<Rectangle> slotBounds;
    private boolean slotsInitialized = false;
    private boolean initializedSlotBounds = false;

    public AlchTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastAlchTime >= getCooldownForSpell());
    }

    public boolean execute() {

        if (!initializedSlotBounds) {
            script.log(dTempAlcher.class, "Opening inventory tab");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

            script.log(dTempAlcher.class, "Slot chosen: " + alchSlotID);

            Inventory inventory = script.getWidgetManager().getInventory();
            slotBounds = script.getItemManager().getBoundsForSlot(alchSlotID, inventory);

            script.log(dTempAlcher.class, "Slot bounds: " + slotBounds.toString());

            if (slotBounds != null) {
                initializedSlotBounds = true;
            }
        }

        if (alchCount > 2 && !slotsInitialized) {
            script.log(dTempAlcher.class, "Opening inventory tab");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

            var inventory = script.getWidgetManager().getInventory();
            var freeSlotsResult = script.getItemManager().getFreeSlots(inventory);

            if (!freeSlotsResult.isEmpty()) {
                int count = freeSlotsResult.size();
                if (count >= 8 && count <= 27) {
                    slotsFree = count;
                    script.log("DEBUG", "Cached free inventory slots: " + slotsFree);
                } else {
                    script.log("WARN", "Unexpected free slot count: " + count + " (expected 8–27)");
                }
            } else {
                script.log("ERROR", "Failed to get free slots: result was empty.");
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
            script.log(dTempAlcher.class, "Spell sprite not found for " + spellToCast.getName() + ". Stopping script...");
            script.stop();
            return false;
        }

        if (success) {
            // Failsafe: Recheck free slots before casting
            var currentFreeSlots = script.getItemManager().getFreeSlots(script.getWidgetManager().getInventory());
            if (slotsInitialized && !currentFreeSlots.isEmpty() && currentFreeSlots.size() > slotsFree) {
                script.log("FAILSAFE", "Free inventory slots increased from " + slotsFree + " to " + currentFreeSlots.size() + ". Most likely out of items to alch. Stopping script.");
                script.stop();
                return false;
            }

            long now = System.currentTimeMillis();
            long timeSinceLastAlch = now - lastAlchTime;
            script.log("DEBUG", "Time since last alch: " + timeSinceLastAlch + "ms");

            lastAlchTime = System.currentTimeMillis();
            alchCount++;

            script.getFinger().tap(slotBounds.get().getBounds());
            script.log(dTempAlcher.class, "Cast " + spellToCast.getName() + ".");

            // Print stats to log
            printStats();

            // Schedule next task attempt after cooldown
            script.submitTask(() -> (System.currentTimeMillis() - lastAlchTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName() + ".");
        }

        return false;
    }

    private void printStats() {
        // Alchs per hour
        long elapsed = System.currentTimeMillis() - startTime;
        int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);

        // XP tracking
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