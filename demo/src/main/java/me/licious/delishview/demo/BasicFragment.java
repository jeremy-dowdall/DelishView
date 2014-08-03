package me.licious.delishview.demo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import me.licious.view.DelishView;
import me.licious.view.DelishView.OnDropAdapter;

public class BasicFragment extends Fragment {

    private DelishView delishView;
    private List<String> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_basic, container, false);

        items = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            items.add("Item - " + i);
        }

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
