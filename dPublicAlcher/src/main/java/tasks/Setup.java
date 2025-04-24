package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.chatbox.ChatboxTab;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResult;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import main.dPublicAlcher;
import utils.Task;
import static main.dPublicAlcher.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        script.log("DEBUG", "We are now inside the Setup task logic");

        if (alchItemID == -1 || alchItemID == ItemID.BANK_FILLER) {
            script.log("ERROR", "Item ID to alch is invalid, invalid search detected. STOPPING SCRIPT!");
            script.stop();
        }

        UIResult<ItemSearchResult> alchitem = script.getItemManager().findItem(script.getWidgetManager().getInventory(), alchItemID);
        if (alchitem.isNotFound()) {
            script.log("FAIL", "Could not locate " + itemName + " to alch.");
            script.stop();
        } else {
            int stack = alchitem.get().getStackAmount();
            script.log("INFO", "Item " + itemName + " found, stack: " + stack);
            stackSize = stack;
            hasReqs = true;
            itemRect = alchitem.get().getTappableBounds();
        }

        script.log(dPublicAlcher.class, "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);


        script.log(dPublicAlcher.class, "Closing chatbox (if open)");
        closeChatBox();

        setupDone = true;
        return false;
    }

    public void closeChatBox() {
        ChatboxTab chatboxTab = (ChatboxTab) script.getWidgetManager().getComponent(ChatboxTab.class);
        ChatboxComponent chatboxComponent = (ChatboxComponent) script.getWidgetManager().getComponent(ChatboxComponent.class);

        // Only proceed if the chatbox is open and no dialogue is currently active
        if (chatboxComponent.isOpen() && script.getWidgetManager().getDialogue().getDialogueType() == null) {
            Rectangle chatBoxTabBounds = chatboxTab.getBounds();
            if (chatBoxTabBounds == null) {
                script.log(dPublicAlcher.class, "Chatbox bounds are null, cannot close Chatbox.");
                return;
            }

            script.getFinger().tap(chatBoxTabBounds);
            script.submitTask(() -> !chatboxComponent.isOpen(), 4000);
        }
    }
}
