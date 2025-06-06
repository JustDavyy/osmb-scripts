package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dAmethystMiner.*;

public class BankTask extends Task {

    private static final Area bankArea = new RectangleArea(3010, 9715, 10, 6, 0);
    private static final Area bankWalkArea = new RectangleArea(3014, 9718, 2, 1, 0);

    public static final Set<Integer> ITEM_IDS_TO_NOT_DEPOSIT2 = new HashSet<>(Set.of(
            ItemID.BRONZE_PICKAXE, ItemID.IRON_PICKAXE,
            ItemID.STEEL_PICKAXE, ItemID.BLACK_PICKAXE, ItemID.MITHRIL_PICKAXE,
            ItemID.ADAMANT_PICKAXE, ItemID.RUNE_PICKAXE, ItemID.DRAGON_PICKAXE,
            ItemID.DRAGON_PICKAXE_OR, ItemID.CRYSTAL_PICKAXE, ItemID.INFERNAL_PICKAXE,
            ItemID.INFERNAL_PICKAXE_OR, ItemID.ANTIQUE_LAMP, ItemID.GILDED_PICKAXE,
            ItemID.DRAGON_PICKAXE_12797, ItemID.CHISEL
    ));

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!bankMode) return false;
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inv != null && inv.isFull();
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(getTrackedItemIDs());
        if (inv == null) return false;

        WorldPosition myPos = script.getWorldPosition();
        if (myPos != null && !bankArea.contains(myPos)) {
            task = "Walk to bank area";
            return script.getWalker().walkTo(bankWalkArea.getRandomPosition());
        }

        // Handle banking
        return bankItems(inv);
    }

    private boolean bankItems(ItemGroupResult inv) {
        if (!script.getWidgetManager().getDepositBox().isVisible()) {
            task = "Bank at deposit box";
            script.log(getClass(), "Searching for deposit box...");

            Predicate<RSObject> bankQuery = obj ->
                    obj.getName() != null &&
                            obj.getName().equalsIgnoreCase("Bank Deposit Box") &&
                            obj.canReach();

            List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
            if (banksFound.isEmpty()) {
                script.log(getClass(), "Can't find any banks matching criteria...");
                return false;
            }

            RSObject depositBox = (RSObject) script.getUtils().getClosest(banksFound);
            if (!depositBox.interact("Deposit")) {
                script.log(getClass(), "Failed to interact with deposit box.");
                return false;
            }

            AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
            AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
            script.submitHumanTask(() -> {
                WorldPosition current = script.getWorldPosition();
                if (current == null) return false;
                if (pos.get() == null || !current.equals(pos.get())) {
                    positionChangeTimer.get().reset();
                    pos.set(current);
                }
                return script.getWidgetManager().getDepositBox().isVisible() || positionChangeTimer.get().timeElapsed() > 4000;
            }, 20000);
        }

        var snapshot = script.getWidgetManager().getDepositBox().search(ITEM_IDS_TO_NOT_DEPOSIT2);
        if (snapshot == null) {
            script.log(getClass(), "Deposit box not open.");
            return false;
        }

        if (!script.getWidgetManager().getDepositBox().depositAll(ITEM_IDS_TO_NOT_DEPOSIT2)) {
            script.log(getClass(), "Failed to deposit items.");
            return false;
        }

        script.getWidgetManager().getDepositBox().close();
        script.log(getClass(), "Banked items and closed deposit box.");
        return true;
    }

    private Set<Integer> getTrackedItemIDs() {
        return Set.of(
                ItemID.UNCUT_SAPPHIRE,
                ItemID.UNCUT_EMERALD,
                ItemID.UNCUT_RUBY,
                ItemID.UNCUT_DIAMOND,
                ItemID.AMETHYST,
                ItemID.CLUE_GEODE_BEGINNER,
                ItemID.CLUE_GEODE_EASY,
                ItemID.CLUE_GEODE_MEDIUM,
                ItemID.CLUE_GEODE_HARD,
                ItemID.CLUE_GEODE_ELITE
        );
    }
}