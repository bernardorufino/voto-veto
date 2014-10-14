package com.votoxveto.app.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import com.google.common.base.Function;
import com.votoxveto.app.R;
import com.votoxveto.app.helpers.CustomViewHelper;

import static com.google.common.base.Preconditions.checkArgument;

public class LinePlaceholderView extends LinearLayout {

    private static final long SHIMMER_DURATION = 800;

    private ViewGroup vLine;
    private View vGap;
    private View vShimmer;
    private TranslateAnimation mShimmerAnimation;
    private boolean mAnimationConfigured = false;

    public LinePlaceholderView(Context context) {
        super(context);
        init(null);
    }

    public LinePlaceholderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LinePlaceholderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        initView();
        if (attrs != null) {
            initAttrs(attrs);
        }
    }

    private void initView() {
        setOrientation(LinearLayout.HORIZONTAL);
        inflate(getContext(), R.layout.view_line_placeholder, this);
        vLine = (ViewGroup) findViewById(R.id.line_placeholder_line);
        vGap = findViewById(R.id.line_placeholder_gap);
        vShimmer = findViewById(R.id.line_placeholder_shimmer);
        CustomViewHelper.executeOnLayout(this, mOnLayout);
    }

    private final Function<? super LinePlaceholderView, Boolean> mOnLayout = new Function<LinePlaceholderView, Boolean>() {
        @Override
        public Boolean apply(LinePlaceholderView view) {
            if (!tryConfigureShimmerAnimation()) {
                return false;
            }
            return true;
        }
    };

    private boolean tryConfigureShimmerAnimation() {
        if (mAnimationConfigured) return true;

        int totalWidth = getWidth();
        int shimmerWidth = vShimmer.getWidth();
        if (totalWidth == 0 || shimmerWidth == 0) {
            return false;
        }

        mShimmerAnimation = new TranslateAnimation(-shimmerWidth, totalWidth, 0, 0);
        mShimmerAnimation.setDuration(SHIMMER_DURATION);
        mShimmerAnimation.setRepeatMode(Animation.RESTART);
        mShimmerAnimation.setRepeatCount(Animation.INFINITE);
        vShimmer.setAnimation(mShimmerAnimation);
        mShimmerAnimation.startNow();

        mAnimationConfigured = true;
        return true;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.LinePlaceholderView);
        try {
            setLineWidth(array.getFloat(R.styleable.LinePlaceholderView_line_width, 1f));
            /* TODO: Handle absence of line background attr */
            setLineBackground(array.getDrawable(R.styleable.LinePlaceholderView_line_background));
        } finally {
            array.recycle();
        }
    }

    public void setLineBackground(Drawable drawable) {
        CustomViewHelper.setBackground(vLine, drawable);
    }

    // Can't be called after animation, so privatizing for now, if needed handle it
    private void setLineWidth(float width) {
        checkArgument(0 <= width && width <= 1, "Line width must be between 0 and 1");

        LinearLayout.LayoutParams lineParams = (LayoutParams) vLine.getLayoutParams();
        lineParams.weight = width;
        vLine.setLayoutParams(lineParams); // Doc says nothing about getLayoutParams() returning safe copies

        LinearLayout.LayoutParams gapParams = (LayoutParams) vGap.getLayoutParams();
        gapParams.weight = 1 - width;
        vGap.setLayoutParams(gapParams);
    }
}
