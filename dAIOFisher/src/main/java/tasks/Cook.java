package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.ocr.fonts.Font;
import data.FishingLocation;
import utils.Task;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static main.dAIOFisher.*;

public class Cook extends Task {

    public Cook(Script script) {
        super(script);
    }

    public boolean activate() {
        if (!cookMode) return false;
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return false;
        }
        ItemGroupResult inv;
        inv = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getCatchableFish()));
        return inv != null && inv.isFull() && inv.containsAny(Set.copyOf(fishingMethod.getCatchableFish()));
    }

    public boolean execute() {
        task = "Find and cook raw fish";

        List<Integer> rawFish = fishingMethod.getCatchableFish();
        ItemGroupResult inventory = script.getWidgetManager().getInventory().search(Set.copyOf(rawFish));
        if (inventory == null || inventory.isEmpty()) {
            script.log(getClass(), "No raw fish found in inventory.");
            return false;
        }

        // Different cooking method for eels
        if (fishingLocation.equals(FishingLocation.Zul_Andra) || fishingLocation.equals(FishingLocation.Mor_Ul_Rek_East) || fishingLocation.equals(FishingLocation.Mor_Ul_Rek_West)) {
            // Select which tool/fish to use
            int toolToUse = 0;
            int fishToUse = 0;
            if (fishingLocation.equals(FishingLocation.Zul_Andra)) {
                toolToUse = ItemID.KNIFE;
                fishToUse = ItemID.SACRED_EEL;
            } else {
                toolToUse = ItemID.HAMMER;
                fishToUse = ItemID.INFERNAL_EEL;
            }

            task = "Start cooking action";
            inventory = script.getWidgetManager().getInventory().search(Set.of(fishToUse, toolToUse));
            if (inventory == null) {
                script.log(getClass(), "Inventory could not be found");
                return false;
            }

            if (!alreadyCountedFish) {
                fish1Caught += inventory.getAmount(fishToUse);
                alreadyCountedFish = true;
            }

            boolean clickSuccess;
            int clickOrder = script.random(1, 2);
            if (clickOrder == 1) {
                // Click fish first, then tool
                clickSuccess = inventory.getItem(fishToUse).interact() &&
                        inventory.getItem(toolToUse).interact();
            } else {
                // Click tool first, then fish
                clickSuccess = inventory.getItem(toolToUse).interact() &&
                        inventory.getItem(fishToUse).interact();
            }

            if (!clickSuccess) {
                return false;
            }

            // We're now cutting the eels, wait to complete.
            waitUntilFinishedCutting(fishToUse);
        }

        for (int rawId : rawFish) {
            if (!inventory.contains(rawId)) continue;

            RSObject cookObject = getClosestCookObject(fishingMethod.getCookingObjectName(), fishingMethod.getCookingObjectAction());
            if (cookObject == null) {
                script.log(getClass(), "No cookable object found nearby (" + fishingMethod.getCookingObjectName() + ").");
                return false;
            }

            task = "Interact with object";
            if (!cookObject.interact(fishingMethod.getCookingObjectAction())) {
                script.log(getClass(), "Failed to interact with cooking object. Retrying...");
                if (!cookObject.interact(fishingMethod.getCookingObjectAction())) {
                    return false;
                }
            }

            task = "Start cooking action";
            BooleanSupplier condition = () -> script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
            script.submitHumanTask(condition, script.random(4000, 6000));

            if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION) {
                int cookedId = fishingMethod.getCookedFish().get(rawFish.indexOf(rawId));

                boolean selected = script.getWidgetManager().getDialogue().selectItem(rawId)
                        || script.getWidgetManager().getDialogue().selectItem(cookedId);

                if (!selected) {
                    script.log(getClass(), "Initial food selection failed, retrying...");
                    script.submitHumanTask(() -> false, script.random(150, 300));

                    selected = script.getWidgetManager().getDialogue().selectItem(rawId)
                            || script.getWidgetManager().getDialogue().selectItem(cookedId);
                }

                if (!selected) {
                    script.log(getClass(), "Failed to select food item in dialogue after retry.");
                    continue;
                }

                script.log(getClass(), "Selected food to cook: " + rawId + "/" + cookedId);
                waitUntilFinishedCooking(Set.of(rawId), cookedId);

                return false; // let next execution handle next type
            }
        }

        return false;
    }

    private RSObject getClosestCookObject(String name, String requiredAction) {
        List<RSObject> objects = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(name)
                    && Arrays.stream(gameObject.getActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(requiredAction))
                    && gameObject.canReach();
        });

        if (objects.isEmpty()) {
            script.log(getClass(), "No objects found matching query for: " + name + " with action: " + requiredAction);
            return null;
        }

        RSObject closest = (RSObject) script.getUtils().getClosest(objects);
        if (closest == null) {
            script.log(getClass(), "Closest object is null.");
        }
        return closest;
    }

    private void waitUntilFinishedCooking(Set<Integer> itemIdsToWatch, int cookedId) {
        task = "Wait until cooking finish";
        Timer amountChangeTimer = new Timer();

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute – resetting as we just performed an action.");
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
        }

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.submitHumanTask(() -> false, script.random(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 66000) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(itemIdsToWatch);
            if (inventorySnapshot == null) return false;

            if (!readyToReadCookingXP) {
                ItemGroupResult postCookInv = script.getWidgetManager().getInventory().search(Set.of(cookedId));
                if (postCookInv != null && postCookInv.getAmount(cookedId) > 0) {
                    script.log(getClass(), "Cooked at least one " + cookedId + ", enabling cooking XP tracking.");
                    readyToReadCookingXP = true;
                }
            }

            if (readyToReadCookingXP) {
                readCookingXp();
            }

            return itemIdsToWatch.stream().noneMatch(inventorySnapshot::contains);
        };

        script.log(getClass(), "Using human task to wait until cooking finishes.");
        script.submitHumanTask(condition, script.random(66000, 70000));
    }

    private void waitUntilFinishedCutting(int itemIdToWatch) {
        task = "Wait until cooking finish";

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute – resetting as we just performed an action.");
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
        }

        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.submitHumanTask(() -> false, script.random(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 66000) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(itemIdToWatch));
            if (inventorySnapshot == null) return false;

            int fishLeft = inventorySnapshot.getAmount(itemIdToWatch);

            if (!readyToReadCookingXP) {
                ItemGroupResult postCookInv = script.getWidgetManager().getInventory().search(Set.of(itemIdToWatch));
                if (postCookInv != null && postCookInv.getAmount(itemIdToWatch) < fishLeft) {
                    script.log(getClass(), "Processed at least one " + script.getItemManager().getItemName(itemIdToWatch) + ", enabling cooking XP tracking.");
                    readyToReadCookingXP = true;
                }
            }

            if (readyToReadCookingXP) {
                readCookingXp();
            }

            return !inventorySnapshot.contains(itemIdToWatch);
        };

        script.log(getClass(), "Using human task to wait until cooking finishes.");
        script.submitHumanTask(condition, script.random(66000, 70000));
    }

    private void readCookingXp() {
        XPDropsComponent xpComponent = (XPDropsComponent) script.getWidgetManager().getComponent(XPDropsComponent.class);

        if (xpComponent == null) {
            script.log(getClass(), "XP button component not found.");
            return;
        }

        ComponentSearchResult<Integer> result = xpComponent.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) return;

        Rectangle componentBounds = result.getBounds();
        Rectangle xpTextRect = new Rectangle(componentBounds.x - 140, componentBounds.y - 1, 119, 38);

        script.submitTask(() -> false, script.random(200, 400));
        String xpText = script.getOCR().getText(Font.SMALL_FONT, xpTextRect, Color.WHITE.getRGB());

        if (xpText == null || xpText.isBlank()) return;
        xpText = xpText.replaceAll("[^\\d]", "");
        if (xpText.isEmpty()) return;

        try {
            double currentXp = Double.parseDouble(xpText);
            if (currentXp <= 0) return;

            if (previousCookingXpRead < 0) {
                previousCookingXpRead = currentXp;
                return;
            }

            double xpGained = currentXp - previousCookingXpRead;
            if (xpGained > 0 && xpGained <= 15000) {
                cookingXp += xpGained;
                script.log(getClass(), "Cooking XP gained: " + xpGained + " (" + cookingXp + ")");
                previousCookingXpRead = currentXp;
                lastXpGained = System.currentTimeMillis();
            }

        } catch (NumberFormatException e) {
            script.log(getClass(), "Failed to parse Cooking XP text: " + xpText);
        }
    }

}
