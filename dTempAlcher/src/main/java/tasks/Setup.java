package tasks;

import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

import utils.Task;

import static main.dTempAlcher.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return !setupDone;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        script.log("DEBUG", "We are now inside the Setup task logic");

        task = "Check mode";
        if (!multipleSlotsMode) {
            if (alchSlotID < 0 || alchSlotID > 27) {
                script.log("ERROR", "Single slot index is out of bounds (0–27). STOPPING SCRIPT!");
                script.stop();
            }
        } else {
            if (slotsToAlch == null || slotsToAlch.isEmpty()) {
                script.log("ERROR", "No slots provided in multiple slot mode. STOPPING SCRIPT!");
                script.stop();
            }
        }

        task = "Open inventory";
        script.log(getClass().getSimpleName(), "Opening inventory tab...");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        setupDone = true;
        return false;
    }
}
