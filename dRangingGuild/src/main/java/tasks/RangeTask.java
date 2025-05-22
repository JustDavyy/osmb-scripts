package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import utils.Task;
import static main.dRangingGuild.*;
import com.osmb.api.script.Script;
import com.osmb.api.scene.RSObject;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Arrays;

public class RangeTask extends Task {

    public static RSObject cachedTarget2 = null;

    private static final String TARGET_NAME = "Target";
    private static final String TARGET_ACTION = "Fire-at";
    private static final WorldPosition POS1 = new WorldPosition(2679, 3426, 0);
    private static final WorldPosition POS2 = new WorldPosition(2681, 3425, 0);

    public static final Predicate<RSObject> targetQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;

        if (!gameObject.getName().equalsIgnoreCase(TARGET_NAME)) return false;

        for (String action : gameObject.getActions()) {
            if (action != null && action.equalsIgnoreCase(TARGET_ACTION)) {
                return gameObject.isInteractable();
            }
        }

        return false;
    };

    public RangeTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return readyToShoot;
    }

    @Override
    public boolean execute() {
        task = "Shooting target (" + shotsLeft + " left)";
        script.log(getClass().getSimpleName(), "🎯 Attempting shot...");

        competitionDialogueCounter = 0;

        // Find or reuse cached target
        if (cachedTarget2 == null || !cachedTarget2.isInteractableOnScreen()) {
            List<RSObject> allObjects = script.getObjectManager().getObjects(targetQuery);
            script.log(getClass().getSimpleName(), "🧪 Found " + allObjects.size() + " total objects.");

            for (RSObject obj : allObjects) {
                if (obj == null) continue;

                String name = obj.getName();
                String[] actions = obj.getActions();
                WorldPosition pos = obj.getWorldPosition();

                script.log(getClass().getSimpleName(), "🔍 Inspecting object: " +
                        "Name=" + name + ", Pos=" + pos + ", Actions=" + Arrays.toString(actions));

                if (name == null || actions == null) {
                    script.log(getClass().getSimpleName(), "⚠ Skipped object due to null name or actions.");
                    continue;
                }

                if (!name.equalsIgnoreCase(TARGET_NAME)) continue;
                if (!Arrays.asList(actions).contains(TARGET_ACTION)) {
                    script.log(getClass().getSimpleName(), "⛔ Skipped object at " + pos + " - no matching 'Fire-at' action.");
                    continue;
                }

                if (!pos.equals(POS1) && !pos.equals(POS2)) {
                    script.log(getClass().getSimpleName(), "⛔ Skipped object at " + pos + " - position not matched.");
                    continue;
                }

                script.log(getClass().getSimpleName(), "✅ Valid target found at " + pos);
                cachedTarget2 = obj;
                break;
            }

            if (cachedTarget2 == null) {
                script.log(getClass().getSimpleName(), "❌ No valid target found after filtering.");
                failSafeNeeded = true;
                return false;
            }
        }

        // Check for dialogues first
        if (script.getWidgetManager().getDialogue().getDialogueType() != null) {
            if (script.getWidgetManager().getDialogue().getDialogueType().equals(DialogueType.CHAT_DIALOGUE)) {
                String dialogueText = script.getWidgetManager().getDialogue().getText().toString().toLowerCase();

                if (dialogueText.contains("suggest you use".toLowerCase())) {
                    task = "Check inventory";
                    ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BRONZE_ARROW));

                    if (inventorySnapshot == null) {
                        // Inventory not visible
                        return false;
                    }

                    if (inventorySnapshot.contains(ItemID.BRONZE_ARROW)) {
                        script.log(getClass().getSimpleName(), "Bronze arrows found, equipping!");
                        UIResult<Rectangle> tappableSlot = inventorySnapshot.getItem(ItemID.BRONZE_ARROW).getTappableBounds();
                        boolean success = script.getFinger().tap(tappableSlot.get().getRandomPoint());
                        script.submitHumanTask(() -> false, script.random(100, 250));

                        if (success) {
                            readyToShoot = true;
                            shotsLeft = 10;
                            return true;
                        }
                    }
                }

                if (dialogueText.contains("that score you will".toLowerCase())) {
                    readyToShoot = false;
                    shotsLeft = 0;
                    return false;
                }
            }
        }

        // Step 1: Fire at the target using tile cube instead of long-press
        RSTile tile = script.getSceneManager().getTile(cachedTarget2.getWorldPosition());
        if (tile == null) {
            script.log(getClass(), "❌ Failed to get RSTile for target position.");
            return false;
        }

        if (!tile.isOnGameScreen()) {
            script.log(getClass(), "❌ Target tile is not on the game screen.");
            return false;
        }

        Polygon targetPoly = tile.getTileCube(40, 45).getResized(0.6);
        if (targetPoly == null) {
            script.log(getClass(), "❌ Failed to get tile cube for target.");
            return false;
        }

        boolean success = script.getFinger().tap(targetPoly);

        if (!success) {
            script.log(getClass(), "❌ Failed to tap target tile.");
            return false;
        }

        lastTaskRanAt = System.currentTimeMillis();

        // Step 2: Wait for target interface to disappear
        if (!script.submitTask(() -> !targetInterface.isVisible(), script.random(1750, 2500), true, true)) {
            script.log(getClass().getSimpleName(), "❌ Target interface did not disappear.");
            return false;
        }

        // Step 3: Wait for it to reappear
        if (!script.submitHumanTask(() -> targetInterface.isVisible() || script.getWidgetManager().getDialogue().getDialogueType() != null, script.random(4000, 5000), true, true)) {
            script.log(getClass().getSimpleName(), "❌ Target interface did not return — shot may have failed.");
            return false;
        }

        if (script.getWidgetManager().getDialogue().getDialogueType() != null) {
            readyToShoot = false;
            return false;
        }

        // Step 4: Get score and result
        String resultText = targetInterface.getResultText();
        int shotScore = 0;

        switch (resultText.toLowerCase()) {
            case "bulls-eye!":
                shotScore = 100;
                bullShots++;
                break;
            case "hit yellow!":
                shotScore = 50;
                yellowShots++;
                break;
            case "hit red!":
                shotScore = 30;
                redShots++;
                break;
            case "hit blue!":
                shotScore = 20;
                blueShots++;
                break;
            case "hit black!":
                shotScore = 10;
                blackShots++;
                break;
            case "missed!":
                shotScore = 0;
                missedShots++;
                break;
            default:
                script.log(getClass().getSimpleName(), "⚠ Unrecognized result text: " + resultText);
                break;
        }

        currentScore += shotScore;
        shotsLeft--;

        script.log(getClass().getSimpleName(), "✅ Shot complete — Result: \"" + resultText + "\" → +" + shotScore + " points (Round total: " + currentScore + ")");

        if (shotsLeft == 0) {
            totalScore += currentScore;
            script.log(getClass().getSimpleName(), "📊 Round complete — Final Score: " + currentScore + " → Added to total.");
            currentScore = 0;
            totalRounds++;
        } else {
            if (script.random(0, 99) < 30) {
                script.submitHumanTask(() -> false, script.random(1, 150));
            } else {
                script.submitTask(() -> false, script.random(50, 350));
            }
        }

        return true;
    }
}