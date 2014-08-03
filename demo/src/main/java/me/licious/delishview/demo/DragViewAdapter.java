package me.licious.delishview.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import me.licious.view.DragAdapter;

public class DragViewAdapter extends BaseAdapter implements DragAdapter {

    private final LayoutInflater inflater;
    private final List<String> elements;

    public DragViewAdapter(LayoutInflater inflater, List<String> elements) {
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
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = (convertView != null) ? convertView : inflater.inflate(R.layout.simple_item, parent, false);
        ((TextView) view).setText("Item " + position);
        return view;
    }

    @Override
    public View getDragView(int position, ViewGroup parent) {
        View view = inflater.inflate(R.layout.simple_drag_item, parent, false);
        ((TextView) view).setText("Item " + position);
        return view;
    }
}
