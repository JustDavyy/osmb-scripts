package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.utils.Result;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import main.dCannonballSmelter;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static main.dCannonballSmelter.*;

public class FirstBank extends Task {

    public FirstBank(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return !bankSetupDone;
    }

    @Override
    public boolean execute() {
        UIResult<ItemSearchResult> hasMould = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD);

        if (hasMould.isFound()) {
            UIResult<ItemSearchResult> hasBars = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.STEEL_BAR);
            if (hasBars.isFound()) {
                script.log(getClass().getSimpleName(), "We already have a mould and bars in our inventory, marking bank setup as done.");
                bankSetupDone = true;
                return false;
            }
        }

        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        if (hasMould.isNotFound()) {
            script.getWidgetManager().getBank().depositAll(new int[]{ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD});
            script.submitTask(() -> false, script.random(300, 600));
            UIResult<ItemSearchResult> doubleMould = script.getItemManager().findItem(script.getWidgetManager().getBank(), ItemID.DOUBLE_AMMO_MOULD);
            if (doubleMould.isFound()) {
                script.getWidgetManager().getBank().withdraw(ItemID.DOUBLE_AMMO_MOULD, 1);
            } else {
                UIResult<ItemSearchResult> normalMould = script.getItemManager().findItem(script.getWidgetManager().getBank(), ItemID.AMMO_MOULD);
                if (normalMould.isFound()) {
                    script.getWidgetManager().getBank().withdraw(ItemID.AMMO_MOULD, 1);
                } else {
                    script.log(getClass(), "No ammo moulds available. Stopping script.");
                    script.stop();
                    return false;
                }
            }
        }

        UIResult<ItemSearchResult> steelBarBank = script.getItemManager().findItem(script.getWidgetManager().getBank(), ItemID.STEEL_BAR);
        if (steelBarBank.isNotFound()) {
            script.log(getClass(), "No steel bars in bank. Stopping script.");
            script.stop();
            return false;
        }

        script.getWidgetManager().getBank().withdraw(ItemID.STEEL_BAR, 27);
        script.getWidgetManager().getBank().close();
        script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(5000, 7500));

        UIResult<ItemSearchResult> finalMould = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD);
        UIResult<ItemSearchResult> finalSteel = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.STEEL_BAR);

        if (finalMould.isFound() && finalSteel.isFound()) {
            bankSetupDone = true;
            script.log(getClass(), "Bank setup completed successfully.");
        }

        return false;
    }

    private void openBank() {
        script.log(getClass(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(dCannonballSmelter.bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dCannonballSmelter.BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object.");
            return;
        }

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
}