/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.setupwizard.setup;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.CheckBox;
import android.widget.TextView;

import com.cyanogenmod.setupwizard.ui.SetupPageFragment;
import com.cyanogenmod.setupwizard.util.SetupWizardUtils;
import com.cyanogenmod.setupwizard.util.WhisperPushUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.namelessrom.setupwizard.R;

public class CyanogenSettingsPage extends SetupPage {

    public static final String TAG = "CyanogenSettingsPage";

    public static final String KEY_SEND_METRICS = "send_metrics";
    public static final String KEY_REGISTER_WHISPERPUSH = "register";
    public static final String KEY_ENABLE_NAV_KEYS = "enable_nav_keys";
    public static final String KEY_APPLY_DEFAULT_THEME = "apply_default_theme";

    private static final String WHISPERPUSH_PACKAGE = "org.whispersystems.whisperpush";

    public CyanogenSettingsPage(Context context, SetupDataCallbacks callbacks) {
        super(context, callbacks);
    }

    @Override
    public Fragment getFragment(FragmentManager fragmentManager, int action) {
        Fragment fragment = fragmentManager.findFragmentByTag(getKey());
        if (fragment == null) {
            Bundle args = new Bundle();
            args.putString(Page.KEY_PAGE_ARGUMENT, getKey());
            args.putInt(Page.KEY_PAGE_ACTION, action);
            fragment = new CyanogenSettingsFragment();
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public String getKey() {
        return TAG;
    }

    @Override
    public int getTitleResId() {
        return R.string.setup_services_nameless;
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.HARDWARE_KEYS_DISABLE, enabled ? 1 : 0);
    }

    @Override
    public void onFinishSetup() {
        getCallbacks().addFinishRunnable(new Runnable() {
            @Override
            public void run() {
                if (getData().containsKey(KEY_ENABLE_NAV_KEYS)) {
                    writeDisableNavkeysOption(mContext, getData().getBoolean(KEY_ENABLE_NAV_KEYS));
                }
            }
        });
        handleWhisperPushRegistration();
        handleEnableMetrics();
        handleDefaultThemeSetup();
    }

    private void handleWhisperPushRegistration() {
        Bundle privacyData = getData();
        if (privacyData != null &&
                privacyData.containsKey(CyanogenSettingsPage.KEY_REGISTER_WHISPERPUSH) &&
                privacyData.getBoolean(CyanogenSettingsPage.KEY_REGISTER_WHISPERPUSH)) {
            Log.i(TAG, "Registering with WhisperPush");
            WhisperPushUtils.startRegistration(mContext);
        }
    }

    private void handleEnableMetrics() {
        Bundle privacyData = getData();
        if (privacyData != null
                && privacyData.containsKey(CyanogenSettingsPage.KEY_SEND_METRICS)) {
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.STATS_COLLECTION,
                    privacyData.getBoolean(CyanogenSettingsPage.KEY_SEND_METRICS) ? 1 : 0);
        }
    }

    private void handleDefaultThemeSetup() {
        Bundle themeData = getData();
        if (!ThemeUtils.getDefaultThemePackageName(mContext).equals(ThemeConfig.SYSTEM_DEFAULT) &&
                themeData != null && themeData.getBoolean(KEY_APPLY_DEFAULT_THEME)) {
            Log.i(TAG, "Applying default theme");
            final ThemeManager tm = (ThemeManager) mContext.getSystemService(Context.THEME_SERVICE);
            tm.applyDefaultTheme();
        } else {
            getCallbacks().finishSetup();
        }
    }

    private static boolean hideWhisperPush(Context context) {
        final int playServicesAvailable = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(context);
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(WHISPERPUSH_PACKAGE, 0);
            if (pi == null) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
        return playServicesAvailable != ConnectionResult.SUCCESS
                || !SetupWizardUtils.hasTelephony(context)
                || (SetupWizardUtils.hasTelephony(context) &&
                SetupWizardUtils.isSimMissing(context));
    }

    protected static boolean hideThemeSwitch(Context context) {
        return ThemeUtils.getDefaultThemePackageName(context).equals(ThemeConfig.SYSTEM_DEFAULT);
    }

    public static class CyanogenSettingsFragment extends SetupPageFragment {
        private View mMetricsRow;
        private View mDefaultThemeRow;
        private View mNavKeysRow;
        private View mSecureSmsRow;
        private CheckBox mMetrics;
        private CheckBox mDefaultTheme;
        private CheckBox mNavKeys;
        private CheckBox mSecureSms;

        private boolean mHideNavKeysRow = false;
        private boolean mHideThemeRow = false;
        private boolean mHideSmsRow = false;

        private View.OnClickListener mMetricsClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mMetrics.isChecked();
                mMetrics.setChecked(checked);
                mPage.getData().putBoolean(KEY_SEND_METRICS, checked);
            }
        };

        private View.OnClickListener mDefaultThemeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mDefaultTheme.isChecked();
                mDefaultTheme.setChecked(checked);
                mPage.getData().putBoolean(KEY_APPLY_DEFAULT_THEME, checked);
            }
        };

        private View.OnClickListener mNavKeysClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mNavKeys.isChecked();
                mNavKeys.setChecked(checked);
                mPage.getData().putBoolean(KEY_ENABLE_NAV_KEYS, checked);
            }
        };

        private View.OnClickListener mSecureSmsClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mSecureSms.isChecked();
                mSecureSms.setChecked(checked);
                mPage.getData().putBoolean(KEY_REGISTER_WHISPERPUSH, checked);
            }
        };

        @Override
        protected void initializePage() {
            mMetricsRow = mRootView.findViewById(R.id.metrics);
            mMetricsRow.setOnClickListener(mMetricsClickListener);
            String osName = getString(R.string.os_name_nameless);
            String metricsHelpImproveNameless =
                    getString(R.string.services_help_improve_nameless, osName);
            String metricsSummary = getString(R.string.services_metrics_label_nameless,
                    metricsHelpImproveNameless, osName);
            final SpannableStringBuilder metricsSpan = new SpannableStringBuilder(metricsSummary);
            metricsSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    0, metricsHelpImproveNameless.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView metrics = (TextView) mRootView.findViewById(R.id.enable_metrics_summary);
            metrics.setText(metricsSpan);
            mMetrics = (CheckBox) mRootView.findViewById(R.id.enable_metrics_checkbox);

            mDefaultThemeRow = mRootView.findViewById(R.id.theme);
            mHideThemeRow = hideThemeSwitch(getActivity());
            if (mHideThemeRow) {
                mDefaultThemeRow.setVisibility(View.GONE);
            } else {
                mDefaultThemeRow.setOnClickListener(mDefaultThemeClickListener);
                String defaultTheme = getString(R.string.services_apply_theme,
                        getString(R.string.default_theme_name));
                String defaultThemeSummary = getString(R.string.services_apply_theme_label,
                        defaultTheme);
                final SpannableStringBuilder themeSpan =
                        new SpannableStringBuilder(defaultThemeSummary);
                themeSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        0, defaultTheme.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView theme = (TextView) mRootView.findViewById(R.id.enable_theme_summary);
                theme.setText(themeSpan);
                mDefaultTheme = (CheckBox) mRootView.findViewById(R.id.enable_theme_checkbox);
            }

            mNavKeysRow = mRootView.findViewById(R.id.nav_keys);
            mHideNavKeysRow = true;
            try {
                IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
                mHideNavKeysRow = windowManager.needsNavigationBar();
            } catch (RemoteException e) {
            }

            if (mHideNavKeysRow) {
                mNavKeysRow.setVisibility(View.GONE);
            } else {
                mNavKeysRow.setOnClickListener(mNavKeysClickListener);
                mNavKeys = (CheckBox) mRootView.findViewById(R.id.nav_keys_checkbox);
            }

            mSecureSmsRow = mRootView.findViewById(R.id.secure_sms);
            mSecureSmsRow.setOnClickListener(mSecureSmsClickListener);
            String useSecureSms = getString(R.string.services_use_secure_sms);
            String secureSmsSummary = getString(R.string.services_secure_sms_label,
                    useSecureSms, getString(R.string.os_name_nameless));
            final SpannableStringBuilder secureSmsSpan =
                    new SpannableStringBuilder(secureSmsSummary);
            secureSmsSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    0, useSecureSms.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView secureSms = (TextView) mRootView.findViewById(R.id.secure_sms_summary);
            secureSms.setText(secureSmsSpan);
            mHideSmsRow = hideWhisperPush(getActivity());
            if (mHideSmsRow) {
                mSecureSmsRow.setVisibility(View.GONE);
            }
            mSecureSms = (CheckBox) mRootView.findViewById(R.id.secure_sms_checkbox);
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_cyanogen_services;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateDisableNavkeysOption();
            updateThemeOption();
            updateSmsOption();
        }

        private void updateThemeOption() {
            if (!mHideThemeRow) {
                final Bundle myPageBundle = mPage.getData();
                boolean themesChecked = !myPageBundle.containsKey(KEY_APPLY_DEFAULT_THEME) ||
                        myPageBundle.getBoolean(KEY_APPLY_DEFAULT_THEME);
                mDefaultTheme.setChecked(themesChecked);
                myPageBundle.putBoolean(KEY_APPLY_DEFAULT_THEME, themesChecked);
            }
        }

        private void updateSmsOption() {
            if (!mHideSmsRow) {
                final Bundle myPageBundle = mPage.getData();
                boolean smsChecked = !myPageBundle.containsKey(KEY_REGISTER_WHISPERPUSH) ||
                        myPageBundle.getBoolean(KEY_REGISTER_WHISPERPUSH);
                mSecureSms.setChecked(smsChecked);
                myPageBundle.putBoolean(KEY_REGISTER_WHISPERPUSH, smsChecked);
            }
        }

        private void updateDisableNavkeysOption() {
            if (!mHideNavKeysRow) {
                final Bundle myPageBundle = mPage.getData();
                boolean enabled = Settings.Secure.getInt(getActivity().getContentResolver(),
                        Settings.Secure.DEV_FORCE_SHOW_NAVBAR, 0) != 0;
                boolean checked = myPageBundle.containsKey(KEY_ENABLE_NAV_KEYS) ?
                        myPageBundle.getBoolean(KEY_ENABLE_NAV_KEYS) :
                        enabled;
                mNavKeys.setChecked(checked);
            }
        }

    }
}
