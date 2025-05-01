package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.utils.Result;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import main.dCooker;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCooker.cookingItemID;

public class BankTask extends Task {

    private UIResultList<ItemSearchResult> itemInventory;

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        // get inventory amount here, if true execute is processed right after, so no need to search again
        itemInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), cookingItemID);
        if (!checkResult(itemInventory, true)) {
            return true;
        }
        // execute the task if no food
        return itemInventory.isEmpty() || script.getWidgetManager().getBank().isVisible();
    }

    @Override
    public boolean execute() {
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        if (!script.getWidgetManager().getBank().depositAll(new int[]{cookingItemID})) {
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
        int amountNeeded = targetAmount - itemInventory.size();

        if (amountNeeded == 0) {
            // banking complete
            script.getWidgetManager().getBank().close();
            script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        } else if (amountNeeded > 0) {
            // need to withdraw
            UIResultList<ItemSearchResult> itemBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), cookingItemID);
            if (!checkResult(itemBank, false)) {
                return false;
            }
            if (!script.getWidgetManager().getBank().withdraw(cookingItemID, targetAmount)) {
                script.log(getClass(), "Withdraw failed for item id: " + cookingItemID);
                return false;
            }
        } else {
            // need to deposit...
            if (!script.getWidgetManager().getBank().deposit(cookingItemID, Math.abs(targetAmount))) {
                script.log(getClass(), "Withdraw failed for item id: " + cookingItemID);
                return false;
            }
        }
        return false;
    }

    private boolean checkResult(Result result, boolean inventory) {
        if (result.isNotVisible()) {
            return false;
        }
        if (result.isNotFound()) {
            if (!inventory) {
                script.log(getClass(), "Ran out of food to cook. Stopping script.");
                script.stop();
            }
            return false;
        }
        return true;
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
        }, script.random(14000, 16000), true, false, true);
    }
}
