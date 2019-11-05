/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet;

import androidx.lifecycle.ProcessLifecycleOwner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate;
import com.crashlytics.android.Crashlytics;
import com.github.moduth.blockcanary.BlockCanary;
import com.google.firebase.FirebaseApp;
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.wavesplatform.sdk.WavesSdk;
import com.wavesplatform.sdk.utils.Environment;
import com.wavesplatform.wallet.v2.data.analytics.Analytics;
import com.wavesplatform.wallet.v2.data.helpers.AuthHelper;
import com.wavesplatform.wallet.v2.data.manager.AccessManager;
import com.wavesplatform.wallet.v2.data.receiver.ScreenReceiver;
import com.wavesplatform.wallet.v2.injection.component.DaggerApplicationV2Component;
import com.wavesplatform.wallet.v2.util.EnvironmentManager;
import com.wavesplatform.wallet.v2.util.PrefsUtil;
import com.wavesplatform.wallet.v2.util.connectivity.ConnectivityManager;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import io.fabric.sdk.android.Fabric;
import io.reactivex.plugins.RxJavaPlugins;
import io.realm.Realm;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import pers.victor.ext.Ext;
import timber.log.Timber;

public class App extends DaggerApplication {

    @Inject
    PrefsUtil mPrefsUtil;
    @Inject
    AuthHelper authHelper;
    private static App application;
    private static AccessManager accessManager;
    private LocalizationApplicationDelegate localizationDelegate
            = new LocalizationApplicationDelegate(this);

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        Fabric.with(this, new Crashlytics());
        application = this;
        BlockCanary.install(this, new AppBlockCanaryContext()).start();

        Realm.init(this);
        Ext.INSTANCE.setCtx(this);

        Analytics.init(this);

        Sentry.init(new AndroidSentryClientFactory(this.getApplicationContext()));

        RxJavaPlugins.setErrorHandler(Timber::e);

        accessManager = new AccessManager(mPrefsUtil, authHelper);

        WavesSdk.init(this, Environment.Companion.getDEFAULT());
        EnvironmentManager.update();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        // sessions handlers
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        ConnectivityManager.getInstance().registerNetworkListener(this);


        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        SimpleChromeCustomTabs.initialize(this);
    }

    public static App getAppContext() {
        return application;
    }

    public static AccessManager getAccessManager() {
        return accessManager;
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationV2Component.builder().create(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localizationDelegate.attachBaseContext(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        localizationDelegate.onConfigurationChanged(this);
    }

    @Override
    public Context getApplicationContext() {
        return localizationDelegate.getApplicationContext(super.getApplicationContext());
    }
}
