package com.james.status.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.james.status.R;
import com.james.status.Status;
import com.james.status.adapters.SimplePagerAdapter;
import com.james.status.data.PreferenceData;
import com.james.status.data.icon.IconData;
import com.james.status.fragments.AppPreferenceFragment;
import com.james.status.fragments.GeneralPreferenceFragment;
import com.james.status.fragments.HelpFragment;
import com.james.status.fragments.IconPreferenceFragment;
import com.james.status.services.StatusServiceImpl;
import com.james.status.utils.StaticUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.jfenn.attribouter.Attribouter;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, CompoundButton.OnCheckedChangeListener {

    public static final String ACTION_TOO_MANY_ICONS = "com.james.status.MainActivity.TOO_MANY_ICONS";
    public static final String EXTRA_MANY_ICONS = "com.james.status.MainActivity.EXTRA_MANY_ICONS";

    private Status status;

    private SwitchCompat service;
    private SearchView searchView;

    private AppBarLayout appbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SimplePagerAdapter adapter;
    private View bottomSheet;
    ImageView expand;
    private TextView title, content;
    private ImageView icon;
    private FloatingActionButton fab;

    private BottomSheetBehavior behavior;
    private MenuItem resetItem;

    private TooManyIconsReceiver tooManyIconsReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (Status) getApplicationContext();

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        appbar = findViewById(R.id.appbar);
        tabLayout = findViewById(R.id.tabLayout);
        service = findViewById(R.id.serviceEnabled);
        viewPager = findViewById(R.id.viewPager);
        bottomSheet = findViewById(R.id.bottomSheet);
        expand = findViewById(R.id.expand);
        title = findViewById(R.id.title);
        content = findViewById(R.id.content);
        icon = findViewById(R.id.tutorialIcon);
        fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getItem(viewPager.getCurrentItem()) instanceof AppPreferenceFragment)
                    ((AppPreferenceFragment) adapter.getItem(viewPager.getCurrentItem())).showDialog();
            }
        });

        ViewCompat.setElevation(bottomSheet, StaticUtils.getPixelsFromDp(10));

        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    content.animate().alpha(1).start();
                    expand.animate().alpha(0).start();
                } else {
                    content.animate().alpha(0).start();
                    expand.animate().alpha(1).start();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    OnTutorialClickListener listener = (OnTutorialClickListener) v.getTag();
                    if (listener != null)
                        listener.onClick();

                    if (behavior.isHideable())
                        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });

        expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset != 0 && behavior != null && behavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        service.setChecked((boolean) PreferenceData.STATUS_ENABLED.getValue(this) && StaticUtils.isStatusServiceRunning(this));
        service.setOnCheckedChangeListener(this);

        adapter = new SimplePagerAdapter(this, getSupportFragmentManager(), viewPager, new GeneralPreferenceFragment(), new IconPreferenceFragment(), new AppPreferenceFragment(), new HelpFragment());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);

        tabLayout.setupWithViewPager(viewPager);

        tooManyIconsReceiver = new TooManyIconsReceiver(this);
        registerReceiver(tooManyIconsReceiver, new IntentFilter(ACTION_TOO_MANY_ICONS));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(tooManyIconsReceiver);
    }

    public void setTutorial(@StringRes int titleRes, @StringRes int contentRes, OnTutorialClickListener listener, boolean forceRead) {
        title.setText(titleRes);
        content.setText(contentRes);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.setHideable(!forceRead);
        appbar.setExpanded(true, true);
        bottomSheet.setTag(listener);

        if (forceRead) {
            bottomSheet.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            int textColorPrimary = ContextCompat.getColor(this, R.color.textColorPrimaryInverse);
            title.setTextColor(textColorPrimary);
            expand.setColorFilter(textColorPrimary);
            content.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondaryInverse));
            icon.setColorFilter(textColorPrimary);
        } else {
            bottomSheet.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            int textColorPrimary = ContextCompat.getColor(this, R.color.textColorPrimary);
            title.setTextColor(textColorPrimary);
            expand.setColorFilter(textColorPrimary);
            content.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary));
            icon.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        service.setOnCheckedChangeListener(null);
        service.setChecked((boolean) PreferenceData.STATUS_ENABLED.getValue(this) || StaticUtils.isStatusServiceRunning(this));
        service.setOnCheckedChangeListener(this);

        if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            if (!StaticUtils.isStatusServiceRunning(this) && StaticUtils.shouldShowTutorial(this, "enable")) {
                setTutorial(R.string.tutorial_enable, R.string.tutorial_enable_desc, new OnTutorialClickListener() {
                    @Override
                    public void onClick() {
                        if (service != null) service.setChecked(true);
                    }
                }, false);
            } else if (!StaticUtils.isAllPermissionsGranted(this)) {
                setTutorial(R.string.tutorial_missing_permissions, R.string.tutorial_missing_permissions_desc, new OnTutorialClickListener() {
                    @Override
                    public void onClick() {
                        List<String> permissions = new ArrayList<>();
                        for (IconData icon : StatusServiceImpl.getIcons(MainActivity.this)) {
                            permissions.addAll(Arrays.asList(icon.getPermissions()));
                        }

                        StaticUtils.requestPermissions(MainActivity.this, permissions.toArray(new String[permissions.size()]));
                    }
                }, true);
            } else if (searchView != null && StaticUtils.shouldShowTutorial(MainActivity.this, "search", 1)) {
                setTutorial(R.string.tutorial_search, R.string.tutorial_search_desc, new OnTutorialClickListener() {
                    @Override
                    public void onClick() {
                        if (searchView != null) searchView.setIconified(false);
                    }
                }, false);
            } else if (tabLayout != null && viewPager != null && viewPager.getCurrentItem() != 3 && StaticUtils.shouldShowTutorial(MainActivity.this, "faqs", 2)) {
                setTutorial(R.string.tutorial_help, R.string.tutorial_help_desc, new OnTutorialClickListener() {
                    @Override
                    public void onClick() {
                        if (viewPager != null) viewPager.setCurrentItem(3);
                    }
                }, false);
            } else if (StaticUtils.shouldShowTutorial(MainActivity.this, "donate", 3)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.tutorial_donate)
                        .setMessage(R.string.tutorial_donate_desc)
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=james.donate")));
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        resetItem = menu.findItem(R.id.action_reset);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(viewPager.getCurrentItem(), query.toLowerCase());
                appbar.setExpanded(true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(viewPager.getCurrentItem(), newText.toLowerCase());
                appbar.setExpanded(true);
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                adapter.filter(viewPager.getCurrentItem(), null);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_setup:
                startActivity(new Intent(this, StartActivity.class));
                break;
            case R.id.action_tutorial:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                for (String key : prefs.getAll().keySet()) {
                    if (key.startsWith("tutorial")) editor.remove(key);
                }

                editor.apply();
                break;
            case R.id.action_about:
                Attribouter attribouter = Attribouter.from(this);
                int githubAuthKey = getResources().getIdentifier("githubAuthKey", "string", getPackageName());
                if (githubAuthKey != 0)
                    attribouter = attribouter.withGitHubToken(getString(githubAuthKey));

                attribouter.show();
                break;
            case R.id.action_reset:
                if (adapter.getItem(viewPager.getCurrentItem()) instanceof AppPreferenceFragment)
                    ((AppPreferenceFragment) adapter.getItem(viewPager.getCurrentItem())).reset();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (StaticUtils.isAllPermissionsGranted(this) && !behavior.isHideable()) {
            behavior.setHideable(true);
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        status.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (resetItem != null) {
            if (adapter.getItem(position) instanceof AppPreferenceFragment) {
                fab.show();
                resetItem.setVisible(true);
            } else {
                fab.hide();
                resetItem.setVisible(false);
            }
        }

        if (behavior != null && behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            switch (position) {
                case 1:
                    if (StaticUtils.shouldShowTutorial(this, "disableicon")) {
                        setTutorial(R.string.tutorial_icon_switch, R.string.tutorial_icon_switch_desc, null, false);
                    } else if (StaticUtils.shouldShowTutorial(this, "moveicon", 1)) {
                        setTutorial(R.string.tutorial_icon_order, R.string.tutorial_icon_order_desc, null, false);
                    }
                    break;
                case 2:
                    if (StaticUtils.shouldShowTutorial(this, "activities")) {
                        setTutorial(R.string.tutorial_activities, R.string.tutorial_activities_desc, null, false);
                    }
                    break;
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
            StatusServiceImpl.start(this);
            if (!StaticUtils.isReady(this)) {
                startActivity(new Intent(this, StartActivity.class));

                service.setOnCheckedChangeListener(null);
                service.setChecked(false);
                service.setOnCheckedChangeListener(this);
                return;
            }
        } else StatusServiceImpl.stop(this);

        PreferenceData.STATUS_ENABLED.setValue(this, b);
    }

    public interface OnTutorialClickListener {
        void onClick();
    }

    public static class TooManyIconsReceiver extends BroadcastReceiver {

        private MainActivity activity;

        public TooManyIconsReceiver(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_TOO_MANY_ICONS)) {
                if (intent.getBooleanExtra(EXTRA_MANY_ICONS, true))
                    activity.setTutorial(R.string.tutorial_too_many_icons, R.string.tutorial_too_many_icons_desc, null, true);
                else if (!activity.behavior.isHideable()) {
                    activity.behavior.setHideable(true);
                    activity.behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        }
    }
}
