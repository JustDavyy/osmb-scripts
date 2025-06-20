package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import data.FishingLocation;
import utils.Task;

import java.util.Set;

import static main.dAIOFisher.*;

public class Setup extends Task {

    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log(getClass().getSimpleName(), "We are now inside the Setup task logic");

        // Check if we have all the necessary tools in our inventory
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(
                Set.copyOf(fishingMethod.getRequiredTools())
        );

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (!fishingMethod.getRequiredTools().contains(ItemID.SMALL_FISHING_NET) && !fishingMethod.getRequiredTools().contains(ItemID.BIG_FISHING_NET)) {
            task = "Check required tools";
            if (!inventorySnapshot.containsAll(Set.copyOf(fishingMethod.getRequiredTools()))) {
                script.log(getClass().getSimpleName(), "Not all required tools could be located in inventory, stopping script!");
                script.stop();
                return false;
            }
        } else {
            script.log(getClass(), "Skipped required tools check, as one or more is a textured item!");
            if (fishingLocation.equals(FishingLocation.Karamja_West)) {
                ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(Set.of(ItemID.RAW_KARAMBWANJI));

                if (inventorySnapshot == null) {
                    // Inventory not visible
                    return false;
                }

                task = "Check karambwanji start count";
                script.log(getClass(), "Checking initial Karambwanji count.");
                startAmount = invCheck.getAmount(ItemID.RAW_KARAMBWANJI);
                script.log(getClass(), "Karambwanji start count detected: " + startAmount);
            }
        }

        // Check if we have a fishing barrel in our inventory
        ItemGroupResult inventorySnapshot2 = script.getWidgetManager().getInventory().search(Set.of(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL));

        if (inventorySnapshot2 == null) {
            // Inventory not visible
            return false;
        }

        // Check if we're using fishing barrel
        task = "Check fish barrel";
        if (inventorySnapshot2.containsAny(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL)) {
            script.log(getClass().getSimpleName(), "Fishing barrel detected in inventory, marking usage as TRUE");
            usingBarrel = true;
        }

        task = "Finish set up";
        setupDone = true;
        return false;
    }
}
