package nl.chimpgamer.mapartlock.lock;

/** Result of a lock or unlock attempt. Each value names a key in messages.yml. */
public enum LockOutcome {
    LOCKED("map_locked", true),
    UNLOCKED("map_unlocked", true),
    ALREADY_LOCKED("already_locked", false),
    ALREADY_UNLOCKED("already_unlocked", false),
    NOT_OWNER("not_owner", false),
    NOT_A_MAP("must_hold_map", false),
    NO_MAP_DATA("map_has_no_data", false);

    private final String messageKey;
    private final boolean changedState;

    LockOutcome(String messageKey, boolean changedState) {
        this.messageKey = messageKey;
        this.changedState = changedState;
    }

    public String messageKey() {
        return messageKey;
    }

    /** Only a real change is worth writing to the server log. */
    public boolean changedState() {
        return changedState;
    }
}
