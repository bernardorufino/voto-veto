package com.votoxveto.app.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class Duel implements Parcelable {

    private Proposal mFirstProposal;
    private Proposal mSecondProposal;
    private List<Function<? super String, Void>> mOnVoteListeners = new ArrayList<>();
    private Proposal mWinner;

    public Duel(Proposal firstProposal, Proposal secondProposal) {
        mFirstProposal = firstProposal;
        mSecondProposal = secondProposal;
    }

    private Duel(Parcel in) {
        mFirstProposal = in.readParcelable(Proposal.class.getClassLoader());
        mSecondProposal = in.readParcelable(Proposal.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mFirstProposal, flags);
        dest.writeParcelable(mSecondProposal, flags);
    }

    public Proposal getFirstProposal() {
        return mFirstProposal;
    }

    public Proposal getSecondProposal() {
        return mSecondProposal;
    }

    public Candidate getFirstCandidate() {
        return mFirstProposal.getCandidate();
    }

    public Candidate getSecondCandidate() {
        return mSecondProposal.getCandidate();
    }

    public void addOnVoteListener(Function<? super String, Void> listener) {
        mOnVoteListeners.add(listener);
    }

    public void vote(Proposal proposal) {
        checkArgument(ImmutableList.of(mFirstProposal, mSecondProposal).contains(proposal), "Proposal is not included in this duel");

        mWinner = proposal;
        mWinner.vote(true);
        getLoser().vote(false);
        for (Function<? super String, Void> listener : mOnVoteListeners) {
            listener.apply(proposal.getCandidateId());
        }
    }

    public Proposal getWinner() {
        checkState(mWinner != null, "There must have been a vote before fetching the winner.");

        return mWinner;
    }

    public Proposal getLoser() {
        checkState(mWinner != null, "There must have been a vote before fetching the loser.");

        return (mWinner == mFirstProposal) ? mSecondProposal : mFirstProposal;
    }

    @Override
    public String toString() {
        return mFirstProposal.getObjectId().substring(0, 5) + "x" + mSecondProposal.getObjectId().substring(0, 5);
    }

    public static final Creator<Duel> CREATOR = new Creator<Duel>() {

        @Override
        public Duel createFromParcel(Parcel in) {
            return new Duel(in);
        }

        @Override
        public Duel[] newArray(int size) {
            return new Duel[size];
        }
    };
}
