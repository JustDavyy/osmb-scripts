package tasks;

// GENERAL JAVA IMPORTS
import main.dTeleporter;
import utils.Task;
import static main.dTeleporter.*;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;


public class TeleportTask extends Task {
    private long lastTeleportTime = 0;
    private long startTime = 0;
    private int teleportCount = 0;

    public TeleportTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastTeleportTime >= getCooldownForSpell());
    }

    public boolean execute() {

        boolean success = false;
        try {
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    null
            );
        } catch (SpellNotFoundException e) {
            script.log(dTeleporter.class, "Spell sprite not found for " + spellToCast.getName() + ". Stopping script...");
            script.stop();
            return false;
        }

        if (success) {
            long now = System.currentTimeMillis();
            long timeSinceLastTeleport = now - lastTeleportTime;
            script.log("DEBUG", "Time since last teleport: " + timeSinceLastTeleport + "ms");

            lastTeleportTime = System.currentTimeMillis();
            teleportCount++;

            // Print stats to log
            printStats();

            // Schedule next task attempt after cooldown
            script.submitTask(() -> (System.currentTimeMillis() - lastTeleportTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName());
        }

        return false;
    }

    private void printStats() {
        // Teleports per hour
        long elapsed = System.currentTimeMillis() - startTime;
        int teleportsPerHour = (int) ((teleportCount * 3600000L) / elapsed);

        // XP tracking
        double totalXp = teleportCount * getXpPerCast();
        int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

        script.log("STATS", String.format(
                "Teleports done: %d | Teleports/hr: %,d | XP gained: %,.1f | XP/hr: %,d",
                teleportCount, teleportsPerHour, totalXp, xpPerHour
        ));
    }

    private static double getXpPerCast() {
        return switch (spellToCast) {
            case VARROCK_TELEPORT -> 35;
            case LUMBRIDGE_TELEPORT -> 41;
            case FALADOR_TELEPORT -> 48;
            case CAMELOT_TELEPORT -> 55.5;
            case KOUREND_TELEPORT -> 58;
            case ARDOUGNE_TELEPORT -> 61;
            case CIVITAS_ILLA_FORTIS_TELEPORT -> 64;
            case WATCHTOWER_TELEPORT, TROLLHEIM_TELEPORT -> 68;
            default -> 0;
        };
    }

    private static long getCooldownForSpell() {
        return 1800;
    }
}