package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import data.FishingMethod;
import utils.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static main.dAIOFisher.*;

public class Bank extends Task {
    private final Set<Integer> ignoreItems = new HashSet<>();

    public Bank(Script script) {
        super(script);
        ignoreItems.addAll(fishingMethod.getRequiredTools());
        Collections.addAll(ignoreItems,
                ItemID.SPIRIT_FLAKES,
                ItemID.FISH_BARREL,
                ItemID.OPEN_FISH_BARREL
        );
    }

    public boolean activate() {
        return bankMode;
    }

    public boolean execute() {
        task = getClass().getSimpleName();

        WorldPosition myPos = script.getWorldPosition();

        // Move to bank first if we're not there yet
        if (!fishingLocation.getBankArea().contains(myPos)) {
            task = "Moving to bank area";
            return script.getWalker().walkTo(fishingLocation.getBankArea().getRandomPosition());
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getAllFish()));
        if (!alreadyCountedFish) {
            if (inv == null) return false;

            for (int i = 0; i < fishingMethod.getAllFish().size(); i++) {
                int id = fishingMethod.getAllFish().get(i);
                int count = inv.getAmount(id);

                switch (i) {
                    case 0 -> fish1Caught += count;
                    case 1 -> fish2Caught += count;
                    case 2 -> fish3Caught += count;
                    case 3 -> fish4Caught += count;
                    case 4 -> fish5Caught += count;
                    case 5 -> fish6Caught += count;
                    case 6 -> fish7Caught += count;
                    case 7 -> fish8Caught += count;
                }
            }

            alreadyCountedFish = true;
        }

        // Open the correct bank type
        if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK)) {
            if (!script.getWidgetManager().getBank().isVisible()) {
                openBank();
                return false;
            } else {
                script.log(getClass(), "Bank interface is visible.");
            }
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {
            if (!script.getWidgetManager().getDepositBox().isVisible()) {
                openDepositBox();
                return false;
            } else {
                script.log(getClass(), "Deposit box interface is visible.");
            }
        }

        // Deposit items
        task = "Deposit items";
        if (usingBarrel && inv.containsAny(Set.copyOf(fishingMethod.getAllFish()))) {
            task = "Empty fish barrel";
            script.log(getClass().getSimpleName(), "Emptying fish barrel in the bank");
            if (!inv.getItem(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL).interact("Empty")) {
                return false;
            }

            fish1Caught += 28;
        }

        if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK)) {
            if (!script.getWidgetManager().getBank().depositAll(Set.copyOf(ignoreItems))) {
                script.log(getClass().getSimpleName(), "Deposit items failed.");
                return false;
            } else {
                script.log(getClass(), "Deposit items was successful.");
            }
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {
            if (!script.getWidgetManager().getDepositBox().depositAll(Set.copyOf(ignoreItems))) {
                script.log(getClass().getSimpleName(), "Deposit items failed.");
                return false;
            } else {
                script.log(getClass(), "Deposit items was successful.");
            }
        }


        task = "Close bank";
        if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK)) {
            script.getWidgetManager().getBank().close();
            script.submitTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {
            script.getWidgetManager().getDepositBox().close();
            script.submitTask(() -> !script.getWidgetManager().getDepositBox().isVisible(), script.random(4000, 6000));
        }

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Searching for a bank...");

        task = "Get bank name/action";
        String bankName = fishingMethod.getBankObjectName();
        String bankAction = fishingMethod.getBankObjectAction();

        if (bankName == null || bankAction == null) {
            script.log(getClass(), "Bank name or action is not defined in fishingMethod.");
            return;
        }

        task = "Get bank objects";
        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(bankName)
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && action.equalsIgnoreCase(bankAction))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found matching name: " + bankName + " and action: " + bankAction);
            return;
        }

        task = "Interact with bank object";
        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(bankAction)) {
            script.log(getClass(), "Failed to interact with bank object.");
            return;
        }

        // Wait for banking UI to appear or player to stop moving
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for bank to open";
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

    private void openDepositBox() {
        task = "Open Deposit box";
        script.log(getClass(), "Searching for a deposit box...");

        task = "Get bank name/action";
        String bankName = fishingMethod.getBankObjectName();
        String bankAction = fishingMethod.getBankObjectAction();

        if (bankName == null || bankAction == null) {
            script.log(getClass(), "Bank name or action is not defined in fishingMethod.");
            return;
        }

        task = "Get deposit box objects";
        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(bankName)
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && action.equalsIgnoreCase(bankAction))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            script.log(getClass(), "No deposit box objects found matching name: " + bankName + " and action: " + bankAction);
            return;
        }

        task = "Interact with deposit box object";
        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(bankAction)) {
            script.log(getClass(), "Failed to interact with deposit box object.");
            return;
        }

        // Wait for deposit box UI to appear or player to stop moving
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for deposit box to open";
        script.submitTask(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getDepositBox().isVisible() || positionChangeTimer.get().timeElapsed() > 3000;
        }, script.random(14000, 16000));
    }
}
