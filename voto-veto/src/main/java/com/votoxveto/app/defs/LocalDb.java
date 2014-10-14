package com.votoxveto.app.defs;

public class LocalDb {

    public static final String KEY_READ_PROPOSALS = "read-proposals";

    public static final String USER_STATE = "São Paulo"; /* TODO */

    // Prevents instantiation
    private LocalDb() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
