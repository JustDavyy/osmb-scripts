package data;

import com.osmb.api.location.position.types.WorldPosition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FishingMethod {
    private final String name;
    private final List<Integer> catchableFish;
    private final List<HandlingMode> handlingModes;
    private final List<Integer> requiredTools;
    private final List<Integer> cookedFish;
    private final List<Integer> burntFish;
    private final String menuEntry;
    private final Set<WorldPosition> fishingSpots;
    private final String cookingObjectName;
    private final String cookingObjectAction;
    private final String bankObjectName;
    private final String bankObjectAction;

    public FishingMethod(String name,
                         List<Integer> catchableFish,
                         List<HandlingMode> handlingModes,
                         List<Integer> requiredTools,
                         List<Integer> cookedFish,
                         List<Integer> burntFish,
                         String menuEntry,
                         Set<WorldPosition> fishingSpots,
                         String cookingObjectName,
                         String cookingObjectAction,
                         String bankObjectName,
                         String bankObjectAction) {
        this.name = name;
        this.catchableFish = catchableFish;
        this.handlingModes = handlingModes;
        this.requiredTools = requiredTools;
        this.cookedFish = cookedFish;
        this.burntFish = burntFish;
        this.menuEntry = menuEntry;
        this.fishingSpots = fishingSpots;
        this.cookingObjectName = cookingObjectName;
        this.cookingObjectAction = cookingObjectAction;
        this.bankObjectName = bankObjectName;
        this.bankObjectAction = bankObjectAction;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getCatchableFish() {
        return catchableFish;
    }

    public List<HandlingMode> getHandlingModes() {
        return handlingModes;
    }

    public List<Integer> getRequiredTools() {
        return requiredTools;
    }

    public List<Integer> getCookedFish() {
        return cookedFish;
    }

    public List<Integer> getBurntFish() {
        return burntFish;
    }

    public List<Integer> getAllFish() {
        Set<Integer> allFish = new HashSet<>(catchableFish);
        allFish.addAll(cookedFish);
        allFish.addAll(burntFish);
        return new ArrayList<>(allFish);
    }

    public String getMenuEntry() {
        return menuEntry;
    }

    public Set<WorldPosition> getFishingSpots() {
        return fishingSpots;
    }

    public String getCookingObjectName() {
        return cookingObjectName;
    }

    public String getCookingObjectAction() {
        return cookingObjectAction;
    }

    public String getBankObjectName() {
        return bankObjectName;
    }

    public String getBankObjectAction() {
        return bankObjectAction;
    }

    @Override
    public String toString() {
        return name;
    }
}
