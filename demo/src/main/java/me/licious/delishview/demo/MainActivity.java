package me.licious.delishview.demo;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SpinnerAdapter navAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[] {
                "Basic",
                "DragView",
                "DragView + Headers",
                "Swipe to Refresh"
        });

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(navAdapter, new OnNavigationListener() {
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                setFragment(itemPosition);
                return true;
            }
        });

        setFragment(0);
    }

    private void setFragment(int ix) {
        Fragment fragment = null;
        switch(ix) {
            case 0: fragment = new BasicFragment();         break;
            case 1: fragment = new DragViewFragment();      break;
            case 2: fragment = new HeadersFragment();       break;
            case 3: fragment = new SwipToRefreshFragment(); break;
        }
        if(fragment != null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
            .commit();
        }
    }

}
