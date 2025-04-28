package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.chatbox.ChatboxTab;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResult;
import com.osmb.api.script.Script;

import main.dTempAlcher;
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
        script.log("DEBUG", "We are now inside the Setup task logic");

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

        script.log(dTempAlcher.class, "Opening inventory tab...");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        script.log(dTempAlcher.class, "Closing chatbox if open...");
        closeChatBox();

        setupDone = true;
        return false;
    }

    private void closeChatBox() {
        ChatboxTab chatboxTab = (ChatboxTab) script.getWidgetManager().getComponent(ChatboxTab.class);
        ChatboxComponent chatboxComponent = (ChatboxComponent) script.getWidgetManager().getComponent(ChatboxComponent.class);

        if (chatboxComponent.isOpen() && script.getWidgetManager().getDialogue().getDialogueType() == null) {
            Rectangle chatBoxTabBounds = chatboxTab.getBounds();
            if (chatBoxTabBounds == null) {
                script.log(dTempAlcher.class, "Chatbox bounds are null, cannot close chatbox.");
                return;
            }
            script.getFinger().tap(chatBoxTabBounds);
            script.submitTask(() -> !chatboxComponent.isOpen(), 4000);
        }
    }
}
