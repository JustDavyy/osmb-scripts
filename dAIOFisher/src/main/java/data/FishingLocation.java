package data;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.List;
import java.util.Set;

public enum FishingLocation {
    Barb_Village(
            new RectangleArea(3099, 3424, 10, 12, 0), // Main fishing area
            List.of(
                    new RectangleArea(3107, 3432, 2, 2, 0),
                    new RectangleArea(3101, 3424, 2, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(3091, 3488, 7, 11, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.BANK, HandlingMode.DROP, HandlingMode.COOK, HandlingMode.COOKnBANK),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(3104, 3425, 0),
                                    new WorldPosition(3104, 3424, 0),
                                    new WorldPosition(3110, 3432, 0),
                                    new WorldPosition(3110, 3433, 0),
                                    new WorldPosition(3110, 3434, 0)
                            ),
                            "Fire",
                            "Cook",
                            "Bank booth",
                            "Bank"
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.BANK, HandlingMode.DROP, HandlingMode.COOK, HandlingMode.COOKnBANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(3104, 3425, 0),
                                    new WorldPosition(3104, 3424, 0),
                                    new WorldPosition(3110, 3432, 0),
                                    new WorldPosition(3110, 3433, 0),
                                    new WorldPosition(3110, 3434, 0)
                            ),
                            "Fire",
                            "Cook",
                            "Bank booth",
                            "Bank"
                    )
            )
    ),
    Ottos_Grotto(
            new RectangleArea(2495, 3490, 15, 38, 0), // Main fishing area
            List.of(
                    new RectangleArea(2502, 3492, 3, 3, 0),
                    new RectangleArea(2497, 3503, 3, 10, 0),
                    new RectangleArea(2499, 3515, 7, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(2530, 3568, 7, 9, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Barbarian Rod (Feathers)",
                            List.of(ItemID.LEAPING_TROUT, ItemID.LEAPING_SALMON, ItemID.LEAPING_STURGEON),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BARBARIAN_ROD, ItemID.FEATHER),
                            List.of(),
                            List.of(),
                            "Use-rod",
                            Set.of(
                                    // North
                                    new WorldPosition(2504, 3516, 0),

                                    // West
                                    new WorldPosition(2500, 3512, 0),
                                    new WorldPosition(2500, 3510, 0),
                                    new WorldPosition(2500, 3509, 0),
                                    new WorldPosition(2500, 3507, 0),
                                    new WorldPosition(2500, 3506, 0),

                                    // South
                                    new WorldPosition(2504, 3499, 0),
                                    new WorldPosition(2504, 3498, 0),
                                    new WorldPosition(2504, 3497, 0),
                                    new WorldPosition(2506, 3494, 0),
                                    new WorldPosition(2506, 3493, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use"
                    )
            )
    ),
    Mount_Quidamortem_CoX(
            new RectangleArea(1251, 3540, 24, 11, 0), // Main fishing area
            List.of(
                    new RectangleArea(1251, 3541, 5, 4, 0),
                    new RectangleArea(1260, 3541, 8, 4, 0),
                    new RectangleArea(1269, 3543, 5, 7, 0)
            ), // Fishing spot areas
            new RectangleArea(1249, 3565, 11, 10, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Barbarian Rod (Feathers)",
                            List.of(ItemID.LEAPING_TROUT, ItemID.LEAPING_SALMON, ItemID.LEAPING_STURGEON),
                            List.of( HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BARBARIAN_ROD, ItemID.FEATHER),
                            List.of(),
                            List.of(),
                            "Use-rod",
                            Set.of(
                                    // West
                                    new WorldPosition(1253, 3542, 0),

                                    // Middle
                                    new WorldPosition(1266, 3541, 0),
                                    new WorldPosition(1261, 3542, 0),

                                    // East
                                    new WorldPosition(1272, 3546, 0),
                                    new WorldPosition(1272, 3547, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use"
                    )
            )
    );
//    Karamja(
//            new RectangleArea(2495, 3490, 15, 38, 0), // Main fishing area
//            List.of(
//                    new RectangleArea(2502, 3492, 3, 3, 0),
//                    new RectangleArea(2497, 3503, 3, 10, 0),
//                    new RectangleArea(2499, 3515, 7, 3, 0)
//            ), // Fishing spot areas
//            new RectangleArea(2530, 3568, 7, 9, 0), // Bank area
//            List.of(
//                    new FishingMethod(
//                            "Barbarian Rod (Feathers)",
//                            List.of(ItemID.LEAPING_TROUT, ItemID.LEAPING_SALMON, ItemID.LEAPING_STURGEON),
//                            List.of(HandlingMode.DROP, HandlingMode.BANK),
//                            List.of(ItemID.BARBARIAN_ROD, ItemID.FEATHER),
//                            List.of(),
//                            List.of(),
//                            "Use-rod",
//                            Set.of(
//                                    // North
//                                    new WorldPosition(2504, 3516, 0),
//
//                                    // West
//                                    new WorldPosition(2500, 3512, 0),
//                                    new WorldPosition(2500, 3510, 0),
//                                    new WorldPosition(2500, 3509, 0),
//                                    new WorldPosition(2500, 3507, 0),
//                                    new WorldPosition(2500, 3506, 0),
//
//                                    // South
//                                    new WorldPosition(2504, 3499, 0),
//                                    new WorldPosition(2504, 3498, 0),
//                                    new WorldPosition(2504, 3497, 0),
//                                    new WorldPosition(2506, 3494, 0),
//                                    new WorldPosition(2506, 3493, 0)
//                            ),
//                            "None",
//                            "None",
//                            "Bank chest",
//                            "Use"
//                    )
//            )
//    ),

    private final Area fishingArea;
    private final List<Area> fishingSpotAreas;
    private final Area bankArea;
    private final List<FishingMethod> methods;

    FishingLocation(Area fishingArea, List<Area> fishingSpotAreas, Area bankArea, List<FishingMethod> methods) {
        this.fishingArea = fishingArea;
        this.fishingSpotAreas = fishingSpotAreas;
        this.bankArea = bankArea;
        this.methods = methods;
    }

    public Area getFishingArea() {
        return fishingArea;
    }

    public List<Area> getFishingSpotAreas() {
        return fishingSpotAreas;
    }

    public Area getBankArea() {
        return bankArea;
    }

    public List<FishingMethod> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return name().replace('_', ' ');
    }
}