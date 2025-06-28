package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Equipment;
import com.osmb.api.utils.UIResult;
import data.FishingLocation;
import utils.Task;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class Setup extends Task {
    // Ardougne cloak item IDs
    private final int[] cloakIds = {
            ItemID.ARDOUGNE_CLOAK_1,
            ItemID.ARDOUGNE_CLOAK_2,
            ItemID.ARDOUGNE_CLOAK_3,
            ItemID.ARDOUGNE_CLOAK_4
    };

    // Quest cape item IDs
    private final int[] qcapeIds = {
            ItemID.QUEST_POINT_CAPE,
            ItemID.QUEST_POINT_CAPE_T
    };

    // Harpoon with spec item IDs
    private final int[] harpoonIds = {
            ItemID.DRAGON_HARPOON,
            ItemID.DRAGON_HARPOON_OR,
            ItemID.DRAGON_HARPOON_OR_30349,
            ItemID.CRYSTAL_HARPOON,
            ItemID.CRYSTAL_HARPOON_23864,
            ItemID.CRYSTAL_HARPOON_INACTIVE,
            ItemID.INFERNAL_HARPOON,
            ItemID.INFERNAL_HARPOON_OR,
            ItemID.INFERNAL_HARPOON_OR_30342,
            ItemID.INFERNAL_HARPOON_UNCHARGED,
            ItemID.INFERNAL_HARPOON_UNCHARGED_25367,
            ItemID.INFERNAL_HARPOON_UNCHARGED_30343
    };

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
                script.getWidgetManager().getLogoutTab().logout();
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
            if (fishingLocation.equals(FishingLocation.Minnows)) {
                ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(Set.of(ItemID.MINNOW));

                if (inventorySnapshot == null) {
                    // Inventory not visible
                    return false;
                }

                task = "Check minnow start count";
                script.log(getClass(), "Checking initial Minnow count.");
                startAmount = invCheck.getAmount(ItemID.MINNOW);
                script.log(getClass(), "Minnow start count detected: " + startAmount);
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

        // Reset timer
        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute – resetting as we just performed an action.");
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
        }

        // Karambwan logic
        if (fishingLocation.equals(FishingLocation.Karambwans)) {
            // Check if we have Ardougne cloak equipped if we use that fairy ring option
            if (fairyOption.equals("Ardougne cloak")) {
                task = "Check ardy cloak";
                script.log(getClass().getSimpleName(), "Ardougne cloak option is selected, checking if it's equipped...");
                Equipment equipment = script.getWidgetManager().getEquipment();
                UIResult<Boolean> result = equipment.isEquiped(cloakIds);

                if (result.isFound()) {
                    script.log(getClass().getSimpleName(), "One of the ardougne cloaks is found to be equipped.");
                    // One of the cloaks is equipped, find which one
                    for (int cloakId : cloakIds) {
                        UIResult<Boolean> check = equipment.isEquiped(cloakId);
                        if (check.isFound()) {
                            equippedCloakId = cloakId;
                            script.log(getClass().getSimpleName(), "Ardougne cloak equipped: " + script.getItemManager().getItemName(equippedCloakId) + " (" + equippedCloakId + ").");
                            break;
                        }
                    }
                } else {
                    script.log(getClass().getSimpleName(), "Ardougne cloak is not equipped. Please make sure it is and restart the script.");
                    script.stop();
                    return false;
                }
            }

            // Check if we have Quest cape equipped if we use that fairy ring option
            if (fairyOption.equals("Quest cape")) {
                task = "Check quest cape";
                script.log(getClass().getSimpleName(), "Quest cape option is selected, checking if it's equipped...");
                Equipment equipment = script.getWidgetManager().getEquipment();
                UIResult<Boolean> result = equipment.isEquiped(qcapeIds);

                if (result.isFound()) {
                    script.log(getClass().getSimpleName(), "One of the quest capes is found to be equipped.");
                    // One of the capes is equipped, find which one
                    for (int cloakId : qcapeIds) {
                        UIResult<Boolean> check = equipment.isEquiped(cloakId);
                        if (check.isFound()) {
                            equippedCloakId = cloakId;
                            script.log(getClass().getSimpleName(), "Quest cape equipped: " + script.getItemManager().getItemName(equippedCloakId) + " (" + equippedCloakId + ").");
                            break;
                        }
                    }
                } else {
                    script.log(getClass().getSimpleName(), "Quest cape is not equipped. Please make sure it is and restart the script.");
                    script.stop();
                    return false;
                }
            }

            // Check if we have a crafting cape in our inventory if we use that bank option
            if (bankOption.equals("Crafting Guild")) {
                task = "Check crafting cape";
                script.log(getClass().getSimpleName(), "Crafting Guild bank option is selected, checking if we have the cape.");
                inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET));

                if (inventorySnapshot == null) {
                    // Inventory not visible
                    return false;
                }

                if (inventorySnapshot.contains(ItemID.CRAFTING_CAPE)) {
                    script.log(getClass().getSimpleName(), "Untrimmed crafting cape found in inventory, storing item ID...");
                    teleportCapeId = ItemID.CRAFTING_CAPE;
                    script.log(getClass().getSimpleName(), "Teleport item: " + script.getItemManager().getItemName(teleportCapeId) + "(" + teleportCapeId + ").");
                } else if (inventorySnapshot.contains(ItemID.CRAFTING_CAPET)) {
                    script.log(getClass().getSimpleName(), "Trimmed crafting cape found in inventory, storing item ID...");
                    teleportCapeId = ItemID.CRAFTING_CAPET;
                    script.log(getClass().getSimpleName(), "Teleport item: " + script.getItemManager().getItemName(teleportCapeId) + "(" + teleportCapeId + ").");
                } else {
                    script.log(getClass().getSimpleName(), "Crafting cape is not in inventory while option is selected. Please make sure it is and restart the script.");
                    script.stop();
                    return false;
                }
            }
        }

        task = "Finish set up";
        currentPos = script.getWorldPosition();
        setupDone = true;
        return false;
    }
}
