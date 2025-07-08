package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dFossilWCer.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = "Open skills tab";
        script.getWidgetManager().getTabManager().openTab(Tab.Type.SKILLS);

        task = "Get agility level";
        SkillsTabComponent skillsTab = (SkillsTabComponent) script.getWidgetManager()
                .getTabManager().getTabComponent(Tab.Type.SKILLS);

        if (skillsTab != null) {
            var agilitySkill = skillsTab.getSkillLevel(SkillType.AGILITY);
            int agilityLevel = agilitySkill.getLevel();
            int boostedLevel = agilitySkill.getBoostedLevel();
            script.log(getClass(), "Agility level: " + agilityLevel + " (Boosted: " + boostedLevel + ")");

            if (agilityLevel < 70) {
                script.log(getClass(), "Agility level is below 70 (" + agilityLevel + "). Disabling usage of the shortcut.");
                useShortcut = false;
            }
        } else {
            script.log(getClass(), "Failed to get SkillsTabComponent");
            return false;
        }

        task = "Get screen center";
        // Get and store center bounds
        int screenX = script.getScreen().getBounds().width;
        int screenY = script.getScreen().getBounds().height;

        centerX = (int) Math.ceil(screenX / 2.0);
        centerY = (int) Math.ceil(screenY / 2.0);

        script.log(getClass(), "Calculated screenX: " + screenX + ", screenY: " + screenY);
        script.log(getClass(), "Initialized centerX: " + centerX + ", centerY: " + centerY);

        task = "Open inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if using log basket
        task = "Check log basket usage";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET));
        if (inv == null) return false;

        if (inv.containsAny(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET))) {
            script.log(getClass(), "Log basket detected in inventory. Marking usage as true.");
            useLogBasket = true;
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
