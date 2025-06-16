package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
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

        task = "Check required tools";
        if (!inventorySnapshot.containsAll(Set.copyOf(fishingMethod.getRequiredTools()))) {
            script.log(getClass().getSimpleName(), "Not all required tools (rods/feathers etc) could be located in inventory, stopping script!");
            script.stop();
            return false;
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
