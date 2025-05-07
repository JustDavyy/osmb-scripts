package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCannonballSmelter.*;

public class BankTask extends Task {

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return script.getWidgetManager().getInventory().search(Set.of(ItemID.STEEL_BAR)).isEmpty() || script.getWidgetManager().getBank().isVisible();
    }

    @Override
    public boolean execute() {
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        boolean depositCannonballs = script.random(100) < 85;
        if (depositCannonballs) {
            script.log(getClass(), "Depositing cannonballs (85% case).");
            script.getWidgetManager().getBank().depositAll(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD));
        } else {
            script.log(getClass(), "Skipping cannonball deposit (15% case).");
        }

        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.STEEL_BAR));

        if (bankSnapshot.isEmpty()) {
            script.log(getClass(), "Ran out of supplies. Stopping script.");
            script.stop();
            return false;
        }

        script.getWidgetManager().getBank().withdraw(ItemID.STEEL_BAR, 27);
        script.getWidgetManager().getBank().close();
        script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(5000, 7500));

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.STEEL_BAR));
        if (inventorySnapshot.isEmpty()) {
            script.log(getClass(), "No steel bars in inventory. Exiting banking logic.");
            return false;
        }

        return false;
    }

    private void openBank() {
        script.log(getClass(), "Opening bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (!banksFound.isEmpty()) {
            RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
            bank.interact(BANK_ACTIONS);
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
        }, 15000);
    }
}
