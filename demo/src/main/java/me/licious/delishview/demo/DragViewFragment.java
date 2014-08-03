package me.licious.delishview.demo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import me.licious.view.DelishView;

public class DragViewFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_dragview, container, false);

        List<String> items = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            items.add("Item - " + i);
        }

        DelishView delishView = (DelishView) view.findViewById(R.id.delish);
        delishView.setAdapter(new DragViewAdapter(inflater, items));

        return view;
    }
}
