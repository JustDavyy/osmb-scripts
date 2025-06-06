package data;

import com.osmb.api.item.ItemID;

public enum CookingItem {
    SHRIMPS(ItemID.RAW_SHRIMPS, ItemID.SHRIMPS),
    SEAWEED(ItemID.SEAWEED, ItemID.SEAWEED),
    GIANT_SEAWEED(ItemID.GIANT_SEAWEED, ItemID.GIANT_SEAWEED),
    BREAD(ItemID.BREAD_DOUGH, ItemID.BREAD),
    CHICKEN(ItemID.RAW_CHICKEN, ItemID.COOKED_CHICKEN),
    ANCHOVIES(ItemID.RAW_ANCHOVIES, ItemID.ANCHOVIES),
    SARDINE(ItemID.RAW_SARDINE, ItemID.SARDINE),
    HERRING(ItemID.RAW_HERRING, ItemID.HERRING),
    MACKEREL(ItemID.RAW_MACKEREL, ItemID.MACKEREL),
    REDBERRY_PIE(ItemID.UNCOOKED_BERRY_PIE, ItemID.REDBERRY_PIE),
    TROUT(ItemID.RAW_TROUT, ItemID.TROUT),
    COD(ItemID.RAW_COD, ItemID.COD),
    PIKE(ItemID.RAW_PIKE, ItemID.PIKE),
    MEAT_PIE(ItemID.UNCOOKED_MEAT_PIE, ItemID.MEAT_PIE),
    SALMON(ItemID.RAW_SALMON, ItemID.SALMON),
    STEW(ItemID.UNCOOKED_STEW, ItemID.STEW),
    TUNA(ItemID.RAW_TUNA, ItemID.TUNA),
    APPLE_PIE(ItemID.UNCOOKED_APPLE_PIE, ItemID.APPLE_PIE),
    KARAMBWAN(ItemID.RAW_KARAMBWAN, ItemID.COOKED_KARAMBWAN),
    GARDEN_PIE(ItemID.RAW_GARDEN_PIE, ItemID.GARDEN_PIE),
    LOBSTER(ItemID.RAW_LOBSTER, ItemID.LOBSTER),
    BASS(ItemID.RAW_BASS, ItemID.BASS),
    SWORDFISH(ItemID.RAW_SWORDFISH, ItemID.SWORDFISH),
    FISH_PIE(ItemID.RAW_FISH_PIE, ItemID.FISH_PIE),
    BOTANICAL_PIE(ItemID.UNCOOKED_BOTANICAL_PIE, ItemID.BOTANICAL_PIE),
    MUSHROOM_PIE(ItemID.UNCOOKED_MUSHROOM_PIE, ItemID.MUSHROOM_PIE),
    CURRY(ItemID.UNCOOKED_CURRY, ItemID.CURRY),
    MONKFISH(ItemID.RAW_MONKFISH, ItemID.MONKFISH),
    ADMIRAL_PIE(ItemID.RAW_ADMIRAL_PIE, ItemID.ADMIRAL_PIE),
    DRAGONFRUIT_PIE(ItemID.UNCOOKED_DRAGONFRUIT_PIE, ItemID.DRAGONFRUIT_PIE),
    SHARK(ItemID.RAW_SHARK, ItemID.SHARK),
    SEA_TURTLE(ItemID.RAW_SEA_TURTLE, ItemID.SEA_TURTLE),
    ANGLERFISH(ItemID.RAW_ANGLERFISH, ItemID.ANGLERFISH),
    WILD_PIE(ItemID.RAW_WILD_PIE, ItemID.WILD_PIE),
    DARK_CRAB(ItemID.RAW_DARK_CRAB, ItemID.DARK_CRAB),
    MANTA_RAY(ItemID.RAW_MANTA_RAY, ItemID.MANTA_RAY),
    SUMMER_PIE(ItemID.RAW_SUMMER_PIE, ItemID.SUMMER_PIE);

    private final int rawItemId;
    private final int cookedItemId;

    CookingItem(int rawItemId, int cookedItemId) {
        this.rawItemId = rawItemId;
        this.cookedItemId = cookedItemId;
    }

    public int getRawItemId() {
        return rawItemId;
    }

    public int getCookedItemId() {
        return cookedItemId;
    }

    public static CookingItem fromRawItemId(int rawItemId) {
        for (CookingItem item : values()) {
            if (item.rawItemId == rawItemId) {
                return item;
            }
        }
        return null;
    }
}
