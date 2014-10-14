package com.votoxveto.app.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.base.Function;
import com.parse.ParseObject;
import com.votoxveto.app.defs.ParseDb;

import java.util.ArrayList;
import java.util.List;

public class Proposal implements Parcelable, Comparable<Proposal> {

    private static final String DEFAULT_THEME = "Presidente";
    public static final String DEFAULT_STATE = "Brasil";

    private ParseObject mParseObject;
    private String mObjectId;
    private String mText;
    private String mTheme;
    private Candidate mCandidate;
    private List<Function<? super Proposal, Void>> mOnVoteListeners = new ArrayList<>();
    private boolean mWon;

    public Proposal(ParseObject parseObject) {
        mParseObject = parseObject;
        mObjectId = parseObject.getObjectId();
        mText = parseObject.getString(ParseDb.Proposals.FIELD_TEXT).trim();
        mTheme = parseObject.getString(ParseDb.Proposals.FIELD_THEME);
        String candidateId = parseObject.getString(ParseDb.Proposals.FIELD_CANDIDATE_ID);
        String candidateName = parseObject.getString(ParseDb.Proposals.FIELD_CANDIDATE_NAME);
        String candidateState = DEFAULT_STATE;
        String candidateImageUrl = parseObject.getString(ParseDb.Proposals.FIELD_CANDIDATE_IMAGE_URL);
        mCandidate = Candidate.getOrCreateById(candidateId);
        mCandidate.setName(candidateName);
        mCandidate.setState(candidateState);
        mCandidate.setImageUrl(candidateImageUrl);
    }

    private Proposal(Parcel in) {
        mObjectId = in.readString();
        mText = in.readString();
        mTheme = in.readString();
        mCandidate = in.readParcelable(Candidate.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mObjectId);
        dest.writeString(mText);
        dest.writeString(mTheme);
        dest.writeParcelable(mCandidate, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getText() {
        return mText;
    }

    public String getObjectId() {
        return mObjectId;
    }

    public String getTheme() {
        return mTheme;
    }

    public Candidate getCandidate() {
        return mCandidate;
    }

    public String getCandidateId() {
        return mCandidate.getId();
    }

    public String getCandidateName() {
        return mCandidate.getName();
    }

    public String getCandidateState() {
        return mCandidate.getState();
    }

    public String getCandidateImageUrl() {
        return mCandidate.getImageUrl();
    }

    public ParseObject getParseObject() {
        return mParseObject;
    }

    public void vote(final boolean won) {
        mWon = won;
        for (Function<? super Proposal, Void> listener : mOnVoteListeners) {
            listener.apply(this);
        }
    }

    public boolean hasWon() {
        return mWon;
    }

    public void addOnVoteListener(Function<? super Proposal, Void> listener) {
        mOnVoteListeners.add(listener);
    }

    public static final Creator<Proposal> CREATOR = new Creator<Proposal>() {

        @Override
        public Proposal createFromParcel(Parcel in) {
            return new Proposal(in);
        }

        @ Override
        public Proposal[] newArray(int size) {
            return new Proposal[size];
        }
    };

    @Override
    public int compareTo(Proposal another) {
        return getCandidate().compareTo(another.getCandidate());
    }
}
