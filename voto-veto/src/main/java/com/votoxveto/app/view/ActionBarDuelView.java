package com.votoxveto.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.facebook.rebound.*;
import com.makeramen.RoundedImageView;
import com.nineoldandroids.view.ViewHelper;
import com.squareup.picasso.Picasso;
import com.votoxveto.app.R;
import com.votoxveto.app.helpers.CustomViewHelper;
import com.votoxveto.app.model.Candidate;
import com.votoxveto.app.model.ProposalManager;

public class ActionBarDuelView extends FrameLayout {

    private static final SpringSystem SPRING_SYSTEM = SpringSystem.create();
    private static final SpringConfig SPRING_CONFIG = new SpringConfig(90, 6);

    private TextView vFirstScore;
    private TextView vSecondScore;
    private RoundedImageView vFirstImage;
    private RoundedImageView vSecondImage;
    private ProposalManager mProposalManager;

    private Candidate mFirstCandidate;
    private Candidate mSecondCandidate;
    private RoundedImageView mAnimatedImage;
    private TextView mAnimatedScore;
    private int mAnimationFinalColor;
    private int mAnimationStartColor;

    public ActionBarDuelView(Context context) {
        super(context);
        init();
    }

    public ActionBarDuelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActionBarDuelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.action_bar_duel, this);
        vFirstScore = (TextView) findViewById(R.id.action_bar_duel_first_candidate_score);
        vSecondScore = (TextView) findViewById(R.id.action_bar_duel_second_candidate_score);
        vFirstImage = (RoundedImageView) findViewById(R.id.action_bar_duel_first_candidate_image);
        vSecondImage = (RoundedImageView) findViewById(R.id.action_bar_duel_second_candidate_image);
        mProposalManager = ProposalManager.getInstance(getContext());
        mAnimationFinalColor = getResources().getColor(R.color.action_bar_highlighted_text);
        mAnimationStartColor = vFirstScore.getTextColors().getDefaultColor();
    }

    public Candidate getFirstCandidate() {
        return mFirstCandidate;
    }

    public Candidate getSecondCandidate() {
        return mSecondCandidate;
    }

    public ActionBarDuelView bind(Candidate firstCandidate, Candidate secondCandidate) {
        // Order alphabetically
        if (firstCandidate.getName().compareTo(secondCandidate.getName()) > 0) {
            Candidate tmp = secondCandidate;
            secondCandidate = firstCandidate;
            firstCandidate = tmp;
        }

        mFirstCandidate = firstCandidate;
        mSecondCandidate = secondCandidate;
        update();
        return this;
    }

    public boolean isBound() {
        return mFirstCandidate != null && mSecondCandidate != null;
    }

    public ActionBarDuelView update() {
        String firstNumber = mFirstCandidate.getId();
        String secondNumber = mSecondCandidate.getId();
        vFirstScore.setText(Integer.toString(mProposalManager.getCandidateScore(firstNumber)));
        vSecondScore.setText(Integer.toString(mProposalManager.getCandidateScore(secondNumber)));
        Picasso picasso = Picasso.with(getContext());
        picasso.load(mFirstCandidate.getImageUrl()).placeholder(R.drawable.placeholder).into(vFirstImage);
        picasso.load(mSecondCandidate.getImageUrl()).placeholder(R.drawable.placeholder).into(vSecondImage);
        return this;
    }

    private SpringListener mSpringListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            float i = (float) spring.getCurrentValue(); // 0 -> 1
            ViewHelper.setScaleX(mAnimatedImage, i);
            ViewHelper.setScaleY(mAnimatedImage, i);
            ViewHelper.setScaleX(mAnimatedScore, i);
            ViewHelper.setScaleY(mAnimatedScore, i);
        }
    };

    public ActionBarDuelView animateWinner(Candidate winner) {
        mAnimatedImage = (winner.equals(mFirstCandidate)) ? vFirstImage : vSecondImage;
        mAnimatedScore = (winner.equals(mFirstCandidate)) ? vFirstScore : vSecondScore;
        if (CustomViewHelper.hasAnimation()) {
            Spring spring = SPRING_SYSTEM.createSpring();
            spring.setSpringConfig(SPRING_CONFIG);
            spring.addListener(mSpringListener);
            spring.setCurrentValue(1);
            spring.setVelocity(10);
            spring.setEndValue(1);
        }
        return this;
    }
}
