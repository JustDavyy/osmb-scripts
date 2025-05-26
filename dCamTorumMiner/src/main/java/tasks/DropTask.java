package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Collections;
import java.util.Set;

import static main.dCamTorumMiner.*;

public class DropTask extends Task {
    private int prevClueBeginner = 0;
    private int prevClueEasy = 0;
    private int prevClueMedium = 0;
    private int prevClueHard = 0;
    private int prevClueElite = 0;
    private int prevLoopKey = 0;

    private static final Set<Integer> DROP_ITEMS = Set.of(
            ItemID.UNCUT_SAPPHIRE,
            ItemID.UNCUT_EMERALD,
            ItemID.UNCUT_RUBY,
            ItemID.UNCUT_DIAMOND,
            ItemID.CALCIFIED_DEPOSIT
    );

    public DropTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!dropMode) {
            return false;
        }
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inv != null && inv.isFull();
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(getTrackedItemIDs());
        if (inv == null) return false;

        int totalXp = 0;

        // Dropped items XP
        int sapphires = safeAmount(inv.getAmount(ItemID.UNCUT_SAPPHIRE));
        int emeralds = safeAmount(inv.getAmount(ItemID.UNCUT_EMERALD));
        int rubies = safeAmount(inv.getAmount(ItemID.UNCUT_RUBY));
        int diamonds = safeAmount(inv.getAmount(ItemID.UNCUT_DIAMOND));
        int deposits = safeAmount(inv.getAmount(ItemID.CALCIFIED_DEPOSIT));

        int dropXp = (sapphires + emeralds + rubies + diamonds + deposits) * 33;
        totalXp += dropXp;

        script.log(getClass(), String.format("Dropped: Sapphires=%d, Emeralds=%d, Rubies=%d, Diamonds=%d, Deposits=%d → +%d XP",
                sapphires, emeralds, rubies, diamonds, deposits, dropXp));

        // Kept items XP
        int curBeginner = safeAmount(inv.getAmount(ItemID.CLUE_GEODE_BEGINNER));
        int curEasy = safeAmount(inv.getAmount(ItemID.CLUE_GEODE_EASY));
        int curMedium = safeAmount(inv.getAmount(ItemID.CLUE_GEODE_MEDIUM));
        int curHard = safeAmount(inv.getAmount(ItemID.CLUE_GEODE_HARD));
        int curElite = safeAmount(inv.getAmount(ItemID.CLUE_GEODE_ELITE));
        int curLoop = safeAmount(inv.getAmount(ItemID.LOOP_HALF_OF_KEY_30107));

        int gainedBeginner = Math.max(0, curBeginner - prevClueBeginner);
        int gainedEasy = Math.max(0, curEasy - prevClueEasy);
        int gainedMedium = Math.max(0, curMedium - prevClueMedium);
        int gainedHard = Math.max(0, curHard - prevClueHard);
        int gainedElite = Math.max(0, curElite - prevClueElite);
        int gainedLoop = Math.max(0, curLoop - prevLoopKey);

        int keptXp = (gainedBeginner + gainedEasy + gainedMedium + gainedHard + gainedElite + gainedLoop) * 33;
        totalXp += keptXp;

        script.log(getClass(), String.format("Kept: Beginner=%d (%d→%d), Easy=%d (%d→%d), Medium=%d (%d→%d), Hard=%d (%d→%d), Elite=%d (%d→%d), LoopKeys=%d (%d→%d) → +%d XP",
                gainedBeginner, prevClueBeginner, curBeginner,
                gainedEasy, prevClueEasy, curEasy,
                gainedMedium, prevClueMedium, curMedium,
                gainedHard, prevClueHard, curHard,
                gainedElite, prevClueElite, curElite,
                gainedLoop, prevLoopKey, curLoop,
                keptXp));

        task = "Add XP gained for solid items";
        // Add XP once
        miningXpGained += totalXp;
        script.log(getClass(), "Total mining XP added: " + totalXp);

        // Update previous counts
        prevClueBeginner = curBeginner;
        prevClueEasy = curEasy;
        prevClueMedium = curMedium;
        prevClueHard = curHard;
        prevClueElite = curElite;
        prevLoopKey = curLoop;

        task = "Drop uncuts, deposits and keys";
        // Attempt dropping up to 3 times if needed
        for (int attempt = 1; attempt <= 3; attempt++) {
            boolean droppedAll = script.getWidgetManager().getInventory().dropItems(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND, ItemID.CALCIFIED_DEPOSIT, ItemID.LOOP_HALF_OF_KEY_30107);
            if (droppedAll) {
                script.log(getClass(), "Successfully dropped all items (attempt " + attempt + ")");
                return true;
            } else {
                script.log(getClass(), "Drop attempt " + attempt + " incomplete, retrying...");
                script.submitHumanTask(() -> false, script.random(250, 450));
            }
        }

        script.log(getClass(), "Failed to drop all items after 3 attempts.");
        return true;
    }

    private int safeAmount(int amount) {
        return Math.max(amount, 0);
    }

    private Set<Integer> getTrackedItemIDs() {
        return Set.of(
                ItemID.UNCUT_SAPPHIRE,
                ItemID.UNCUT_EMERALD,
                ItemID.UNCUT_RUBY,
                ItemID.UNCUT_DIAMOND,
                ItemID.CALCIFIED_DEPOSIT,
                ItemID.CLUE_GEODE_BEGINNER,
                ItemID.CLUE_GEODE_EASY,
                ItemID.CLUE_GEODE_MEDIUM,
                ItemID.CLUE_GEODE_HARD,
                ItemID.CLUE_GEODE_ELITE,
                ItemID.LOOP_HALF_OF_KEY_30107
        );
    }
}