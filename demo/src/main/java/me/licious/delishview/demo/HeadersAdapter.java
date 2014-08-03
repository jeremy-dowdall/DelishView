package me.licious.delishview.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.licious.delishview.demo.HeadersFragment.Item;
import me.licious.view.DragAdapter;
import me.licious.view.HeaderAdapter;

public class HeadersAdapter extends BaseAdapter implements DragAdapter, HeaderAdapter {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;


    private final LayoutInflater inflater;
    private final List<Object> elements;

    public HeadersAdapter(LayoutInflater inflater, List<Object> elements) {
        this.inflater = inflater;
        this.elements = elements;
    }

    @Override
    public int getCount() {
        return elements.size();
    }

    @Override
    public Object getItem(int position) {
        return elements.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return (getItem(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(getItemViewType(position) == TYPE_HEADER) {
            View view = (convertView != null) ? convertView : inflater.inflate(R.layout.simple_header, parent, false);
            ((TextView) view).setText(getItem(position).toString());
            return view;
        } else {
            View view = (convertView != null) ? convertView : inflater.inflate(R.layout.simple_item, parent, false);
            ((TextView) view).setText(getItem(position).toString());
            return view;
        }
    }

    @Override
    public View getDragView(int position, ViewGroup parent) {
        View view = inflater.inflate(R.layout.simple_drag_item, parent, false);
        ((TextView) view).setText(getItem(position).toString());
        return view;
    }

    @Override
    public int getHeaderPosition(int position) {
        Object element = getItem(position);
        if(element instanceof String) {
            return position;
        }
        return ((Item) element).header;
    }

    @Override
    public List<Integer> getHeaderPositions() {
        List<Integer> positions = new ArrayList<>();
        for(int i = 0; i < elements.size(); i++) {
            if(elements.get(i) instanceof String) {
                positions.add(i);
            }
        }
        return positions;
    }

    @Override
    public View getHeaderView(int position, ViewGroup parent) {
        View view = inflater.inflate(R.layout.simple_sticky_header, parent, false);
        TextView textView = (TextView) view.findViewById(R.id.label);
        textView.setText(getItem(position).toString());
        return view;
    }
}
