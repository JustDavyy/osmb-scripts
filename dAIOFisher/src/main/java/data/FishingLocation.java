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
                            "Bank",
                            FishingMethod.BankObjectType.BANK,
                            20000
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
                            "Bank",
                            FishingMethod.BankObjectType.BANK,
                            30000
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
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            20000
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
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            20000
                    )
            )
    ),
    Karamja_West(
            new RectangleArea(2799, 3007, 14, 16, 0), // Main fishing area
            List.of(
                    new RectangleArea(2806, 3015, 7, 8, 0),
                    new RectangleArea(2800, 3008, 4, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_KARAMBWANJI, ItemID.RAW_SHRIMPS),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(ItemID.RAW_SHRIMPS),
                            List.of(),
                            "Net",
                            Set.of(
                                    // East
                                    new WorldPosition(2807, 3021, 0),
                                    new WorldPosition(2810, 3017, 0),

                                    // South
                                    new WorldPosition(2801, 3010, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Small Fishing Net (SafeMode/10HP)",
                            List.of(ItemID.RAW_KARAMBWANJI, ItemID.RAW_SHRIMPS),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(ItemID.RAW_SHRIMPS),
                            List.of(),
                            "Net",
                            Set.of(
                                    // East
                                    new WorldPosition(2807, 3021, 0),
                                    new WorldPosition(2810, 3017, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    )
            )
    ),
    Shilo_Village(
            new RectangleArea(2833, 2970, 32, 2, 0), // Main fishing area
            List.of(
                    new RectangleArea(2859, 2969, 6, 3, 0),
                    new RectangleArea(2834, 2968, 11, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(2850, 2952, 4, 4, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.BANK, HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    // South east
                                    new WorldPosition(2860, 2972, 0),
                                    new WorldPosition(2865, 2972, 0),

                                    // South west
                                    new WorldPosition(2836, 2971, 0),
                                    new WorldPosition(2841, 2971, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            20000
                    )
            )
    ),
    Zul_Andra(
            new RectangleArea(2179, 3060, 26, 10, 0), // Main fishing area
            List.of(
                    new RectangleArea(2197, 3064, 2, 2, 0),
                    new RectangleArea(2188, 3067, 4, 2, 0),
                    new RectangleArea(2182, 3065, 3, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.SACRED_EEL),
                            List.of(HandlingMode.COOK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT, ItemID.KNIFE),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // West
                                    new WorldPosition(2181, 3067, 0),

                                    // Middle
                                    new WorldPosition(2186, 3070, 0),
                                    new WorldPosition(2192, 3070, 0),

                                    // East
                                    new WorldPosition(2200, 3066, 0),
                                    new WorldPosition(2199, 3066, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            50000
                    )
            )
    ),
    Port_Piscarilius_East(
            new RectangleArea(1823, 3769, 16, 8, 0), // Main fishing area
            List.of(
                    new RectangleArea(1824, 3771, 3, 2, 0),
                    new RectangleArea(1831, 3770, 2, 2, 0),
                    new RectangleArea(1837, 3776, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(1803, 3785, 7, 5, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Sandworms)",
                            List.of(ItemID.RAW_ANGLERFISH),
                            List.of(HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.SANDWORMS),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // South
                                    new WorldPosition(1827, 3770, 0),
                                    new WorldPosition(1826, 3770, 0),
                                    new WorldPosition(1831, 3767, 0),
                                    new WorldPosition(1836, 3771, 0),
                                    new WorldPosition(1833, 3769, 0),

                                    // East
                                    new WorldPosition(1840, 3776, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            50000
                    )
            )
    ),
    Port_Piscarilius_West(
            new RectangleArea(1744, 3793, 23, 7, 0), // Main fishing area
            List.of(
                    new RectangleArea(1760, 3794, 5, 1, 0),
                    new RectangleArea(1746, 3799, 5, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(1803, 3785, 7, 5, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.BIG_SWORDFISH),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            50000
                    )
            )
    ),
    Kingstown(
            new RectangleArea(1719, 3684, 2, 1, 0), // Main fishing area
            List.of(
                    new RectangleArea(1720, 3684, 1, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(1720, 3683, 0),
                                    new WorldPosition(1722, 3685, 0),
                                    new WorldPosition(1721, 3683, 0),
                                    new WorldPosition(1721, 3686, 0),
                                    new WorldPosition(1720, 3686, 0),
                                    new WorldPosition(1722, 3684, 0)

                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(1720, 3683, 0),
                                    new WorldPosition(1722, 3685, 0),
                                    new WorldPosition(1721, 3683, 0),
                                    new WorldPosition(1721, 3686, 0),
                                    new WorldPosition(1720, 3686, 0),
                                    new WorldPosition(1722, 3684, 0)

                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            35000
                    )
            )
    );
//    Mor_Ul_Rek(
//            new RectangleArea(2179, 3060, 26, 10, 0), // Main fishing area
//            List.of(
//                    new RectangleArea(2197, 3064, 2, 2, 0),
//                    new RectangleArea(2188, 3067, 4, 2, 0),
//                    new RectangleArea(2182, 3065, 3, 1, 0)
//            ), // Fishing spot areas
//            new RectangleArea(0, 0, 0, 0, 0), // Bank area
//            List.of(
//                    new FishingMethod(
//                            "Oily Fishing Rod (Bait)",
//                            List.of(ItemID.SACRED_EEL),
//                            List.of(HandlingMode.COOK),
//                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT, ItemID.KNIFE),
//                            List.of(),
//                            List.of(),
//                            "Bait",
//                            Set.of(
//                                    // West
//                                    new WorldPosition(2181, 3067, 0),
//
//                                    // Middle
//                                    new WorldPosition(2186, 3070, 0),
//                                    new WorldPosition(2192, 3070, 0),
//
//                                    // East
//                                    new WorldPosition(2200, 3066, 0),
//                                    new WorldPosition(2199, 3066, 0)
//                            ),
//                            "None",
//                            "None",
//                            "None",
//                            "None",
//                            FishingMethod.BankObjectType.NONE,
//                            40000
//                    )
//            )
//    );

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