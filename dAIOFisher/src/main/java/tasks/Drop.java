package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import utils.Task;
import java.util.*;

import static main.dAIOFisher.*;

public class Drop extends Task {

    public Drop(Script script) {
        super(script);
    }

    public boolean activate() {
        if (!dropMode) return false;

        List<Integer> relevantFishIds = cookMode
                ? fishingMethod.getCookedFish()
                : fishingMethod.getCatchableFish();

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(relevantFishIds));

        return inv != null && inv.isFull();
    }

    public boolean execute() {
        task = getClass().getSimpleName();

        List<Integer> relevantFishIds = cookMode
                ? fishingMethod.getCookedFish()
                : fishingMethod.getCatchableFish();

        if (relevantFishIds.isEmpty()) return false;

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(relevantFishIds));
        if (inv == null) return false;

        for (int i = 0; i < relevantFishIds.size(); i++) {
            int id = relevantFishIds.get(i);
            int count = inv.getAmount(id);

            switch (i) {
                case 0 -> fish1Caught += count;
                case 1 -> fish2Caught += count;
                case 2 -> fish3Caught += count;
                case 3 -> fish4Caught += count;
                case 4 -> fish5Caught += count;
                case 5 -> fish6Caught += count;
                case 6 -> fish7Caught += count;
                case 7 -> fish8Caught += count;
            }
        }

        Set<Integer> dropIds = Set.copyOf(relevantFishIds);
        for (int attempt = 0; attempt < 3; attempt++) {
            script.getWidgetManager().getInventory().dropItems(dropIds);
            ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(dropIds);

            if (afterDrop == null || afterDrop.isEmpty()) {
                break; // All items successfully dropped
            }

            script.submitTask(() -> false, script.random(150, 400));
        }

        return false;
    }
}
