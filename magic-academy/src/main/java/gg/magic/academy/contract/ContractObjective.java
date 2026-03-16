package gg.magic.academy.contract;

/**
 * A single trackable objective within a contract.
 *
 * @param type     KILL_MOB | COMPLETE_DUNGEON | CAST_SPELL
 * @param target   Entity-type name, dungeon id, or spell id depending on type
 * @param required Number of times to complete this objective
 * @param label    Display label shown in the GUI
 */
public record ContractObjective(Type type, String target, int required, String label) {

    public enum Type {
        KILL_MOB,
        COMPLETE_DUNGEON,
        CAST_SPELL
    }
}
