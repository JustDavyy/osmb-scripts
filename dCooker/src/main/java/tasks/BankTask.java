package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import main.dCooker;
import utils.Task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCooker.bankMethod;
import static main.dCooker.cookingItemID;

public class BankTask extends Task {

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public boolean execute() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        if (!script.getWidgetManager().getBank().depositAll(Set.of(cookingItemID))) {
            return false;
        }
        // work out our target amount
        int targetAmount = script.getWidgetManager().getInventory().getGroupSize();
        if (cookingItemID == ItemID.GIANT_SEAWEED) {
            targetAmount /= 6;
        }

        // here we can simply work out how many we need to withdraw,
        // if the amount is below 0, it would also mean we have
        // too many and would want to deposit the absolute value of the negative result
        int amountNeeded = targetAmount - inventorySnapshot.getAmount(cookingItemID);

        if (amountNeeded == 0) {
            // banking complete
            script.getWidgetManager().getBank().close();
            script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        } else if (amountNeeded > 0) {
            // need to withdraw
            ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(cookingItemID));
            if (!bankSnapshot.contains(cookingItemID)) {
                script.log(getClass(), "Ran out of food to cook. Stopping script.");
                script.stop();
                return false;
            }
            if (!script.getWidgetManager().getBank().withdraw(cookingItemID, targetAmount)) {
                script.log(getClass(), "Withdraw failed for item id: " + cookingItemID);
                return false;
            }
            script.getWidgetManager().getBank().close();
            script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        } else {
            // need to deposit...
            if (bankMethod.equals("Deposit all")) {
                if (!script.getWidgetManager().getBank().depositAll(Set.of(cookingItemID))) {
                    script.log(getClass().getSimpleName(), "Deposit all action failed.");
                    return false;
                }
            } else {
                if (!script.getWidgetManager().getBank().deposit(cookingItemID, Math.abs(targetAmount))) {
                    script.log(getClass().getSimpleName(), "Deposit item by item action failed.");
                    return false;
                }
            }
        }
        return false;
    }

    private void openBank() {
        script.log(getClass(), "Searching for a bank...");

        if (script.getWorldPosition() != null && script.getWorldPosition().getRegionID() == 12109) {
            // We are in 12109, use the NPC tile
            WorldPosition npcTilePosition = new WorldPosition(3042, 4972, 1);

            RSTile npcTile = script.getSceneManager().getTile(npcTilePosition);
            if (npcTile == null) {
                script.log(getClass(), "NPC tile is null, cannot open bank via NPC.");
                return;
            }

            if (!npcTile.isOnGameScreen()) {
                script.log(getClass(), "NPC tile not on screen, walking...");
                script.getWalker().walkTo(npcTilePosition);
                return;
            }

            script.log(getClass(), "Interacting with NPC tile for banking...");
            if (!script.getFinger().tap(npcTile.getTileCube(110).getResized(0.4), "Bank")) {
                script.log(getClass(), "Failed to long-press 'Bank' on NPC tile.");
                return;
            }
        } else {
            // Regular bank object
            List<RSObject> banksFound = script.getObjectManager().getObjects(dCooker.bankQuery);
            if (banksFound.isEmpty()) {
                script.log(getClass(), "No bank objects found.");
                return;
            }

            RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
            if (!bank.interact(dCooker.BANK_ACTIONS)) {
                script.log(getClass(), "Failed to interact with bank object.");
                return;
            }
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
}
