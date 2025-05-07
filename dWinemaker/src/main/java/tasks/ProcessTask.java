package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.Set;
import java.util.function.BooleanSupplier;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.script.Script;
import main.dWinemaker;
import utils.Task;

import static main.dWinemaker.*;

public class ProcessTask extends Task {
    private final long startTime;
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
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(grapeID, ItemID.JUG_OF_WATER));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (!inventorySnapshot.contains(grapeID) || !inventorySnapshot.contains(ItemID.JUG_OF_WATER)) {
            script.log(dWinemaker.class, "Missing ingredients. Flagging bank.");
            shouldBank = true;
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        boolean interacted = interactAndWaitForDialogue(inventorySnapshot);

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
            waitUntilFinishedProducing();
            craftCount += 14;
            printStats();
        }

        return false;
    }

    private boolean interactAndWaitForDialogue(ItemGroupResult inventSnapshot) {
        boolean firstIsGrape = script.random(2) == 0;

        int firstID = firstIsGrape ? grapeID : ItemID.JUG_OF_WATER;
        int secondID = firstIsGrape ? ItemID.JUG_OF_WATER : grapeID;

        // First interaction with retry
        if (!inventSnapshot.getRandomItem(firstID).interact()) {
            script.log(getClass(), "First item interaction failed, retrying...");
            if (!inventSnapshot.getRandomItem(firstID).interact()) {
                return false;
            }
        }

        script.sleep(script.random(150, 300));

        // Second interaction with retry
        if (!inventSnapshot.getRandomItem(secondID).interact()) {
            script.log(getClass(), "Second item interaction failed, retrying...");
            if (!inventSnapshot.getRandomItem(secondID).interact()) {
                return false;
            }
        }

        // Wait for dialogue
        boolean useHumanTask = script.random(10) < 3; // 30% chance
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        return useHumanTask
                ? script.submitHumanTask(condition, script.random(3000, 5000))
                : script.submitTask(condition, script.random(3000, 5000));
    }

    private void waitUntilFinishedProducing() {
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

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(grapeID, ItemID.JUG_OF_WATER));
            if (inventorySnapshot == null) {return false;}
            return inventorySnapshot.isEmpty();
        };

        boolean useHumanTask = script.random(10) < 3; // 30% chance

        if (useHumanTask) {
            script.log(getClass(), "Using human task to wait until processing finishes.");
            script.submitHumanTask(condition, script.random(60000, 62000));
        } else {
            script.log(getClass(), "Using regular task to wait until processing finishes.");
            script.submitTask(condition, script.random(60000, 62000));
        }
    }

    private void printStats() {
        long elapsed = System.currentTimeMillis() - startTime;
        int winesPerHour = (int) ((craftCount * 3600000L) / elapsed);

        int xpPerWine = 200;
        int totalXp = craftCount * xpPerWine;
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Wines made: %d | Wines/hr: %,d | XP gained: %,d | XP/hr: %,d",
                craftCount, winesPerHour, totalXp, xpPerHour
        ));
    }
}