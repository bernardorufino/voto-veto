package com.votoxveto.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.facebook.rebound.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.makeramen.RoundedImageView;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.squareup.picasso.Picasso;
import com.votoxveto.app.R;
import com.votoxveto.app.helpers.AsyncHelper;
import com.votoxveto.app.helpers.CustomViewHelper;
import com.votoxveto.app.model.Candidate;

public class SplashScoreView extends FrameLayout {

    private static final SpringSystem SPRING_SYSTEM = SpringSystem.create();
    private static final SpringConfig SPRING_CONFIG = new SpringConfig(95, 10);
    private static final int TIMEOUT_AFTER_REST = 800;
    public static final int FADE_DURATION = 600;
    public static final float FINAL_WINNER_ALPHA = 1f;
    public static final float FINAL_LOSER_ALPHA = .9f;

    private RoundedImageView vLoserImage;
    private RoundedImageView vWinnerImage;
    private TextView vLoserName;
    private TextView vWinnerName;
    private ViewGroup vWinnerContainer;
    private ViewGroup vLoserContainer;
    private LinearLayout mContainer;
    private SettableFuture<SplashScoreView> mSpringFuture;
    private boolean mAnimationWinnerFromLeft;

    public SplashScoreView(Context context) {
        super(context);
        init();
    }

    public SplashScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SplashScoreView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mContainer = (LinearLayout) inflate(getContext(), R.layout.view_splash_score, null);
        setClickable(true);
        addView(mContainer);
        vWinnerContainer = (ViewGroup) findViewById(R.id.splash_score_winner_container);
        vLoserContainer = (ViewGroup) findViewById(R.id.splash_score_loser_container);
        vWinnerName = (TextView) findViewById(R.id.splash_score_second_candidate_name);
        vLoserName = (TextView) findViewById(R.id.splash_score_first_candidate_name);
        vWinnerImage = (RoundedImageView) findViewById(R.id.splash_score_second_candidate_image);
        vLoserImage = (RoundedImageView) findViewById(R.id.splash_score_first_candidate_image);
        ViewHelper.setAlpha(vWinnerContainer, FINAL_WINNER_ALPHA);
        ViewHelper.setAlpha(vLoserContainer, FINAL_LOSER_ALPHA);
    }

    // Winner is either the first or the second candidate
    public SplashScoreView bind(Candidate winner, Candidate loser, boolean winnerFirst) {
        vWinnerName.setText(winner.getName());
        vLoserName.setText(loser.getName());
        Picasso.with(getContext()).load(winner.getImageUrl()).placeholder(R.drawable.placeholder).into(vWinnerImage);
        Picasso.with(getContext()).load(loser.getImageUrl()).placeholder(R.drawable.placeholder).into(vLoserImage);

        int w = mContainer.indexOfChild(vWinnerContainer);
        int l = mContainer.indexOfChild(vLoserContainer);
        if (winnerFirst && w > l || !winnerFirst && w < l) {
            View first = (winnerFirst) ? vWinnerContainer : vLoserContainer;
            View second = (winnerFirst) ? vLoserContainer : vWinnerContainer;
            mContainer.removeView(vWinnerContainer);
            mContainer.removeView(vLoserContainer);
            mContainer.addView(first);
            mContainer.addView(second);
        }
        return this;
    }

    private SpringListener mSpringListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            float i = (float) spring.getCurrentValue();// 0 -> 1
            int m = (mAnimationWinnerFromLeft) ? 1 : -1;
            int h = vWinnerContainer.getHeight();
            ViewHelper.setX(vWinnerContainer, m * (i - 1) * h);
        }
        @Override
        public void onSpringAtRest(Spring spring) {
            AsyncHelper.setWithDelay(mSpringFuture, SplashScoreView.this, TIMEOUT_AFTER_REST);
        }
    };

    public ListenableFuture<SplashScoreView> enterAnimate(boolean winnerFromLeft) {
        if (CustomViewHelper.hasAnimation()) {
            // Slide
            mAnimationWinnerFromLeft = winnerFromLeft;
            mSpringFuture = SettableFuture.create();
            Spring spring = SPRING_SYSTEM.createSpring();
            spring.setSpringConfig(SPRING_CONFIG);
            spring.addListener(mSpringListener);
            spring.setCurrentValue(0);
            spring.setEndValue(1);

            // Fade in
            ViewHelper.setAlpha(vLoserContainer, 0);
            ViewPropertyAnimator.animate(vLoserContainer)
                    .alpha(FINAL_LOSER_ALPHA)
                    .setDuration(FADE_DURATION)
                    .start();

            return mSpringFuture;
        } else {
            return AsyncHelper.timeOut(1000, this);
        }
    }

    public SplashScoreView endScreen(Candidate winner, Candidate loser) {
        bind(winner, loser, true);
        ViewGroup.LayoutParams params = vWinnerImage.getLayoutParams();
        params.width = getResources().getDimensionPixelSize(R.dimen.splash_score_winner_picture);
        params.height = params.width;
        vWinnerImage.setLayoutParams(params);
        ViewHelper.setAlpha(vLoserImage, 0.5f);
        ViewHelper.setAlpha(vLoserName, 0.5f);
        return this;
    }
}
