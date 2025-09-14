package utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.SearchableImage;
import main.dGemstoneCrabber;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static main.dGemstoneCrabber.lastXpGainAt;

public class XPTracking {

    // Supported skill sprite IDs
    private static final int SPRITE_TOTAL      = 222;
    private static final int SPRITE_ATTACK     = 197;
    private static final int SPRITE_STRENGTH   = 198;
    private static final int SPRITE_DEFENCE    = 199;
    private static final int SPRITE_RANGED     = 200;
    private static final int SPRITE_MAGIC      = 202;
    private static final int SPRITE_HITPOINTS  = 203;

    private final ScriptCore core;
    private final XPDropsComponent xpDropsComponent;

    // Store one subimage per sprite ID
    private final Map<Integer, SearchableImage> spriteImages = new HashMap<>();

    // Track one XPTracker per sprite ID
    private final Map<Integer, XPTracker> xpTrackers = new HashMap<>();

    // Virtual tracker representing *all* skills combined
    private final XPTracker combinedTracker;

    public XPTracking(ScriptCore core) {
        this.core = core;
        this.xpDropsComponent = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);

        int[] spriteIds = {
                SPRITE_TOTAL, SPRITE_ATTACK, SPRITE_STRENGTH,
                SPRITE_DEFENCE, SPRITE_RANGED, SPRITE_MAGIC, SPRITE_HITPOINTS
        };

        for (int id : spriteIds) {
            SearchableImage full = new SearchableImage(id, core, new SingleThresholdComparator(15), ColorModel.RGB);
            spriteImages.put(id, full.subImage(full.width / 2, 0, full.width / 2, full.height));
        }

        // Combined tracker starts at 0
        combinedTracker = new XPTracker(core, 0);
    }

    /** Old API: returns the combined tracker */
    public XPTracker getXpTracker() {
        return combinedTracker;
    }

    /** Optional: get per-skill tracker */
    public XPTracker getXpTracker(int spriteId) {
        return xpTrackers.get(spriteId);
    }

    /** Checks all supported XP counters in one pass */
    public void checkXP() {
        for (Map.Entry<Integer, SearchableImage> entry : spriteImages.entrySet()) {
            int spriteId = entry.getKey();
            SearchableImage sprite = entry.getValue();

            Integer currentXP = getXpCounter(sprite);
            if (currentXP == null) continue;

            XPTracker tracker = xpTrackers.get(spriteId);
            if (tracker == null) {
                tracker = new XPTracker(core, currentXP);
                xpTrackers.put(spriteId, tracker);
            } else {
                double prev = tracker.getXp();
                double gained = currentXP - prev;

                // Safeguard: ignore >10k in one read
                if (gained > 0 && gained <= 10_000) {
                    tracker.incrementXp(gained);
                    combinedTracker.incrementXp(gained); // also add to combined
                    dGemstoneCrabber.lastXpGainAt = System.currentTimeMillis();
                }
            }
        }
    }

    private Integer getXpCounter(SearchableImage sprite) {
        Rectangle bounds = getXPDropsBounds();
        if (bounds == null) return null;

        boolean matches = core.getImageAnalyzer().findLocation(bounds, sprite) != null;
        if (!matches) return null;

        core.getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB(), 1);

        String xpText = core.getOCR()
                .getText(com.osmb.api.visual.ocr.fonts.Font.SMALL_FONT, bounds, -1)
                .replaceAll("[^0-9]", "");
        if (xpText.isEmpty()) return null;

        try {
            return Integer.parseInt(xpText);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean checkXPCounterActive() {
        if (xpDropsComponent == null) return false;

        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) return true;

        ComponentSearchResult<Integer> result = xpDropsComponent.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
            core.getFinger().tap(bounds);
            boolean succeed = core.pollFramesHuman(() -> {
                ComponentSearchResult<Integer> r = xpDropsComponent.getResult();
                return r != null && r.getComponentImage().getGameFrameStatusType() == 1;
            }, RandomUtils.uniformRandom(1500, 3000));
            bounds = xpDropsComponent.getBounds();
            return succeed && bounds != null;
        }
        return true;
    }

    private Rectangle getXPDropsBounds() {
        XPDropsComponent comp = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        if (comp == null) return null;

        Rectangle b = comp.getBounds();
        if (b == null) return null;

        ComponentSearchResult<Integer> result = comp.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
            return null;
        }
        return new Rectangle(b.x - 140, b.y - 1, 119, 38);
    }
}