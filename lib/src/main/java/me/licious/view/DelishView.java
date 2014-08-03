package me.licious.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import me.licious.delishview.R;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class DelishView extends ListView {

    public interface OnDropListener {
        void onDropHeader(int from, int to);
        void onDropItem(int from, int to);
    }
    public static class OnDropAdapter implements OnDropListener {
        public void onDropHeader(int from, int to) {
            // subclasses to implement if necessary
        }
        public void onDropItem(int from, int to) {
            // subclasses to implement if necessary
        }
    }


    private static final long MIN_ELAPSED_TIME = 100;


    private /* final */ int slop;
    private /* final */ Handler longPressHandler;
    private /* final */ Runnable longPressRunnable;
    private /* final */ int longPressTimeout;
    private final Rect dividerBounds = new Rect();

    private ListAdapter adapter;
    private DragAdapter dragAdapter;
    private HeaderAdapter headerAdapter;

    private DndView header;
    private List<Integer> headers;

    private long downT;
    private int downX,downY;
    private boolean dragging; // should only be used in the main (UI) thread

    private int sourcePosition = -1; // should always be relative to adapter, not ListView
    private int targetPosition = -1; // should always be relative to adapter, not ListView

    private int h;
    private int dy;
    private float dyUP, dyDN;
    private float slide;

    private View dragHandle;
    private DndView dragView;

    private List<DndView> views;

    private Runnable runner;
    private boolean atTop, atBottom;

    private ParentBlocker blocker;
    private boolean reorderable = true;

    private boolean activateOnDragHandle = false;
    private boolean activateOnLongPress = true;

    private OnDropListener onDropListener;


    public DelishView(Context context) {
        super(context);
        init(context);
    }

    public DelishView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DelishView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        longPressHandler = new Handler(Looper.getMainLooper());
        longPressRunnable = new Runnable() {
            public void run() {
                dragHandle.setPressed(false);
                if(!dragging && startDrag(downY)) {
                    dragging = true;
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
            }
        };
        longPressTimeout = max(ViewConfiguration.getLongPressTimeout() / 2, 250);

        blocker = new ParentBlocker(this);

        setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(activateOnLongPress) {
                    sourcePosition = position;
                    dragHandle = view;
                    if(startDrag(downY)) {
                        return dragging = true;
                    }
                }
                return false;
            }
        });
    }


    @Override
    public void setAdapter(ListAdapter adapter) {
        this.adapter = adapter;
        if(adapter instanceof DragAdapter) {
            dragAdapter = (DragAdapter) adapter;
        }
        if(adapter instanceof HeaderAdapter) {
            headerAdapter = (HeaderAdapter) adapter;
        }
        super.setAdapter(adapter);
    }

    public void setOnDropListener(OnDropListener onDropListener) {
        this.onDropListener = onDropListener;
    }


    private int hitCheck(ViewGroup g, int x, int y) {
        for(int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if(c.getLeft() <= x && c.getTop() <= y && c.getRight() > x && c.getBottom() > y) {
                if(c.getId() == R.id.delish_drag_handle) { dragHandle = c; return getPositionForView(c); }
                else if(c instanceof ViewGroup)          { return hitCheck((ViewGroup) c, x - c.getLeft(), y - c.getTop()); }
            }
        }
        return -1;
    }

    private boolean isClick(MotionEvent event) {
        if(Math.abs(downX - (int) event.getX()) > slop) return false;
        if(Math.abs(downY - (int) event.getY()) > slop) return false;
        return true;
    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        updateStickyHeader();
    }

    @Override
    public int getFirstVisiblePosition() {
        if(views != null) return views.get(0).pos;
        return super.getFirstVisiblePosition();
    }

    private int getFirstVisibleTop() {
        if(views != null) return views.get(0).getTop();
        if(getChildCount() > 0) return getChildAt(0).getTop();
        return 0;
    }

    private void updateStickyHeader() {
        if(headerAdapter != null && headers == null && getCount() > 0) {
            int firstPosition = getFirstVisiblePosition() - getHeaderViewsCount();
            if(firstPosition > 0 || (firstPosition == 0 && getFirstVisibleTop() < getPaddingTop())) {
                int headerPosition = headerAdapter.getHeaderPosition(firstPosition);
                if(headerPosition >= 0) {
                    if(header == null || header.pos != headerPosition) {
                        header = new DndView(headerPosition, getStickyHeaderView(headerPosition), 0);
                    }
                    header.top = getStickyHeaderTop(firstPosition);
                    return;
                }
            }
        }
        header = null;
    }

    private int getStickyHeaderTop(int firstPosition) {
        int top = getPaddingTop();
        int nextPosition = firstPosition + 1;
        if(dragView != null && nextPosition == dragView.pos) {
            nextPosition++;
        }
        if(nextPosition < headerAdapter.getCount()) {
            int nextHeaderPosition = headerAdapter.getHeaderPosition(nextPosition);
            if(nextHeaderPosition != header.pos) {
                top = (views != null) ? views.get(0).getTop() : getChildAt(0).getTop();
            }
        }
//        if(dragView != null) {
//            top = min(top, dragView.getTop() + dragView.offset - header.getHeight() - getPaddingTop());
//        }
        return top;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if(views == null) {
            super.dispatchDraw(canvas);
            if(header != null && headers == null) {
                int saveCount = canvas.save();
                canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
                canvas.translate(getPaddingLeft(), 0);
                header.draw(canvas);
                canvas.restoreToCount(saveCount);
            }
        } else {
            int saveCount = canvas.save();
            canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
            canvas.translate(getPaddingLeft(), 0);
            drawDividers(canvas);
            for(DndView v : views) {
                if(header == null || header.pos != v.pos) {
                    v.draw(canvas);
                }
            }
            if(header != null && headers == null) {
                header.draw(canvas);
            }
            canvas.restoreToCount(saveCount);
        }
        if(dragView != null) {
            // dragView is not adjusted for padding
            dragView.draw(canvas);
        }
    }

    private void drawDividers(Canvas canvas) {
        if(views.size() > 0) {
            Drawable divider = (getDividerHeight() > 0) ? getDivider() : null;
            if(divider != null) {
                dividerBounds.left = 0; // left padding handled already
                dividerBounds.right = getWidth() - getPaddingRight();
                for(int i = 0; i < views.size() - 1; i++) {
                    drawDividerBelow(canvas, divider, views.get(i));
                }
                for(int i = 0; i < views.size(); i++) {
                    DndView view = views.get(i);
                    if(view.getBottom() >= dragView.getBottom()) {
                        drawDividerAbove(canvas, divider, view);
                        return;
                    }
                }
                DndView last = views.get(views.size() - 1);
                if(dragView.getBottom() > last.getBottom()) { // dragView may have some transparency
                    drawDividerBelow(canvas, divider, last);
                }
            }
        }
    }

    private void drawDividerAbove(Canvas canvas, Drawable divider, DndView view) {
        if(slide != 0) { // dividers follow items if sliding, otherwise they're fixed
            dividerBounds.bottom = view.getTop();
        } else {
            dividerBounds.bottom = view.getToTop();
        }
        dividerBounds.top = dividerBounds.bottom - getDividerHeight();
        divider.setBounds(dividerBounds);
        divider.draw(canvas);
    }

    private void drawDividerBelow(Canvas canvas, Drawable divider, DndView view) {
        if(slide != 0) { // dividers follow items if sliding, otherwise they're fixed
            dividerBounds.top = view.getBottom();
        } else {
            dividerBounds.top = view.getToTop() + view.getHeight();
        }
        dividerBounds.bottom = dividerBounds.top + getDividerHeight();
        divider.setBounds(dividerBounds);
        divider.draw(canvas);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if(views == null) {
            return super.computeVerticalScrollOffset();
        } else {
            DndView dv = views.get(0);
            int height = dv.getHeight();
            if(height > 0) {
                int firstPosition = dv.pos;
                int top = dv.getTop();
                return Math.max(firstPosition * 100 - (top * 100) / height +
                        (int) ((float) getScrollY() / getHeight() * getCount() * 100), 0);
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return onDragInterceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    private boolean onDragInterceptTouchEvent(MotionEvent event) {
        if((sourcePosition == -1) && (event.getAction() == MotionEvent.ACTION_DOWN)) {
            downT = System.currentTimeMillis();
            downX = (int) event.getX();
            downY = (int) event.getY();
            if(activateOnDragHandle) {
                sourcePosition = hitCheck(this, downX, downY);
                if(sourcePosition != -1) {
                    dragHandle.setPressed(true);
                    longPressHandler.postDelayed(longPressRunnable, longPressTimeout);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return onDragTouchEvent(event) || super.onTouchEvent(event);
    }

    private boolean onDragTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return (sourcePosition != -1);
            case MotionEvent.ACTION_MOVE:
                if(sourcePosition != -1) {
                    int y = (int) event.getY();
                    if(dragging) {
                        onDrag(y);
                        return true;
                    } else {
                        if(Math.abs(downX - (int) event.getX()) > slop) { // we're dragging in the horizontal direction... run away! run away!
                            longPressHandler.removeCallbacks(longPressRunnable);
                            dragHandle.setPressed(false);
                            sourcePosition = -1;
                            return false;
                        }
                        if(Math.abs(downY - y) > slop) { // we're dragging in the vertical direction... good :)
                            longPressHandler.removeCallbacks(longPressRunnable);
                            dragHandle.setPressed(false);
                            long elapsedTime = System.currentTimeMillis() - downT;
                            if(elapsedTime > MIN_ELAPSED_TIME && startDrag(y)) {
                                dragging = true;
                                return true;
                            } else {
                                sourcePosition = -1; // or not... cancel the drag
                                return false;
                            }
                        }
                    }
                }
                return false;
            case MotionEvent.ACTION_UP:
                if(sourcePosition != -1) {
                    longPressHandler.removeCallbacks(longPressRunnable);
                    dragHandle.setPressed(false);
                    if(dragging) {
                        dragging = false;
                        stopDrag();
                        return true;
                    } else {
                        if(isClick(event)) dragHandle.performClick();
                        sourcePosition = -1;
                    }
                }
        }
        return false;
    }

    private void setDragDeltas(boolean animate) {
        int md = dragView.getMiddle();
        for(DndView v : views) {
            int mv = v.getMiddle();
            if(mv < md) v.setDragDelta(dyUP, animate);
            else        v.setDragDelta(dyDN, animate);
        }
    }

    private void setDragViewDelta() {
        int md = dragView.getMiddle();
        for(DndView v : views) {
            if(v.getMiddle() >= md) {
                dragView.setDragDelta((v.getTop() - h) - dragView.top, true);
                return;
            }
        }
        DndView v = views.get(views.size() - 1);
        dragView.setDragDelta(v.getBottom() - dragView.top, true);
    }

    private boolean startDrag(int y) {
//        TODO
//        if( ! adapter.isDndEnabled()) {
//            return false;
//        }

        targetPosition = -1;
        int headerViewsCount = getHeaderViewsCount();

        if(onDropListener == null) {
            return false;
        }
        if(getCount() < 2) {
            return false;
        }
        if(headerViewsCount > 0) {
            if(sourcePosition < headerViewsCount) {
                return false; // don't allow dragging a regular ListView header
            }
            sourcePosition -= headerViewsCount; // put sourcePosition in terms of adapter
        }
        if(headerAdapter != null) {
            int position = sourcePosition;
            int headerPosition = headerAdapter.getHeaderPosition(position);
            if(headerPosition == position) {
                headers = headerAdapter.getHeaderPositions();
                if(headers == null || headers.size() < 2) {
                    headers = null;
                    return false;
                }
            }
        }

        views = new ArrayList<DndView>(getChildCount() + 1);
        int firstPosition = super.getFirstVisiblePosition();
        for(int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int p = firstPosition + i - headerViewsCount; // relative to adapter (header rows will be negative)
            if(p == sourcePosition) {
                dragView = getDragView(p, v);
            }
            else if(headers == null) {
                views.add(new DndView(p, v));
            }
//            else if(p == headerAdapter.getHeaderPosition(p - getHeaderViewsCount())) {
                else if(headers.indexOf(p) != -1) {
                    views.add(new DndView(p, v));
                }
//            }
        }

        if(dragView == null) {
            Log.wtf("DND", "drag view not found at position: " + sourcePosition);
            views = null;
            return false;
        }

        h = dragView.getHeight() + getDividerHeight();
        dy = dragView.getMiddle() - y;
        dyUP = -h/2f; dyDN = h/2f;

        if(headers != null) {
            int delta = 0;
            DndView prev = dragView;
            for(int i = views.size() - 1; i >= 0; i--) {
                DndView view = views.get(i);
                if(view.pos < prev.pos) {
                    delta += (prev.top - (view.top + view.getHeight()) - getDividerHeight());
                    view.setPosDelta(delta, true);
                    prev = view;
                }
            }
            for(int i = headers.size() - 1; i >= 0; i--) {
                if((prev.getToBottom()) <= 0) {
                    break;
                }
                int position = headers.get(i);
                if(position < prev.pos) {
                    View v = getView(null, -1, position);
                    DndView view = new DndView(position, v, -v.getHeight());
                    delta += (prev.top - (view.top + view.getHeight()) - getDividerHeight());
                    view.setPosDelta(delta, true);
                    views.add(0, view);
                    prev = view;
                }
            }

            delta = 0;
            prev = dragView;
            for(int i = 0; i < views.size(); i++) {
                DndView view = views.get(i);
                if(view.pos > prev.pos) {
                    delta += ((prev.top + prev.getHeight()) - view.top + getDividerHeight());
                    view.setPosDelta(delta, true);
                    prev = view;
                }
            }
            for(int i = 0; i < headers.size(); i++) {
                if(prev.getToTop() >= getHeight()) {
                    break;
                }
                int position = headers.get(i);
                if(position > prev.pos) {
                    View v = getView(null, -1, position);
                    DndView view = new DndView(position, v, getHeight());
                    delta += ((prev.top + prev.getHeight()) - view.top + getDividerHeight());
                    view.setPosDelta(delta, true);
                    views.add(view);
                    prev = view;
                }
            }

            // reset each view position to use headerPositions
            for(int i = 0, j = 0; i < headers.size(); i++) {
                int position = headers.get(i);
                if(j < views.size()) {
                    DndView view = views.get(j);
                    if(position == view.pos) {
                        view.pos = i;
                        j++;
                    }
                }
                if(position == dragView.pos) {
                    dragView.pos = i;
                }
            }
        }

        for(DndView v : views) {
            if(v.pos < dragView.pos) v.top -= dyUP;
            if(v.pos > dragView.pos) v.top -= dyDN;
        }
        setDragDeltas(false);

        runner = new Runnable() {
            public void run() {
                if((targetPosition != -1) && dragView.animate(20)) {
                    postDragComplete();
                    return;
                }
                for(int i = 0; i < views.size(); i++) {
                    views.get(i).animate(20);
                }
                if(slide != 0) {
                    slide();
                }
                invalidate();
                if(views != null) {
                    postDelayed(runner, 10);
                }
            }
        };
        postDelayed(runner, 10);

        if(blocker != null) {
            blocker.start();
        }

        invalidate();

        return true;
    }

    private void onDrag(float moveY) {
        if(reorderable) {
            int top = getDragTop();
            int y = (int) moveY - (h/2) + dy;
            if(y <= (top + dragView.offset)) {
                if(atTop) {
                    return; // already at the top: exit
                }
                for(DndView v : views) {
                    v.setDragDelta(dyDN, true);
                }
                y = (top + dragView.offset);
                atTop = true;
            } else {
                atTop = false;
            }
            if(y+dragView.offset+dragView.getHeight() >= getHeight()) {
                if(atBottom) {
                    return; // already at the bottom: exit
                }
                for(DndView v : views) {
                    v.setDragDelta(dyUP, true);
                }
                y = getHeight() - dragView.offset - dragView.getHeight();
                atBottom = true;
            } else {
                atBottom = false;
            }

            dragView.top = y;

            if(atTop) {
                slide = 100;
            }
            else if(atBottom) {
                slide = -100;
            }
            else {
                float m = dragView.getMiddle();
                float i = getHeight() / 5;
                if(m > (4 * i)) slide = 100 * ((4*i)-m)/i; // bottom fifth of the view
                else if(m < i)  slide = 100 * ((i-m)/i);   // top fifth of the view
                else            slide = 0;
            }

            updateStickyHeader();
            setDragDeltas(true);
        } else {
            dragView.top = moveY - (h/2);
        }
    }

    // top of the drag range (dragView hard stops against this)
    private int getDragTop() {
        int firstPosition = getFirstVisiblePosition();
        if(firstPosition < getHeaderViewsCount()) {
            return getChildAt(getHeaderViewsCount() - 1).getBottom();
        } else {
            return 0;
        }
    }

    private DndView getDragView(int p, View v) {
        if(dragAdapter != null) {
            View dv = dragAdapter.getDragView(p, this);
            if(dv != null) {
                layoutView(dv, false);
                int offset = (dv.getHeight() - v.getHeight()) / 2;
                return new DndView(p, dv, v.getTop(), offset, v.getHeight());
            }
        }
        return new DndView(p, v);
    }

    private View getStickyHeaderView(int newPosition) {
        View v = headerAdapter.getHeaderView(newPosition, this);
        layoutView(v, true);
        return v;
    }

    private View getView(View oldView, int oldPosition, int newPosition) {
        View v;
        if(oldView == null || adapter.getItemViewType(oldPosition) != adapter.getItemViewType(newPosition)) {
            v = adapter.getView(newPosition, null, this);
        } else {
            v = adapter.getView(newPosition, oldView, this);
        }
        layoutView(v, true);
        return v;
    }

    private void layoutView(View view, boolean adjustForPadding) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        if(lp == null) {
            lp = (LayoutParams) generateDefaultLayoutParams();
        }

        int w = adjustForPadding ? (getWidth() - getPaddingLeft() - getPaddingRight()) : getWidth();
        int wspec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int hspec;
        if(lp.height > 0) {
            hspec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        } else {
            hspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(wspec, hspec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private int getFirstPosition() {
        if(dragView != null && dragView.pos == 0) {
            return 1;
        }
        return 0;
    }

    private int getLastPosition() {
        int count = (headers != null) ? headers.size() : adapter.getCount();
        if(dragView != null && dragView.pos == count - 1) {
            count--;
        }
        return count - 1;
    }

    private void slide() {
        if(slide < 0) { // at bottom, is there anything to slide up?
            DndView b = views.get(views.size() - 1);
            if(b.getBottom() < getHeight() && b.pos == getLastPosition()) {
                return; // nope - exit
            }
        }
        if(slide > 0) { // at top, is there anything to slide down?
            DndView t = views.get(0);
            if(t.getTop() >= 0 && t.pos <= getFirstPosition()) { // will < if there are ListView headers
                return; // nope - exit
            }
        }

        for(DndView v : views) {
            v.top += slide;
        }

        if(slide < 0) { // at bottom, sliding others up
            DndView t = views.get(0);
            if(t.getBottom() < 0) {
                views.remove(0);
            }
            DndView b = views.get(views.size() - 1);
            if(b.getBottom() < getHeight()) {
                int next = b.pos + 1;
                if(next == dragView.pos) next++;
                if(next <= getLastPosition()) {
                    int position = (headers != null) ? headers.get(next) : next;
                    View v = getView(null, -1, position);
                    DndView view = new DndView(next, v, (int) (b.top + b.getHeight() + getDividerHeight()));
                    view.setDragDelta(dyDN, false);
                    views.add(view);
                }
            }
            updateStickyHeader();
        }
        if(slide > 0) { // at top, sliding others down
            DndView b = views.get(views.size() - 1);
            if(b.getTop() > getHeight()) {
                views.remove(views.size() - 1);
            }
            DndView t = views.get(0);
            if(t.getTop() > 0) {
                int prev = t.pos - 1;
                if(prev == dragView.pos) prev--;
                if(prev >= getFirstPosition()) {
                    int position = (headers != null) ? headers.get(prev) : prev;
                    View v = getView(null, -1, position);
                    DndView view = new DndView(prev, v, (int) (t.top - v.getHeight() - getDividerHeight()));
                    view.setDragDelta(dyUP, false);
                    views.add(0, view);
                }
            }
        }

        setDragDeltas(true);
        awakenScrollBars();
    }

    private void stopDrag() {
        targetPosition = getTargetPosition();
        if(reorderable) setDragViewDelta();
        else dragComplete();
        if(blocker != null) {
            blocker.stop();
        }
    }

    private int getTargetPosition() {
        int p = -1;
        int md = dragView.getMiddle();
        for(DndView v : views) {
            if(v.getMiddle() >= md) {
                p = v.pos;
                break;
            }
        }
        if(p == -1) {
            if(headers == null) return min(views.get(views.size() - 1).pos + 1, getCount() - 1);
            else                return headers.size() - 1;
        }
        if(p > dragView.pos) {
            return p - 1;
        }
        return p;
    }

    private void postDragComplete() {
        post(new Runnable() {
            public void run() {
                dragComplete();
            }
        });
    }

    private void dragComplete() {
        DndView first = views.get(0);
        int topPosition = first.pos + getHeaderViewsCount();
        int topOffset = first.getTop() - getPaddingTop();

        for(int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.setAlpha(1);
            v.setTranslationY(0);
            v.setVisibility(View.VISIBLE);
        }

        slide = 0;
        dragView = null;
        views = null;
        h = 0;

        if(onDropListener != null) {
            if(headers == null) {
                onDropListener.onDropItem(sourcePosition, targetPosition);
                if(sourcePosition < topPosition) topPosition--;
            } else {
                // source is in full list positions, target is in header positions
                onDropListener.onDropHeader(headers.indexOf(sourcePosition), targetPosition);
                topPosition = sourcePosition;
                topOffset = 0;
            }
        }

        if(topPosition >= 0 && topPosition < getCount()) {
            setSelectionFromTop(topPosition, topOffset);
        }

        headers = null;
        sourcePosition = -1;
    }

}
