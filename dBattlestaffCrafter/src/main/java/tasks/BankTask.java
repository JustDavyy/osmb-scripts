package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import main.dBattlestaffCrafter;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static main.dBattlestaffCrafter.*;

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

        // Determine the correct orb based on selected staff
        int orbId = getOrbIdForStaff(staffID);
        if (orbId == -1) {
            script.log(getClass(), "Unknown orb for staff: " + staffID + ", stopping script.");
            script.stop();
            return false;
        }

        // Check if we have both orbs and battlestaffs
        UIResultList<ItemSearchResult> orbsBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), orbId);
        UIResultList<ItemSearchResult> battlestaffsBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), ItemID.BATTLESTAFF);

        if (orbsBank.isNotFound() || battlestaffsBank.isNotFound()) {
            script.log(getClass(), "Ran out of orbs or battlestaffs. Stopping script.");
            script.stop();
            return false;
        }

        boolean randomOrder = script.random(2) == 0;
        int targetAmount = 14;

        if (randomOrder) {
            withdrawWithRetry(orbId, targetAmount);
            withdrawWithRetry(ItemID.BATTLESTAFF, targetAmount);
        } else {
            withdrawWithRetry(ItemID.BATTLESTAFF, targetAmount);
            withdrawWithRetry(orbId, targetAmount);
        }

        closeBankWithRetry();
        script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), 5000);
        shouldBank = false;

        return false;
    }

    private void openBank() {
        script.log(getClass(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(dBattlestaffCrafter.bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dBattlestaffCrafter.BANK_ACTIONS)) {
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

    private int getOrbIdForStaff(int staffId) {
        return switch (staffId) {
            case ItemID.AIR_BATTLESTAFF -> ItemID.AIR_ORB;
            case ItemID.WATER_BATTLESTAFF -> ItemID.WATER_ORB;
            case ItemID.EARTH_BATTLESTAFF -> ItemID.EARTH_ORB;
            case ItemID.FIRE_BATTLESTAFF -> ItemID.FIRE_ORB;
            default -> -1;
        };
    }
}
