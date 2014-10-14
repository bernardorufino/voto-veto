package com.votoxveto.app.defs;

public class ParseDb {

    public static class Proposals {
        public static final String NAME = "SegundoTurno";
        public static final String FIELD_OBJECT_ID = "objectId";
        public static final String FIELD_TEXT = "newsTitle";
        public static final String FIELD_CANDIDATE_ID = "politicianNumber";
        public static final String FIELD_CANDIDATE_NAME = "politicianName";
        public static final String FIELD_CANDIDATE_IMAGE_URL = "politicianImage";
        public static final String FIELD_THEME = "categoria";
        public static final String FIELD_PRIORITY = "priority";
        public static final String FIELD_I_VOTE = "euVoto";
        public static final String FIELD_I_REJECT = "euVeto";
        public static final String PRESIDENT_STATE = "Outros";
    }

    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_CREATED_AT = "createdAt";

    // Prevents instantiation
    private ParseDb() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
