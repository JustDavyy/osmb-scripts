package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.chatbox.ChatboxTab;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import main.dBattlestaffCrafter;
import utils.Task;
import static main.dBattlestaffCrafter.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        script.log("DEBUG", "We are now inside the Setup task logic");

        script.log(dBattlestaffCrafter.class, "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        script.log(dBattlestaffCrafter.class, "Closing chatbox (if open)");
        closeChatBox();

        setupDone = true;
        hasReqs = true;
        shouldBank = true;
        return false;
    }

    public void closeChatBox() {
        // tab which resembles the little button
        ChatboxTab chatboxTab = (ChatboxTab) script.getWidgetManager().getComponent(ChatboxTab.class);
        // resembles the rectangle chatbox what opens/closes when clicking the chatbox tab
        ChatboxComponent chatboxComponent = (ChatboxComponent) script.getWidgetManager().getComponent(ChatboxComponent.class);
        if(chatboxComponent.isOpen()) {
            Rectangle chatBoxTabBounds = chatboxTab.getBounds();
            if(chatBoxTabBounds == null) {
                script.log(dBattlestaffCrafter.class, "Chatbox bounds are null, cannot close Chatbox.");
                return;
            }
            script.getFinger().tap(chatBoxTabBounds);

            script.submitTask(() -> !chatboxComponent.isOpen(), 4000);
        }
    }
}
