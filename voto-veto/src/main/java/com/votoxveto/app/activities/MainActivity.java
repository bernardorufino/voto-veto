package com.votoxveto.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.parse.ParseAnalytics;
import com.votoxveto.app.R;
import com.votoxveto.app.application.CustomApplication;
import com.votoxveto.app.ext.fonts.FontsLoader;
import com.votoxveto.app.ext.future.FutureCallbackAdapter;
import com.votoxveto.app.ext.iterator.CachedAsyncIterator;
import com.votoxveto.app.ext.textresizer.TextResizer;
import com.votoxveto.app.helpers.AsyncHelper;
import com.votoxveto.app.helpers.CustomHelper;
import com.votoxveto.app.helpers.CustomViewHelper;
import com.votoxveto.app.helpers.InternetHelper;
import com.votoxveto.app.model.Candidate;
import com.votoxveto.app.model.Duel;
import com.votoxveto.app.model.Proposal;
import com.votoxveto.app.model.ProposalManager;
import com.votoxveto.app.view.ActionBarDuelView;
import com.votoxveto.app.view.SplashScoreView;

import java.util.Collections;
import java.util.List;

/* TODO: Put initial load time */
public class MainActivity extends Activity {

    private static final String BUNDLE_KEY_SPLASH_SHOWN = "splashShown";
    private static final String PREF_KEY_FIRST_TIME = "firstTime";
    private static final TextResizer PROPOSAL_TEXT_RESIZER = TextResizer.linear()
            .setLimitingPoint(50, 24)
            .setLimitingPoint(300, 15)
            .build();
    private static final long SPLASH_SCREEN_TIMEOUT = 4000; // In milliseconds

    private RelativeLayout vContainer;
    private ActionBarDuelView vActionBar;
    private TextView vProposalTheme;
    private SplashScoreView vSplashScore;
    private TextView vFirstProposal;
    private TextView vSecondProposal;
    private RelativeLayout vSplashScreen;
    private ViewGroup vInstructionsScreen;
    private TextView vSplashScreenTitle;
    private LinearLayout vFirstPlaceholder;
    private View vSecondPlaceholder;
    private View vNoConnectivityScreen;
    private Button vDismissInstructions;

    private ProposalManager mProposalManager;
    private CachedAsyncIterator<Duel> mProposalProvider;
    private Duel mCurrentDuel;
    private Proposal mFirst;
    private ViewState mViewState;
    private ViewState mOldViewState;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        // Track app opened
        ParseAnalytics.trackAppOpened(getIntent());

        // Get views
        vContainer = (RelativeLayout) findViewById(R.id.container);
        vFirstProposal = (TextView) findViewById(R.id.left_proposal);
        vSecondProposal = (TextView) findViewById(R.id.right_proposal);
        vProposalTheme = (TextView) findViewById(R.id.proposal_theme);
        vActionBar = (ActionBarDuelView) findViewById(R.id.action_bar);
        vSplashScore = (SplashScoreView) findViewById(R.id.splash_score);
        vFirstPlaceholder = (LinearLayout) findViewById(R.id.left_proposal_placeholder);
        vSecondPlaceholder = findViewById(R.id.right_proposal_placeholder);
        vNoConnectivityScreen = findViewById(R.id.no_connectivity);
        vSplashScreen = (RelativeLayout) findViewById(R.id.splash_screen);
        vSplashScreenTitle = (TextView) vSplashScreen.findViewById(R.id.splash_screen_title);
        vInstructionsScreen = (ViewGroup) findViewById(R.id.instructions_screen);
        vDismissInstructions = (Button) vInstructionsScreen.findViewById(R.id.instructions_dismiss_button);
        vDismissInstructions.setOnClickListener(mOnDismissInstructionsListener);

        // Get prefs
        mPrefs = getSharedPreferences(CustomApplication.PREFERENCES_FILE, Context.MODE_PRIVATE);

        // Set font
        FontsLoader fontsLoader = FontsLoader.getInstance(this);
        ListenableFuture<Typeface> load = fontsLoader.getTypeface(FontsLoader.APPETITE);
        AsyncHelper.addCallbackOnUiThread(load, new FutureCallbackAdapter<Typeface>() {
            @Override
            public void onSuccess(Typeface typeface) {
                vSplashScreenTitle.setTypeface(typeface);
            }
        });

        // Set proper view
        if (!InternetHelper.hasInternetConnectivity(this)) {
            setViewState(ViewState.NO_CONNECTIVITY);
        } else {
            setViewState(ViewState.LOADING);
        }

        // Listen to network changes
        InternetHelper.executeWhenConnectivityChange(this, mConnectivityListener);

        // Retrieve model provider
        mProposalManager = ProposalManager.getInstance(this);
        mProposalProvider = mProposalManager.getDuelProvider();

        // Display first proposal
        ListenableFuture<Duel> future = mProposalProvider.next();
        AsyncHelper.addCallbackOnUiThread(future, mOnProposalUpdate);

        // Set listeners
        vFirstProposal.setOnClickListener(mOnProposalSelectedListener);
        vSecondProposal.setOnClickListener(mOnProposalSelectedListener);

        // Show splash screen
        boolean wasSplashShown = (state != null) && state.getBoolean(BUNDLE_KEY_SPLASH_SHOWN, false);
        if (!wasSplashShown) {
            vSplashScreen.setVisibility(View.VISIBLE);
            AsyncHelper.executeOnUiThread(mOnSplashScreenFinish, SPLASH_SCREEN_TIMEOUT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(BUNDLE_KEY_SPLASH_SHOWN, true);
    }

    private Runnable mOnSplashScreenFinish = new Runnable() {
        @Override
        public void run() {
            vSplashScreen.setVisibility(View.INVISIBLE);
            if (mPrefs.getBoolean(PREF_KEY_FIRST_TIME, true)) {
                mPrefs.edit().putBoolean(PREF_KEY_FIRST_TIME, false).apply();
                vInstructionsScreen.setVisibility(View.VISIBLE);
            }
        }
    };

    private View.OnClickListener mOnDismissInstructionsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            vInstructionsScreen.setVisibility(View.INVISIBLE);
        }
    };

    private Function<Boolean, Void> mConnectivityListener = new Function<Boolean, Void>() {
        @Override
        public Void apply(Boolean hasInternetConnectivity) {
            if (mProposalProvider.getCacheSize() == 0) {
                if (hasInternetConnectivity) {
                    setViewState(ViewState.LOADING);
                } else {
                    setViewState(ViewState.NO_CONNECTIVITY);
                }
            }
            return null;
        }
    };

    private View.OnClickListener mOnProposalSelectedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CustomHelper.log("====================== Proposal clicked! =======================");
            // Vote
            mCurrentDuel.vote((Proposal) v.getTag());

            // Update action bar
            vActionBar.update();

            // Show splash
            ListenableFuture<?> splash = Futures.immediateFuture(null);
            if (mCurrentDuel != null) {
                Candidate winner = mCurrentDuel.getWinner().getCandidate();
                Candidate loser = mCurrentDuel.getLoser().getCandidate();
                CustomHelper.log(":: w = " + winner.getName() + ", l = " + loser.getName());

                vActionBar.animateWinner(winner);

                vSplashScore.bind(winner, loser, winner == mFirst.getCandidate());
                splash = vSplashScore.enterAnimate(vActionBar.getFirstCandidate() == winner);
                vSplashScore.setVisibility(View.VISIBLE);
            }

            // Fetch next proposal
            ListenableFuture<Duel> future = mProposalProvider.next();
            AsyncHelper.addCallbackOnUiThread(splash, mOnSplashFinish);
            AsyncHelper.addCallbackOnUiThread(AsyncHelper.allWithFirstResult(future, splash), mOnProposalUpdate);
        }
    };

    private FutureCallback<Object> mOnSplashFinish = new FutureCallbackAdapter<Object>() {
        @Override
        public void onSuccess(Object result) {
            if (!InternetHelper.hasInternetConnectivity(MainActivity.this) && mProposalProvider.getCacheSize() == 0) {
                setViewState(ViewState.NO_CONNECTIVITY);
            } else {
                setViewState(ViewState.LOADING);
            }
        }
    };

    private FutureCallback<Duel> mOnProposalUpdate = new FutureCallbackAdapter<Duel>() {
        @Override
        public void onSuccess(Duel duel) {
            if (duel == null) { // End of proposals
                if (mCurrentDuel != null) { // Only show toast if it has just finished
                    Toast.makeText(MainActivity.this, R.string.no_more_proposals_toast, Toast.LENGTH_LONG).show();
                }
                Pair<Candidate, Candidate> candidates = mProposalManager.getCandidates();
                Candidate winner = candidates.first;
                Candidate loser = candidates.second;

                vProposalTheme.setText(R.string.no_more_proposals);
                vSplashScore.endScreen(winner, loser);
                vSplashScore.setVisibility(View.VISIBLE);
                setViewState(ViewState.EMPTY);
            } else {
                mCurrentDuel = duel;
                mFirst = duel.getFirstProposal();
                Proposal second = duel.getSecondProposal();
                if (!vActionBar.isBound()) {
                    vActionBar.bind(mFirst.getCandidate(), second.getCandidate());
                } else {
                    vActionBar.update();
                }
                vProposalTheme.setText(mFirst.getTheme());

                // Shuffle
                List<Proposal> proposals = Lists.newArrayList(mFirst, second);
                Collections.shuffle(proposals);
                mFirst = proposals.get(0);
                second = proposals.get(1);

                // Set views
                float size1 = PROPOSAL_TEXT_RESIZER.apply(vFirstProposal, mFirst.getText());
                vFirstProposal.setTag(mFirst);
                float size2 = PROPOSAL_TEXT_RESIZER.apply(vSecondProposal, second.getText());
                vSecondProposal.setTag(second);
                //vFirstProposal.setText(String.format("%s\n%s - %s - %s", mFirst.getText(), mFirst.getObjectId(), mFirst.getCandidateName(), mFirst.getCandidateState()));
                //vSecondProposal.setText(String.format("%s\n%s - %s - %s", second.getText(), second.getObjectId(), second.getCandidateName(), second.getCandidateState()));
                Toast.makeText(MainActivity.this, "1: length = " + mFirst.getText().length() + ", size = " + size1 + "\n2: length " + second.getText().length() + ", size = " + size2, Toast.LENGTH_LONG).show();

                // Update screen
                setViewState(ViewState.READY);
                vSplashScore.setVisibility(View.INVISIBLE);
            }
        }
    };

    private void setViewState(ViewState state) {
        mOldViewState = mViewState;
        mViewState = state;
        CustomViewHelper.toggleVisibleGone(vFirstPlaceholder, state == ViewState.LOADING);
        CustomViewHelper.toggleVisibleGone(vSecondPlaceholder, state == ViewState.LOADING);
        CustomViewHelper.toggleVisibleGone((View) vFirstProposal.getParent(), state == ViewState.READY);
        CustomViewHelper.toggleVisibleGone((View) vSecondProposal.getParent(), state == ViewState.READY);
        CustomViewHelper.toggleVisibleGone(vNoConnectivityScreen, state == ViewState.NO_CONNECTIVITY);
    }

    private static enum ViewState { LOADING, NO_CONNECTIVITY, READY, EMPTY }
}
