package main;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.chatbox.ChatboxTab;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import javafx.scene.Scene;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(name = "dWyrmAgility", author = "JustDavyy", version = 1.0, description = "Does the Wyrm basic or advanced agility course.", skillCategory = SkillCategory.AGILITY)
public class dWyrmAgility extends Script {
    private Course selectedCourse;
    private int nextRunActivate;
    private int noMovementTimeout = RandomUtils.weightedRandom(6000, 9000);

    // to handle the osrs glitch where the position doesn't update
    private int failThreshold = random(4, 6);
    private int failCount = 0;

    public dWyrmAgility(Object object) {
        super(object);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName The name of the obstacle
     * @param menuOption   The name of the menu option to select
     * @param end          The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param timeout      The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, 1, timeout);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, true, timeout);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param canReach         If {@code false} then this method will avoid using {@link RSObject#canReach()} when querying objects for the obstacle.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, boolean canReach, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, canReach, timeout, null);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param canReach         If {@code false} then this method will avoid using {@link RSObject#canReach()} when querying objects for the obstacle.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @param objectBaseTile   The base tile of the object. If null we avoid this check.
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, boolean canReach, int timeout, WorldPosition objectBaseTile) {
        // cache hp, we determine if we failed the obstacle via hp decrementing
        UIResult<Integer> hitpoints = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        Optional<RSObject> result = core.getObjectManager().getObject(gameObject -> {

            if (gameObject.getName() == null || gameObject.getActions() == null) return false;

            if (!gameObject.getName().equalsIgnoreCase(obstacleName)) {
                return false;
            }

            if (objectBaseTile != null) {
                if (!objectBaseTile.equals(gameObject.getWorldPosition())) {
                    return false;
                }
            }
            if (!canReach) {
                return true;
            }

            return gameObject.canReach(interactDistance);
        });
        if (result.isEmpty()) {
            core.log(dWyrmAgility.class.getSimpleName(), "ERROR: Obstacle (" + obstacleName + ") does not exist with criteria.");
            return ObstacleHandleResponse.OBJECT_NOT_IN_SCENE;
        }
        RSObject object = result.get();
        if (object.interact(menuOption)) {
            core.log(dWyrmAgility.class.getSimpleName(), "Interacted successfully, sleeping until conditions are met...");
            Timer noMovementTimer = new Timer();
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>();
            if (core.submitHumanTask(() -> {
                WorldPosition currentPos = core.getWorldPosition();
                if (currentPos == null) {
                    return false;
                }
                // check if we take damage
                if (hitpoints.isFound()) {
                    UIResult<Integer> newHitpointsResult = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
                    if (newHitpointsResult.isFound()) {
                        if (hitpoints.get() > newHitpointsResult.get()) {
                            return true;
                        }
                    }
                }
                // check for being stood still
                if (previousPosition.get() != null) {
                    if (currentPos.equals(previousPosition.get())) {
                        if (noMovementTimer.timeElapsed() > core.noMovementTimeout) {
                            core.noMovementTimeout = RandomUtils.weightedRandom(2000, 6000);
                            core.printFail();
                            core.failCount++;
                            return true;
                        }
                    } else {
                        noMovementTimer.reset();
                    }
                } else {
                    noMovementTimer.reset();
                }
                previousPosition.set(currentPos);

                RSTile tile = core.getSceneManager().getTile(core.getWorldPosition());
                Polygon poly = tile.getTileCube(120);
                if (core.getPixelAnalyzer().isAnimating(0.1, poly)) {
                    return false;
                }
                if (end instanceof Area area) {
                    if (area.contains(currentPos)) {
                        core.failThreshold = Utils.random(2, 3);
                        return true;
                    }
                } else if (end instanceof Position pos) {
                    if (currentPos.equals(pos)) {
                        core.failThreshold = Utils.random(2, 3);
                        return true;
                    }
                }
                return false;
            }, timeout)) {
                return ObstacleHandleResponse.SUCCESS;
            } else {
                core.failCount++;
                core.printFail();
                return ObstacleHandleResponse.TIMEOUT;
            }
        } else {
            core.log(dWyrmAgility.class.getSimpleName(), "ERROR: Failed interacting with obstacle (" + obstacleName + ").");
            core.failCount++;
            return ObstacleHandleResponse.FAILED_INTERACTION;
        }
    }

    private void printFail() {
        log(dWyrmAgility.class, "Failed to handle obstacle. Fail count: " + failCount + "/" + failThreshold);
    }

    @Override
    public void onStart() {

        UI ui = new UI();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dWyrmAgility Settings", false);

        // set the selected course
        this.selectedCourse = ui.selectedCourse();
        this.nextRunActivate = random(30, 70);

        // Close tabs if they are open
        getWidgetManager().getTabManager().closeContainer();

        log(getClass().getSimpleName(), "Closing chatbox (if open)");
        closeChatBox();
    }

    @Override
    public int onRelog() {
        failCount = 0;
        return 0;
    }

    @Override
    public int poll() {
        if (failCount > failThreshold) {
            log(dWyrmAgility.class, "Failed object multiple times. Relogging.");
            getWidgetManager().getLogoutTab().logout();
            return 0;
        }

        UIResult<Boolean> runEnabled = getWidgetManager().getMinimapOrbs().isRunEnabled();
        if (runEnabled.isFound()) {
            UIResult<Integer> runEnergyOpt = getWidgetManager().getMinimapOrbs().getRunEnergy();
            int runEnergy = runEnergyOpt.orElse(-1);
            if (!runEnabled.get() && runEnergy > nextRunActivate) {
                log(getClass().getSimpleName(), "Enabling run");
                if (!getWidgetManager().getMinimapOrbs().setRun(true)) {
                    return 0;
                }
                nextRunActivate = random(30, 70);
            }
        }
        WorldPosition position = getWorldPosition();
        if (position == null) {
            log(getClass().getSimpleName(), "Position is null.");
            return 0;
        }
        return selectedCourse.poll(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        if (selectedCourse == null) {
            return new int[0];
        }
        return selectedCourse.regions();
    }

    public void closeChatBox() {
        // tab which resembles the little button
        ChatboxTab chatboxTab = (ChatboxTab) getWidgetManager().getComponent(ChatboxTab.class);
        // resembles the rectangle chatbox what opens/closes when clicking the chatbox tab
        ChatboxComponent chatboxComponent = (ChatboxComponent) getWidgetManager().getComponent(ChatboxComponent.class);
        if(chatboxComponent.isOpen()) {
            Rectangle chatBoxTabBounds = chatboxTab.getBounds();
            if(chatBoxTabBounds == null) {
                log(getClass().getSimpleName(), "Chatbox bounds are null, cannot close Chatbox.");
                return;
            }
            getFinger().tap(chatBoxTabBounds);

            submitTask(() -> !chatboxComponent.isOpen(), 4000);
        }
    }
}
