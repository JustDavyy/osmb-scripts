package data;

public class SimpleItemIdentifier implements ItemIdentifier {
    private final int itemID;

    public SimpleItemIdentifier(int itemID) {
        this.itemID = itemID;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

    @Override
    public String toString() {
        return String.valueOf(itemID);
    }
}
