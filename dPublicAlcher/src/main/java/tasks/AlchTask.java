package tasks;

import com.osmb.api.ui.spellbook.StandardSpellbook;
import main.dPublicAlcher;
import utils.Task;

import static main.dPublicAlcher.*;

import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;

public class AlchTask extends Task {
    private long lastAlchTime = 0;
    private long startTime = 0;
    private int alchCount = 0;

    public AlchTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastAlchTime >= getCooldownForSpell());
    }

    public boolean execute() {
        if (stackSize == 0) {
            if (multipleItemsMode && (currentItemIndex + 1) < itemsToAlch.size()) {
                currentItemIndex++;
                alchItemID = itemsToAlch.get(currentItemIndex);
                script.log("INFO", "Switching to next item: " + script.getItemManager().getItemName(alchItemID) + " (ID=" + alchItemID + ")");
                script.log("INFO", "Items left in list: " + (itemsToAlch.size() - currentItemIndex));
                setupDone = false;
                return false;
            } else {
                script.log(dPublicAlcher.class, "We are out of items to alch, stopping script...");
                script.stop();
            }
        }

        boolean success = false;
        try {
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    Spellbook.ResultType.CHANGE_TAB
            );
        } catch (SpellNotFoundException e) {
            script.log(dPublicAlcher.class, "Spell sprite not found for " + spellToCast.getName() + ". Stopping script...");
            script.stop();
            return false;
        }

        if (success) {
            long now = System.currentTimeMillis();
            long timeSinceLastAlch = now - lastAlchTime;
            script.log("DEBUG", "Time since last alch: " + timeSinceLastAlch + "ms");

            lastAlchTime = System.currentTimeMillis();
            alchCount++;
            stackSize--;

            script.getFinger().tap(itemRect.get().getBounds());
            script.log(dPublicAlcher.class, "Cast " + spellToCast.getName() + " on " + itemName);

            printStats();
            script.submitTask(() -> (System.currentTimeMillis() - lastAlchTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName() + " on " + itemName);
        }

        return false;
    }

    private void printStats() {
        long elapsed = System.currentTimeMillis() - startTime;
        int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);

        int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
        int totalXp = alchCount * xpPerCast;
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        long estimatedMillisLeft = (alchsPerHour > 0)
                ? (stackSize * 3600000L) / alchsPerHour
                : 0;

        long seconds = estimatedMillisLeft / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        String timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, secs);

        script.log("STATS", String.format(
                "Alchs done: %d | Alchs/hr: %,d | XP gained: %,d | XP/hr: %,d | Items left: %,d | Finished in: %s",
                alchCount, alchsPerHour, totalXp, xpPerHour, stackSize, timeLeftFormatted
        ));
    }

    private static long getCooldownForSpell() {
        return (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 3000 : 1800;
    }
}
