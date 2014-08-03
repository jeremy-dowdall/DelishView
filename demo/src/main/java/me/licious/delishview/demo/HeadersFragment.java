package me.licious.delishview.demo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import me.licious.view.DelishView;

public class HeadersFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_headers, container, false);

        List<Object> elements = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            int headerPosition = elements.size();
            elements.add("Header - " + i);
            for(int j = 0; j < 10; j++) {
                elements.add(new Item("Item - " + i + " : " + j, headerPosition));
            }
        }

        DelishView delishView = (DelishView) view.findViewById(R.id.delish);
        delishView.setAdapter(new HeadersAdapter(inflater, elements));

        delishView.addHeaderView(inflater.inflate(R.layout.basic_header, delishView, false));

        return view;
    }


    public static class Item {

        public final String label;
        public final int header;

        public Item(String label, int header) {
            this.label = label;
            this.header = header;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}
