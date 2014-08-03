package me.licious.view;

import android.graphics.Canvas;
import android.view.View;

class DndView {

    int pos;
    float top;
    int offset;
    private int height;
    private final View v;
    float dragDelta, dragFromDelta, dragToDelta;
    private float posDelta, posFromDelta, posToDelta;

    DndView(int pos, View v) {
        this(pos, v, v.getTop());
    }

    DndView(int pos, View v, int top) {
        this(pos, v, top, 0, v.getHeight());
    }

    DndView(int pos, View v, int top, int offset, int height) {
        this.pos = pos;
        this.v = v;
        this.top = top;
        this.offset = offset;
        this.height = height;
    }

    boolean animate(int steps) {
        boolean drag = animateDrag(steps);
        boolean pos = animatePos(steps);
        return drag && pos;
    }

    private boolean animateDrag(int steps) {
        if(dragDelta != dragToDelta) {
            dragDelta += ((dragToDelta - dragFromDelta) / (float) steps);
            if(dragToDelta > dragFromDelta && dragDelta >= dragToDelta) {
                dragDelta = dragFromDelta = dragToDelta;
                return true;
            }
            if(dragToDelta < dragFromDelta && dragDelta <= dragToDelta) {
                dragDelta = dragFromDelta = dragToDelta;
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean animatePos(int steps) {
        if(posDelta != posToDelta) {
            posDelta += ((posToDelta - posFromDelta) / (float) steps);
            if(posToDelta > posFromDelta && posDelta >= posToDelta) {
                posDelta = posFromDelta = posToDelta;
                return true;
            }
            if(posToDelta < posFromDelta && posDelta <= posToDelta) {
                posDelta = posFromDelta = posToDelta;
                return true;
            }
            return false;
        }
        return true;
    }

    void draw(Canvas canvas) {
        int saveCount = canvas.save();
        canvas.translate(0, getTop());
        canvas.clipRect(0, 0, v.getWidth(), v.getHeight());
        int alpha = (int) (255 * v.getAlpha());
        if(alpha < 255) {
            canvas.saveLayerAlpha(0, 0, v.getWidth(), v.getHeight(), alpha, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        }
        v.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    int getBottom() {
        return (int) (top - offset + dragDelta + posDelta + height);
    }

    int getHeight() {
        return height;
    }

    int getMiddle() {
        return (int) (top + dragDelta + posDelta + (height / 2));
    }

    int getTop() {
        return (int) (top - offset + dragDelta + posDelta);
    }

    int getToBottom() {
        return (int) (top - offset + dragToDelta + posToDelta + height);
    }

    int getToTop() {
        return (int) (top - offset + dragToDelta + posToDelta);
    }

    void setDragDelta(float dy, boolean animate) {
        if(animate) {
            if(dy != dragToDelta) {
                dragFromDelta = dragToDelta;
                dragToDelta = dy;
            }
        } else {
            dragDelta = dragToDelta = dy;
        }
    }

    void setPosDelta(float dy, boolean animate) {
        if(animate) {
            if(dy != posToDelta) {
                posFromDelta = posToDelta;
                posToDelta = dy;
            }
        } else {
            posDelta = posToDelta = dy;
        }
    }
}
