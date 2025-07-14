package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.timing.Timer;
import main.dCooker;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static main.dCooker.*;

public class ProcessTask extends Task {
    private static int failCount = 0;

    public ProcessTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            script.log(getClass().getSimpleName(), "Opening inventory tab");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            return false;
        }

        return inventorySnapshot.contains(cookingItemID);
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            return script.getWidgetManager().getBank().close();
        }

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (!inventorySnapshot.contains(cookingItemID)) {
            if (failCount >= 3) {
                script.log(getClass(), "No cookable fish found for three times in a row, stopping script!");
                script.stop();
                return false;
            }
            script.log(getClass(), "No fish to cook found in inventory, returning!");
            failCount++;
            return false;
        }

        task = "Find cooking object";
        RSObject cookObject = getClosestCookObject();
        if (cookObject == null) {
            script.log(getClass(), "No cookable object found nearby (range/fire/clay oven).");
            return false;
        }

        task = "Interact with object";
        if (!cookObject.interact(COOKING_ACTIONS)) {
            script.log(getClass(), "Failed to interact with cooking object. Retrying...");
            if (!cookObject.interact(COOKING_ACTIONS)) {
                return false;
            }
        }

        task = "Start cooking action";
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        script.submitHumanTask(condition, script.random(4000, 6000));

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(cookingItemID)
                    || script.getWidgetManager().getDialogue().selectItem(cookedItemID);

            if (!selected) {
                script.log(getClass(), "Initial food selection failed, retrying...");
                script.submitHumanTask(() -> false, script.random(150, 300));

                selected = script.getWidgetManager().getDialogue().selectItem(cookingItemID)
                        || script.getWidgetManager().getDialogue().selectItem(cookedItemID);
            }

            if (!selected) {
                script.log(getClass(), "Failed to select food item in dialogue after retry.");
                return false;
            }

            script.log(getClass(), "Selected food to cook.");

            waitUntilFinishedCooking();

            Set<Integer> idsToSearch = (cookingItemID == cookedItemID)
                    ? Set.of(cookingItemID)
                    : Set.of(cookingItemID, cookedItemID);

            inventorySnapshot = script.getWidgetManager().getInventory().search(idsToSearch);

            if (inventorySnapshot == null || inventorySnapshot.isEmpty()) {
                script.log(getClass(), "No fish to cook could be located.");
            } else {
                int cookedNow = inventorySnapshot.getAmount(cookedItemID);

                if (cookingItemID != ItemID.GIANT_SEAWEED) {
                    totalCookCount += 28;
                    cookCount += cookedNow;
                    burnCount += (28 - cookedNow);
                    totalXpGained += cookedNow * getXpForFood(cookingItemID);
                } else {
                    totalCookCount += 6;
                    cookCount += cookedNow;
                    totalXpGained += cookedNow * getXpForFood(cookingItemID);
                }
            }
        }

        return false;
    }

    private RSObject getClosestCookObject() {
        List<RSObject> objects = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) {
                return false;
            }
            return Objects.equals(gameObject.getName(), "Range") || Objects.equals(gameObject.getName(), "Fire") || Objects.equals(gameObject.getName(), "Clay oven");
        });

        if (objects.isEmpty()) {
            script.log(ProcessTask.class, "No objects found matching query...");
            return null;
        }

        objects.removeIf(object -> !object.canReach());
        if (objects.isEmpty()) {
            script.log(ProcessTask.class, "No reachable objects inside the loaded scene..");
            return null;
        }
        RSObject closest = (RSObject) script.getUtils().getClosest(objects);
        if (closest == null) {
            script.log(ProcessTask.class, "Closest object is null.");
            return null;
        }
        return closest;
    }

    private void waitUntilFinishedCooking() {
        task = "Wait until cooking finish";
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.submitHumanTask(() -> false, script.random(1000, 3000));
                return true;
            }

            int timeout = (cookingItemID == ItemID.GIANT_SEAWEED) ? 8000 : 66000;

            if (amountChangeTimer.timeElapsed() > timeout) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.contains(cookingItemID);
        };

        script.log(getClass(), "Using human task to wait until cooking finishes.");
        script.submitHumanTask(condition, script.random(66000, 70000));
    }

    private double getXpForFood(int itemId) {
        return switch (itemId) {
            case ItemID.RAW_SHRIMPS -> 30.0;
            case ItemID.SEAWEED -> 0.0;
            case ItemID.GIANT_SEAWEED -> 0.0;
            case ItemID.BREAD_DOUGH -> 40.0;
            case ItemID.RAW_CHICKEN -> 30.0;
            case ItemID.RAW_ANCHOVIES -> 30.0;
            case ItemID.RAW_SARDINE -> 40.0;
            case ItemID.RAW_HERRING -> 50.0;
            case ItemID.RAW_MACKEREL -> 60.0;
            case ItemID.UNCOOKED_BERRY_PIE -> 78.0;
            case ItemID.RAW_TROUT -> 70.0;
            case ItemID.RAW_COD -> 75.0;
            case ItemID.RAW_PIKE -> 80.0;
            case ItemID.UNCOOKED_MEAT_PIE -> 110.0;
            case ItemID.RAW_SALMON -> 90.0;
            case ItemID.UNCOOKED_STEW -> 117.0;
            case ItemID.RAW_TUNA -> 100.0;
            case ItemID.UNCOOKED_APPLE_PIE -> 130.0;
            case ItemID.RAW_KARAMBWAN -> 190.0;
            case ItemID.RAW_GARDEN_PIE -> 138.0;
            case ItemID.RAW_LOBSTER -> 120.0;
            case ItemID.RAW_BASS -> 130.0;
            case ItemID.RAW_SWORDFISH -> 140.0;
            case ItemID.RAW_FISH_PIE -> 164.0;
            case ItemID.UNCOOKED_BOTANICAL_PIE -> 180.0;
            case ItemID.UNCOOKED_MUSHROOM_PIE -> 200.0;
            case ItemID.UNCOOKED_CURRY -> 280.0;
            case ItemID.RAW_MONKFISH -> 150.0;
            case ItemID.RAW_ADMIRAL_PIE -> 210.0;
            case ItemID.UNCOOKED_DRAGONFRUIT_PIE -> 220.0;
            case ItemID.RAW_SHARK -> 210.0;
            case ItemID.RAW_SEA_TURTLE -> 211.3;
            case ItemID.RAW_ANGLERFISH -> 230.0;
            case ItemID.RAW_WILD_PIE -> 240.0;
            case ItemID.RAW_DARK_CRAB -> 215.0;
            case ItemID.RAW_MANTA_RAY -> 216.2;
            case ItemID.RAW_SUMMER_PIE -> 260.0;
            default -> 1.0; // fallback value
        };
    }
}
