package com.votoxveto.app.ext.textresizer;

import android.widget.TextView;

import static com.google.common.base.Preconditions.checkState;

public abstract class TextResizer {

    public static LinearBuilder linear() {
        return new LinearBuilder();
    }

    public abstract float convert(int length);

    public float apply(TextView textView, CharSequence text) {
        float size = convert(text.length());
        textView.setTextSize(size);
        textView.setText(text);
        return size;
    }

    public static class LinearBuilder {

        private InterpolationPoint mPointA;
        private InterpolationPoint mPointB;

        private void setPoint(int textLength, float textSize, boolean limit) {
            checkState(mPointB == null, "Cannot specify more than 2 points");

            if (mPointA == null) {
                mPointA = new InterpolationPoint(textLength, textLength, limit);
            } else {
                mPointB = new InterpolationPoint(textLength, textLength, limit);
                if (mPointB.x < mPointA.x) {
                    InterpolationPoint tmp = mPointA;
                    mPointA = mPointB;
                    mPointB = tmp;
                }
            }
        }

        public LinearBuilder setPoint(int textLength, float textSize) {
            setPoint(textLength, textSize, false);
            return this;
        }

        public LinearBuilder setLimitingPoint(int textLength, float textSize) {
            setPoint(textLength, textSize, true);
            return this;
        }

        public TextResizer build() {
            return new TextResizer() {
                @Override
                public float convert(int x) {
                    if (mPointA.limiting && x < mPointA.x) return mPointA.y;
                    if (mPointB.limiting && x > mPointB.x) return mPointB.y;
                    return mPointA.y + (x - mPointA.x) * (mPointB.y - mPointA.y) / (mPointB.x - mPointA.x);
                }
            };
        }

        private static class InterpolationPoint {

            public int x;
            public float y;
            public boolean limiting;

            private InterpolationPoint(int x, float y, boolean limiting) {
                this.x = x;
                this.y = y;
                this.limiting = limiting;
            }
        }


    }
}
