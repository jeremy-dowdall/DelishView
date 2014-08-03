package me.licious.view;

import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;

import java.lang.reflect.Field;

public class ParentBlocker {


    private final View view;
    private SwipeRefreshLayout parentSwipe;
    private boolean parentSwipeEnabled;
    private DrawerLayout parentDrawer;
    private int[] parentDrawerLocks;

    ParentBlocker(View view) {
        this.view = view;
    }

    void start() {
        ViewParent parent = view.getParent();
        while(parent != null) {
            if(parent instanceof ViewPager) {
                try {
                    Field mIsUnableToDrag = parent.getClass().getDeclaredField("mIsUnableToDrag");
                    mIsUnableToDrag.setAccessible(true);
                    mIsUnableToDrag.setBoolean(parent, true);
                } catch(Exception e) {
                    Log.wtf(getClass().getSimpleName(), "unable to block ViewPager: " + e.getLocalizedMessage());
                }
            }
            if(parent instanceof SwipeRefreshLayout) {
                parentSwipe = (SwipeRefreshLayout) parent;
                parentSwipeEnabled = parentSwipe.isEnabled();
                parentSwipe.setEnabled(false);
            }
            if(parent instanceof DrawerLayout) {
                parentDrawer = (DrawerLayout) parent;
                parentDrawerLocks = new int[] { parentDrawer.getDrawerLockMode(Gravity.LEFT), parentDrawer.getDrawerLockMode(Gravity.RIGHT) };
                if(parentDrawer.isDrawerOpen(Gravity.LEFT)) {
                    parentDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, Gravity.LEFT);
                } else {
                    parentDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
                    if(parentDrawer.isDrawerVisible(Gravity.LEFT)) parentDrawer.closeDrawer(Gravity.LEFT);
                }
                if(parentDrawer.isDrawerOpen(Gravity.RIGHT)) {
                    parentDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, Gravity.RIGHT);
                } else {
                    parentDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
                    if(parentDrawer.isDrawerVisible(Gravity.RIGHT)) {
                        parentDrawer.closeDrawer(Gravity.RIGHT);
                    }
                }
            }
            parent = parent.getParent();
        }
    }

    void stop() {
        if(parentSwipe != null) {
            parentSwipe.setEnabled(parentSwipeEnabled);
            parentSwipe = null;
        }
        if(parentDrawer != null) {
            parentDrawer.setDrawerLockMode(parentDrawerLocks[0], Gravity.LEFT);
            parentDrawer.setDrawerLockMode(parentDrawerLocks[1], Gravity.RIGHT);
            parentDrawer = null;
            parentDrawerLocks = null;
        }
    }

}
