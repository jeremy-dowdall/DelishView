package me.licious.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import java.util.List;

public interface HeaderAdapter extends ListAdapter {

    int getHeaderPosition(int position);

    List<Integer> getHeaderPositions();

    View getHeaderView(int position, ViewGroup parent);

}
