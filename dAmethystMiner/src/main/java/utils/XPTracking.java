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
import main.dAmethystMiner;

import java.util.EnumMap;
import java.util.Map;

public class XPTracking {

    public enum SkillType { MINING, CRAFTING }

    private static final int SPRITE_MINING_ID = 209;
    private static final int SPRITE_CRAFTING_ID = 207;

    private final ScriptCore core;
    private final XPDropsComponent xpDropsComponent;

    private final SearchableImage miningSprite;
    private final SearchableImage craftingSprite;

    // One live tracker per skill
    private final Map<SkillType, XPTracker> trackers = new EnumMap<>(SkillType.class);

    public XPTracking(ScriptCore core) {
        this.core = core;
        this.xpDropsComponent = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);

        SingleThresholdComparator comp = new SingleThresholdComparator(15);
        SearchableImage fishFull  = new SearchableImage(SPRITE_MINING_ID, core, comp, ColorModel.RGB);
        SearchableImage cookFull  = new SearchableImage(SPRITE_CRAFTING_ID, core, comp, ColorModel.RGB);

        this.miningSprite = fishFull.subImage(fishFull.width / 2, 0, fishFull.width / 2, fishFull.height);
        this.craftingSprite = cookFull.subImage(cookFull.width / 2, 0, cookFull.width / 2, cookFull.height);
    }

    public XPTracker getXpTracker(SkillType skill) {
        return trackers.get(skill);
    }

    public void checkXP(SkillType skill) {
        Integer currentXP = getXpCounterForSkill(skill);
        if (currentXP == null) return;

        XPTracker t = trackers.get(skill);
        if (t == null) {
            t = new XPTracker(core, currentXP);
            trackers.put(skill, t);
        } else {
            double prev = t.getXp();
            double gained = currentXP - prev;
            if (gained > 0) {
                t.incrementXp(gained);
                
                // Increment skill-specific totals
                switch (skill) {
                    case MINING -> dAmethystMiner.miningXpGained += gained;
                    case CRAFTING -> dAmethystMiner.craftingXpGained += gained;
                }
            }
        }
    }

    public boolean checkXPCounterActive() {
        if (xpDropsComponent == null) return false;
        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) return true;

        ComponentSearchResult<Integer> result = xpDropsComponent.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
            core.getFinger().tap(bounds);
            boolean ok = core.pollFramesHuman(() -> {
                ComponentSearchResult<Integer> r = xpDropsComponent.getResult();
                return r != null && r.getComponentImage().getGameFrameStatusType() == 1;
            }, RandomUtils.uniformRandom(1500, 3000));
            bounds = xpDropsComponent.getBounds();
            return ok && bounds != null;
        }
        return true;
    }

    private Integer getXpCounterForSkill(SkillType skill) {
        Rectangle bounds = getXPDropsBounds();
        if (bounds == null) return null;

        SearchableImage sprite = (skill == SkillType.MINING) ? miningSprite : craftingSprite;

        boolean matches = core.getImageAnalyzer().findLocation(bounds, sprite) != null;
        if (!matches) return null;

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

    private Rectangle getXPDropsBounds() {
        XPDropsComponent comp = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        if (comp == null) return null;

        Rectangle b = comp.getBounds();
        if (b == null) return null;

        ComponentSearchResult<Integer> result = comp.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) return null;

        // Same crop you used before
        return new Rectangle(b.x - 140, b.y - 1, 119, 38);
    }
}