package data;

public enum HandlingMode {
    DROP,
    BANK,
    COOK,
    COOKnBANK;

    @Override
    public String toString() {
        return switch (this) {
            case COOKnBANK -> "Cook & Bank";
            default -> name().charAt(0) + name().substring(1).toLowerCase();
        };
    }
}