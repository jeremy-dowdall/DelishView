package me.licious.delishview.demo;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import me.licious.view.DelishView;
import me.licious.view.DelishView.OnDropAdapter;

public class SwipToRefreshFragment extends Fragment {

    private SwipeRefreshLayout swipe;
    private DelishView delishView;
    private List<String> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_swipe_to_refresh, container, false);

        items = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            items.add("Item - " + i);
        }

        swipe = (SwipeRefreshLayout) view.findViewById(R.id.sync_container);
        swipe.setColorScheme(
                android.R.color.holo_blue_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light,
                android.R.color.holo_red_light)
        ;
        swipe.setOnRefreshListener(new OnRefreshListener() {
            public void onRefresh() {
                swipe.postDelayed(new Runnable() {
                    public void run() {
                        swipe.setRefreshing(false);
                    }
                }, 1000);
            }
        });


        delishView = (DelishView) view.findViewById(R.id.delish);
        delishView.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, items));
        delishView.setOnDropListener(new OnDropAdapter() {
            public void onDropItem(int from, int to) {
                items.add(to, items.remove(from));
                delishView.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, items));
            }
        });

        delishView.addHeaderView(inflater.inflate(R.layout.basic_header, delishView, false));

        return view;
    }
}
