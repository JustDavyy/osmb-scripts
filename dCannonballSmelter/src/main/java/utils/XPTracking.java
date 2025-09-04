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

import java.awt.*;

public class XPTracking {

    private static final int SPRITE_HUNTER_ID = 210;

    private final ScriptCore core;
    private final SearchableImage smithingSprite;
    private final XPDropsComponent xpDropsComponent;
    private XPTracker xpTracker;

    public XPTracking(ScriptCore core) {
        this.core = core;
        this.xpDropsComponent = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        SearchableImage smithingFull = new SearchableImage(SPRITE_HUNTER_ID, core, new SingleThresholdComparator(15), ColorModel.RGB);
        this.smithingSprite = smithingFull.subImage(smithingFull.width / 2, 0, smithingFull.width / 2, smithingFull.height);
    }

    public XPTracker getXpTracker() {
        return xpTracker;
    }

    public void checkXP() {
        Integer currentXP = getXpCounter();
        if (currentXP != null) {
            if (xpTracker == null) {
                xpTracker = new XPTracker(core, currentXP);
            } else {
                double prev = xpTracker.getXp();
                double gainedXP = currentXP - prev;
                if (gainedXP > 0) {
                    xpTracker.incrementXp(gainedXP);
                }
            }
        }
    }

    private Integer getXpCounter() {
        Rectangle bounds = getXPDropsBounds();
        if (bounds == null) {
            return null;
        }
        boolean matchesSmithing = core.getImageAnalyzer().findLocation(bounds, smithingSprite) != null;
        if (!matchesSmithing) {
            return null;
        }
        core.getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB(), 1);
        String xpText = core.getOCR()
                .getText(com.osmb.api.visual.ocr.fonts.Font.SMALL_FONT, bounds, -1)
                .replaceAll("[^0-9]", "");
        if (xpText.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(xpText);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean checkXPCounterActive() {
        if (xpDropsComponent == null) {
            return false;
        }
        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) {
            return true;
        }
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
        if (b == null) {
            return null;
        }
        ComponentSearchResult<Integer> result = comp.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
            return null;
        }
        return new Rectangle(b.x - 140, b.y - 1, 119, 38);
    }
}
