package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import java.util.function.BooleanSupplier;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.script.Script;
import main.dWinemaker;
import utils.Task;

import static main.dWinemaker.*;

public class ProcessTask extends Task {
    private long startTime = 0;
    private int craftCount = 0;
    private final int xpPerWine = 200;

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
        UIResultList<ItemSearchResult> grapeResults = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), grapeID);
        UIResultList<ItemSearchResult> jugResults = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ItemID.JUG_OF_WATER);

        if (grapeResults.isNotFound() || jugResults.isNotFound()) {
            script.log(dWinemaker.class, "Missing ingredients. Flagging bank.");
            shouldBank = true;
            return false;
        }

        if (!script.getItemManager().unSelectItemIfSelected()) {
            return false;
        }

        boolean interacted = interactAndWaitForDialogue(grapeResults.getRandom(), jugResults.getRandom());

        if (!interacted) {
            script.log(dWinemaker.class, "Failed to interact with items.");
            return false;
        }

        // Dialogue opened - select item to produce
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(ItemID.JUG_OF_WINE, grapeID);
            if (!selected) {
                script.log(dWinemaker.class, "Initial selection failed, retrying...");
                script.sleep(script.random(150, 300)); // slight delay before retry
                selected = script.getWidgetManager().getDialogue().selectItem(ItemID.JUG_OF_WINE, grapeID);
            }

            if (!selected) {
                script.log(dWinemaker.class, "Failed to select wine in dialogue after retry.");
                return false;
            }
            script.log(dWinemaker.class, "Selected wine to produce.");
            waitUntilFinishedProducing(grapeID, ItemID.JUG_OF_WATER);
            craftCount += 14;
            printStats();
        }

        return false;
    }

    private boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        int rand = script.random(1);
        ItemSearchResult first = rand == 0 ? item1 : item2;
        ItemSearchResult second = rand == 0 ? item2 : item1;

        // First interaction
        if (!first.interact()) {
            script.log(getClass(), "First item interaction failed, retrying...");
            if (!first.interact()) {
                return false;
            }
        }

        script.sleep(script.random(150, 300)); // slight delay

        // Second interaction
        if (!second.interact()) {
            script.log(getClass(), "Second item interaction failed, retrying...");
            if (!second.interact()) {
                return false;
            }
        }

        // After both interactions succeed, wait for the dialogue
        boolean useHumanTask = script.random(10) < 3; // 30% chance
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        return useHumanTask
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

        boolean useHumanTask = script.random(10) < 3; // 30% chance

        if (useHumanTask) {
            script.log(getClass(), "Using human task to wait until processing finishes.");
            script.submitHumanTask(condition, 60000, true, false, true);
        } else {
            script.log(getClass(), "Using regular task to wait until processing finishes.");
            script.submitTask(condition, 60000, true, false, true);
        }
    }

    private void printStats() {
        long elapsed = System.currentTimeMillis() - startTime;
        int winesPerHour = (int) ((craftCount * 3600000L) / elapsed);

        int totalXp = craftCount * xpPerWine;
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Wines made: %d | Wines/hr: %,d | XP gained: %,d | XP/hr: %,d",
                craftCount, winesPerHour, totalXp, xpPerHour
        ));
    }
}