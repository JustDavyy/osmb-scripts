package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import utils.Task;
import static main.dTeleporter.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        script.log("DEBUG", "We are now inside the Setup task logic");

        script.log(getClass().getSimpleName(), "Opening magic tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.SPELLBOOK);

        setupDone = true;
        return false;
    }
}
