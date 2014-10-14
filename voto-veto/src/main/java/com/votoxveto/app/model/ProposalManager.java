package com.votoxveto.app.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.votoxveto.app.application.CustomApplication;
import com.votoxveto.app.defs.LocalDb;
import com.votoxveto.app.defs.ParseDb;
import com.votoxveto.app.ext.iterator.CachedAsyncIterator;
import com.votoxveto.app.helpers.AsyncHelper;
import com.votoxveto.app.helpers.CollectionsHelper;
import com.votoxveto.app.helpers.CustomHelper;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ProposalManager {

    private static final Random RANDOM = new Random(System.nanoTime());
    private static final int LOWER_BOUND = 5;
    private static final int UPPER_BOUND = 10;
    private static final String CANDIDATE_VOTES_KEY_PREFIX = "VOTES-";
    private static final int MAX_QUERY_LIMIT = 1000;

    private Context mContext;
    private SharedPreferences mPrefs;
    private ListenableFuture<ProposalManager> mLoadFuture;
    private Queue<Duel> mDuels;
    private Pair<Candidate, Candidate> mCandidates;
    private List<Proposal> mProposals;

    private static class InstanceHolder {
        private static final ProposalManager INSTANCE = new ProposalManager();
    }

    public static ProposalManager getInstance(Context context) {
        return InstanceHolder.INSTANCE.setContext(context);
    }

    private ProposalManager() {
        /* Prevent outside instantiation */
    }

    private ProposalManager setContext(Context context) {
        if (mContext != null) return this;
        mContext = context;
        init();
        return this;
    }

    private void init() {
        mPrefs = mContext.getSharedPreferences(CustomApplication.PREFERENCES_FILE, Context.MODE_PRIVATE);
        loadLocalDbInBackground();
    }

    public Pair<Candidate, Candidate> getCandidates() {
        Candidate winner = mCandidates.first;
        Candidate loser = mCandidates.second;
        int w = getCandidateScore(winner.getId());
        int l = getCandidateScore(loser.getId());
        if (w < l) {
            Candidate tmp = winner;
            winner = loser;
            loser = tmp;
        }
        return new Pair<>(winner, loser);
    }

    public ParseQuery<ParseObject> getDuelsQuery() {
        final ParseQuery<ParseObject> duels = ParseQuery.getQuery(ParseDb.Proposals.NAME);
        //duels.whereNotContainedIn(ParseDb.Proposals.FIELD_OBJECT_ID, getLocalIds());
        duels.setLimit(MAX_QUERY_LIMIT);
        duels.orderByDescending(ParseDb.Proposals.FIELD_PRIORITY);
        duels.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
        duels.setMaxCacheAge(TimeUnit.HOURS.toMillis(12));
        return duels;
    }

    private void loadLocalDbInBackground() {
        final ParseQuery<ParseObject> duels = getDuelsQuery();
        CustomHelper.log("Getting from " + (duels.hasCachedResult() ? "LOCAL" : "NETWORK"));
        mLoadFuture = AsyncHelper.executeTask(new Callable<ProposalManager>() {
            @Override
            public ProposalManager call() throws Exception {
                // Set candidates
                ParseQuery<ParseObject> candidates;
                candidates = ParseQuery.getQuery(ParseDb.Proposals.NAME);
                candidates.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
                candidates.setLimit(1);
                Candidate first = TRANSFORMER.apply(candidates.getFirst()).getCandidate();
                candidates = ParseQuery.getQuery(ParseDb.Proposals.NAME);
                candidates.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
                candidates.whereNotEqualTo(ParseDb.Proposals.FIELD_CANDIDATE_ID, first.getId());
                candidates.setLimit(1);
                Candidate second = TRANSFORMER.apply(candidates.getFirst()).getCandidate();
                mCandidates = new Pair<>(first, second);

                // Duels
                List<ParseObject> db = duels.find();
                Collections.shuffle(db, RANDOM);
                mProposals = Lists.newArrayList(Lists.transform(db, TRANSFORMER));
                mDuels = makeDuels(mProposals);

                return ProposalManager.this;
            }
        });
    }

    private LinkedList<Duel> makeDuels(List<Proposal> proposals) {
        Table<String, Candidate, Set<Proposal>> map = HashBasedTable.create();
        Set<Candidate> candidates = new HashSet<>();
        for (Proposal proposal : proposals) {
            candidates.add(proposal.getCandidate());
            if (candidates.size() >= 2) break;
        }
        LinkedList<Duel> duels = new LinkedList<>();
        if (candidates.size() < 2) {
            return duels;
        }
        for (Proposal proposal : proposals) {
            Candidate candidate = proposal.getCandidate();
            String theme = proposal.getTheme();
            Candidate oppositeCandidate = CollectionsHelper.getOther(candidate, candidates);
            Set<Proposal> other = CollectionsHelper.getOrDefault(map, theme, oppositeCandidate, new HashSet<Proposal>());
            if (other.isEmpty()) {
                Set<Proposal> set = CollectionsHelper.getOrDefault(map, theme, candidate, new HashSet<Proposal>());
                set.add(proposal);
            } else {
                Proposal opposite = other.iterator().next();
                other.remove(opposite);
                duels.add(new Duel(proposal, opposite));
            }
        }
        return duels;
    }

    /* TODO: Extract to CandidateManager */
    public int getCandidateScore(String candidateId) {
        String key = CANDIDATE_VOTES_KEY_PREFIX + candidateId;
        return mPrefs.getInt(key, 0);
    }

    private final Function<? super String, Void> mOnDuelVoteListener = new Function<String, Void>() {
        @Override
        public Void apply(String candidateId) {
            String key = CANDIDATE_VOTES_KEY_PREFIX + candidateId;
            int votes = mPrefs.getInt(key, 0) + 1;
            mPrefs.edit().putInt(key, votes).commit(); /* TODO: not on this thread */
            return null;
        }
    };

    public CachedAsyncIterator<Duel> getDuelProvider() {
        return new ProposalIterator(LOWER_BOUND, UPPER_BOUND);
    }

    private Function<? super Proposal, Void> mOnVoteListener = new Function<Proposal, Void>() {
        @Override
        public Void apply(final Proposal proposal) {
            //Set<String> ids = getLocalIds();
            //ids.add(proposal.getObjectId());
            //setLocalIds(ids);
            final ParseObject parseObject = proposal.getParseObject();
            if (parseObject != null) {
                parseObject.increment(proposal.hasWon() ? ParseDb.Proposals.FIELD_I_VOTE : ParseDb.Proposals.FIELD_I_REJECT);
                parseObject.saveEventually();
            }
            return null;
        }
    };

    private Set<String> getLocalIds() {
        String[] idString = mPrefs.getString(LocalDb.KEY_READ_PROPOSALS, "").split(",");
        Set<String> ids = new HashSet<>(Arrays.asList(idString));
        ids.remove("");
        return ids;
    }

    private void setLocalIds(Set<String> ids) {
        String string = Joiner.on(",").join(ids);
        mPrefs.edit().putString(LocalDb.KEY_READ_PROPOSALS, string).apply();
    }

    private final Function<ParseObject, Proposal> TRANSFORMER = new Function<ParseObject, Proposal>() {
        @Override
        public Proposal apply(ParseObject parseObject) {
            Proposal proposal = new Proposal(parseObject);
            proposal.addOnVoteListener(mOnVoteListener);
            return proposal;
        }
    };

    public class ProposalIterator extends CachedAsyncIterator<Duel> {

        protected ProposalIterator(int lowerBound, int upperBound) {
            super(lowerBound, upperBound);
        }

        @Override
        protected void produce(int items, Queue<Duel> queue, Yielder<Duel> yielder) {
            boolean loaded = false;
            while (!loaded) {
                try {
                    CustomHelper.log("loading..");
                    mLoadFuture.get();
                    CustomHelper.log("/loaded");
                    loaded = true;
                } catch (InterruptedException | ExecutionException e) {
                    CustomHelper.log("Error while waiting for local db");
                    CustomHelper.logException(e);
                    CustomHelper.log("/Error");
                    loadLocalDbInBackground();
                }
            }
            for (int i = 0; i < items; i++) {
                if (mDuels.isEmpty()) {
                    Collections.shuffle(mProposals, RANDOM);
                    mDuels = makeDuels(mProposals);
                }
                Duel duel = mDuels.poll();
                duel.addOnVoteListener(mOnDuelVoteListener);
                yielder.yield(duel);
            }
        }
    }
}

