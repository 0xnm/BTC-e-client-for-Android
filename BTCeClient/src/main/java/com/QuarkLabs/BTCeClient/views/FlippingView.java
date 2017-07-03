package com.QuarkLabs.BTCeClient.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/*
 * BTC-e client
 *     Copyright (C) 2014  QuarkDev Solutions <quarkdev.solutions@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class FlippingView extends FrameLayout {

    private View mFrontCover;
    private View mBackCover;
    private AnimatorSet mFlipLeftOut;
    private AnimatorSet mFlipLeftIn;
    private AnimatorSet mFlipRightOut;
    private AnimatorSet mFlipRightIn;
    private boolean frontCoverShowing = true;

    public FlippingView(Context context) {
        this(context, null, 0);
    }

    public FlippingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlippingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addAnimators(AnimatorSet leftOut, AnimatorSet leftIn, AnimatorSet rightOut,
                             AnimatorSet rightIn) {
        mFlipLeftOut = leftOut.clone();
        mFlipLeftIn = leftIn.clone();
        mFlipRightOut = rightOut.clone();
        mFlipRightIn = rightIn.clone();
        mFlipLeftIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBackCover.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFrontCover.setVisibility(GONE);
                frontCoverShowing = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mFlipRightIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFrontCover.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBackCover.setVisibility(GONE);
                frontCoverShowing = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void startFlipping() {
        mFrontCover = getChildAt(0);
        mBackCover = getChildAt(1);
        if (mFrontCover == null || mBackCover == null) {
            throw new IllegalStateException("FlippingView should have 2 child");
        }
        //keeping the height of back cover equal to front cover
        int frontCoverHeight = mFrontCover.getHeight();
        mBackCover.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        frontCoverHeight)
        );

        if (mFlipLeftIn == null || mFlipRightIn == null
                || mFlipLeftOut == null || mFlipRightOut == null) {
            throw new IllegalStateException("Animators should be added at first");
        }
        mFlipRightIn.setTarget(mFrontCover);
        mFlipRightOut.setTarget(mBackCover);
        mFlipLeftIn.setTarget(mBackCover);
        mFlipLeftOut.setTarget(mFrontCover);
        if (frontCoverShowing) {
            mFlipLeftIn.start();
            mFlipLeftOut.start();
        } else {
            mFlipRightIn.start();
            mFlipRightOut.start();
        }
    }
}
