package tasks;

import com.osmb.api.ui.spellbook.StandardSpellbook;
import utils.Task;

import static main.dPublicAlcher.*;

import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;

public class AlchTask extends Task {
    private long lastAlchTime = 0;

    public AlchTask(Script script) {
        super(script);
    }

    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastAlchTime >= getCooldownForSpell());
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        task = "Get stack size";
        if (stackSize == 0) {
            if (multipleItemsMode && (currentItemIndex + 1) < itemsToAlch.size()) {
                currentItemIndex++;
                alchItemID = itemsToAlch.get(currentItemIndex);
                script.log("INFO", "Switching to next item: " + script.getItemManager().getItemName(alchItemID) + " (ID=" + alchItemID + ")");
                script.log("INFO", "Items left in list: " + (itemsToAlch.size() - currentItemIndex));
                setupDone = false;
                return false;
            } else {
                script.log(getClass().getSimpleName(), "We are out of items to alch, stopping script...");
                script.stop();
            }
        }

        boolean success = false;
        task = "Cast spell";
        try {
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    Spellbook.ResultType.CHANGE_TAB
            );
        } catch (SpellNotFoundException e) {
            script.log(getClass().getSimpleName(), "Spell sprite not found for " + spellToCast.getName() + ". Stopping script...");
            script.stop();
            return false;
        }

        if (success) {
            long now = System.currentTimeMillis();
            long timeSinceLastAlch = now - lastAlchTime;
            script.log("DEBUG", "Time since last alch: " + timeSinceLastAlch + "ms");

            task = "Update counts";
            lastAlchTime = System.currentTimeMillis();
            alchCount++;
            stackSize--;

            task = "Tap item";
            script.getFinger().tap(itemRect.get().getBounds());
            script.log(getClass().getSimpleName(), "Cast " + spellToCast.getName() + " on " + itemName);

            printStats();
            task = "Wait for cooldown";
            script.submitTask(() -> (System.currentTimeMillis() - lastAlchTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName() + " on " + itemName);
        }

        return false;
    }

    private void printStats() {
        task = "Print stats";
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
