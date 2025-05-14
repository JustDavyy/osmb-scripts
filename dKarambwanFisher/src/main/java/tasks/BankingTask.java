package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dKarambwanFisher.*;

public class BankingTask extends Task {

    private long startTime = 0;
    private ItemGroupResult inventorySnapshot;
    private int missingKarambwanjiCount = 0;

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public BankingTask(Script script) {
        super(script);
    }

    public boolean activate() {return true;}

    public boolean execute() {
        task = getClass().getSimpleName();
        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.RAW_KARAMBWANJI, ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159, ItemID.RAW_KARAMBWAN, ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL, ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET));

        if (!inventorySnapshot.contains(ItemID.RAW_KARAMBWANJI)) {
            missingKarambwanjiCount++;
            script.log(getClass().getSimpleName(), "❌ Missing Karambwanji (" + missingKarambwanjiCount + "/3)");

            if (missingKarambwanjiCount >= 3) {
                script.log(getClass().getSimpleName(), "‼ Karambwanji missing 3 times in a row. Stopping script...");
                script.stop();
            }

            return false;
        } else {
            missingKarambwanjiCount = 0; // Reset counter if found
        }

        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        if (usingBarrel && inventorySnapshot.contains(ItemID.RAW_KARAMBWAN)) {
            task = "Empty fish barrel";
            script.log(getClass().getSimpleName(), "Emptying fish barrel in the bank");
            inventorySnapshot.getItem(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL).interact("Empty");

            // Add inventory karambwan amount + 28 from fishing barrel
            caughtCount = caughtCount + inventorySnapshot.getAmount(ItemID.RAW_KARAMBWAN) + 28;
        }

        if (!usingBarrel && inventorySnapshot.contains(ItemID.RAW_KARAMBWAN)) {
            caughtCount = caughtCount + inventorySnapshot.getAmount(ItemID.RAW_KARAMBWAN);
        }

        task = "Deposit karambwans";
        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.RAW_KARAMBWANJI, ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159, ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL, ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET))) {
            return false;
        }

        task = "Close bank";
        script.getWidgetManager().getBank().close();
        script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));

        updateStats();
        doneBanking = true;
        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Searching for a bank...");

        // Regular bank object
        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object.");
            return;
        }

        // Same waiting logic after interaction
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.submitTask(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, script.random(14000, 16000));
    }

    private void updateStats() {
        task = "Print stats";
        long elapsed = System.currentTimeMillis() - startTime;
        totalXpGained = caughtCount * 50;
        int caughtPerHour = (int) ((caughtCount * 3600000L) / elapsed);
        int xpPerHour = (int) ((totalXpGained * 3600000L) / elapsed);

        // Print to log
        script.log("STATS", String.format(
                "Karambwan fished: %,d | Karams/hr: %,d | XP gained: %,d | XP/hr: %,d",
                caughtCount, caughtPerHour, (int) totalXpGained, xpPerHour
        ));
    }
}
