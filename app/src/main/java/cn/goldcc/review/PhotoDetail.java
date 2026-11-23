package cn.goldcc.review;

import android.content.Context;
import android.graphics.*;
import android.view.*;

final class PhotoDetail extends View {
    private final Bitmap image;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector pinch;
    private final GestureDetector drag;
    private float zoom = 1, offsetX, offsetY;

    PhotoDetail(Context context, Bitmap image) {
        super(context);
        this.image = image;
        pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector event) {
                float next = Math.max(1, Math.min(10, zoom * event.getScaleFactor()));
                float factor = next / zoom;
                offsetX = event.getFocusX() - (event.getFocusX() - offsetX) * factor;
                offsetY = event.getFocusY() - (event.getFocusY() - offsetY) * factor;
                zoom = next;
                constrain();
                return true;
            }
        });
        drag = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onScroll(MotionEvent first, MotionEvent last, float dx, float dy) {
                if (!pinch.isInProgress()) { offsetX -= dx; offsetY -= dy; constrain(); }
                return true;
            }
        });
    }
    private float scale() {
        return zoom * Math.min((float)getWidth()/image.getWidth(), (float)getHeight()/image.getHeight());
    }
    private static float bound(float offset, float space) {
        return space > 0 ? space/2 : Math.min(0, Math.max(space, offset));
    }
    private void constrain() {
        offsetX = bound(offsetX, getWidth() - image.getWidth()*scale());
        offsetY = bound(offsetY, getHeight() - image.getHeight()*scale());
        invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) { constrain(); }
    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xff15231c);
        canvas.drawBitmap(image,null,new RectF(offsetX,offsetY,offsetX+image.getWidth()*scale(),offsetY+image.getHeight()*scale()),paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        pinch.onTouchEvent(event); drag.onTouchEvent(event); return true;
    }
}