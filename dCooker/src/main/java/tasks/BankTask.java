package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import main.dCooker;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCooker.*;

public class BankTask extends Task {

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        // Always activate this task, it's last and prevents getting stuck
        return shouldBank;
    }

    @Override
    public boolean execute() {
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        script.log(getClass(), "Depositing full inventory...");
        script.getWidgetManager().getBank().depositAll(new int[0]);

        UIResultList<ItemSearchResult> itemBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), cookingItemID);
        if (itemBank.isNotFound()) {
            script.log(getClass(), "Ran out of food to cook. Stopping script.");
            script.stop();
            return false;
        }

        int withdrawAmount = (cookingItemID == ItemID.GIANT_SEAWEED) ? 4 : 28;

        withdrawWithRetry(cookingItemID, withdrawAmount);

        closeBankWithRetry();
        script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), 5000);
        shouldBank = false;

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
        }, 15000, true, false, true);
    }

    private void withdrawWithRetry(int itemID, int amount) {
        if (!script.getWidgetManager().getBank().withdraw(itemID, amount)) {
            script.log(getClass(), "Withdraw failed for " + itemID + ", retrying...");
            script.getWidgetManager().getBank().withdraw(itemID, amount);
        }
    }

    private void closeBankWithRetry() {
        if (!script.getWidgetManager().getBank().close()) {
            script.log(getClass(), "Bank close failed, retrying...");
            script.getWidgetManager().getBank().close();
        }
    }
}
