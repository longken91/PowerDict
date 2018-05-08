package com.ltao.pdict.view;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v7.widget.ActionMenuView;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ltao.pdict.R;
import com.ltao.pdict.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;


public class PowerSearchView extends RelativeLayout {

    private static final int DEFAULT_CONTENT_COLOR = 0xffffffff;
//    private static final int DEFAULT_CONTENT_COLOR = 0xfff0f0f0;

    private static final long DEFAULT_DURATION_ENTER = 200;
    private static final long DEFAULT_DURATION_EXIT = 300;

    private static final Interpolator DECELERATE = new DecelerateInterpolator(3f);
    private static final Interpolator ACCELERATE = new AccelerateInterpolator(2f);

    public interface OnSearchListener {
        void onSearchAction(CharSequence text);
    }

    public interface OnIconClickListener {
        void onNavigationClick();
    }

    public interface OnSearchFocusChangedListener {
        void onFocusChanged(boolean focused);
    }

    final private EditText mSearchInput;
    final private ImageView mNavButtonView;
    final private ActionMenuView mActionMenu;

    final private Activity mActivity;

    final private List<Integer> mAlwaysShowingMenu = new ArrayList<>();

    private OnSearchFocusChangedListener mFocusListener;
    private OnIconClickListener mNavigationClickListener;

    public PowerSearchView(Context context) {
        this(context, null);
    }

    public PowerSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.powerSearchViewStyle);
    }

    public PowerSearchView(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (isInEditMode()) {
            mActivity = null;
        } else {
            mActivity = getActivity();
        }

        setFocusable(true);
        setFocusableInTouchMode(true);

        inflate(getContext(), R.layout.layout_search_query, this);
        mSearchInput = (EditText) findViewById(R.id.psv_search_text);
        mNavButtonView = (ImageView) findViewById(R.id.psv_search_action_navigation);
        mActionMenu = (ActionMenuView) findViewById(R.id.psv_search_action_menu);

        applyXmlAttributes(attrs, defStyleAttr, 0);
        setupViews();
    }

    private void applyXmlAttributes(AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.PowerSearchView, defStyleAttr, defStyleRes);
        setContentBackgroundColor(a.getColor(R.styleable.PowerSearchView_psv_contentBackgroundColor, DEFAULT_CONTENT_COLOR));
        inflateMenu(a.getResourceId(R.styleable.PowerSearchView_psv_menu, 0));
        setPopupTheme(a.getResourceId(R.styleable.PowerSearchView_popupTheme, 0));
        setHint(a.getString(R.styleable.PowerSearchView_android_hint));

        a.recycle();
    }

    private void setupViews() {
        mNavButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNavigationClickListener != null)
                    mNavigationClickListener.onNavigationClick();
            }
        });

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isActivated()) return false;
                setActivated(false);
                return true;
            }
        });

        mSearchInput.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus != isActivated()) setActivated(hasFocus);
            }
        });

        mSearchInput.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                setActivated(false);
                return true;
            }
        });
    }

    public void setContentBackgroundColor(@ColorInt int color) {
        mActionMenu.setBackgroundColor(color);
    }

    public Menu getMenu() {
        return mActionMenu.getMenu();
    }

    public void setPopupTheme(@StyleRes int resId) {
        mActionMenu.setPopupTheme(resId);
    }

    public void inflateMenu(@MenuRes int menuRes) {
        if(menuRes == 0) return;
        if (isInEditMode()) return;
        getActivity().getMenuInflater().inflate(menuRes, mActionMenu.getMenu());
        mAlwaysShowingMenu.add(getMenu().getItem(0).getItemId());
    }

    public void setOnSearchListener(final OnSearchListener listener) {
        mSearchInput.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
                listener.onSearchAction(mSearchInput.getText());
                return true;
            }
        });
    }

    public void setOnMenuItemClickListener(ActionMenuView.OnMenuItemClickListener listener) {
        mActionMenu.setOnMenuItemClickListener(listener);
    }

    public CharSequence getText() {
        return mSearchInput.getText();
    }

    public void setText(CharSequence text) {
        mSearchInput.setText(text);
    }

    public void setHint(CharSequence hint) {
        mSearchInput.setHint(hint);
    }

    @Override
    public void setActivated(boolean activated) {
        if(activated == isActivated()) return;

        super.setActivated(activated);

        if(activated) {
            mSearchInput.requestFocus();
            ViewUtils.showSoftKeyboardDelayed(mSearchInput, 100);
        }else {
            requestFocus();
            ViewUtils.closeSoftKeyboard(mActivity);
        }

        if(mFocusListener != null)
            mFocusListener.onFocusChanged(activated);

        showMenu(!activated);
        Drawable icon = unwrap(getIcon());

        if(icon != null) {
            ObjectAnimator iconAnim = ObjectAnimator.ofFloat(icon, "progress", activated ? 1 : 0);
            iconAnim.setDuration(activated ? DEFAULT_DURATION_ENTER : DEFAULT_DURATION_EXIT);
            iconAnim.setInterpolator(activated ? DECELERATE : ACCELERATE);
            iconAnim.start();
        }
    }

    public void setOnIconClickListener(OnIconClickListener navigationClickListener) {
        mNavigationClickListener = navigationClickListener;
    }

    public void setOnSearchFocusChangedListener(OnSearchFocusChangedListener focusListener) {
        mFocusListener = focusListener;
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        mSearchInput.addTextChangedListener(textWatcher);
    }

    public void removeTextChangedListener(TextWatcher textWatcher) {
        mSearchInput.removeTextChangedListener(textWatcher);
    }

    public void setIcon(@DrawableRes int resId) {
        showIcon(resId != 0);
        mNavButtonView.setImageResource(resId);
    }

    public void setIcon(Drawable drawable) {
        showIcon(drawable != null);
        mNavButtonView.setImageDrawable(drawable);
    }

    public void showIcon(boolean show) {
        mNavButtonView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public Drawable getIcon() {
        if(mNavButtonView == null) return null;
        return mNavButtonView.getDrawable();
    }

    @NonNull
    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        throw new IllegalStateException();
    }

    private void showMenu(final boolean visible) {
        Menu menu = getMenu();
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if(mAlwaysShowingMenu.contains(item.getItemId())) continue;
            item.setVisible(visible);
        }
    }

    static private Drawable unwrap(Drawable icon) {
        if(icon instanceof android.support.v7.graphics.drawable.DrawableWrapper)
            return ((android.support.v7.graphics.drawable.DrawableWrapper)icon).getWrappedDrawable();
        if(Build.VERSION.SDK_INT >= 23 && icon instanceof android.graphics.drawable.DrawableWrapper)
            return ((android.graphics.drawable.DrawableWrapper)icon).getDrawable();
        return DrawableCompat.unwrap(icon);
    }
}
