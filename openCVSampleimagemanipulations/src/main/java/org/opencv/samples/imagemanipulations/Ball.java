package org.opencv.samples.imagemanipulations;

/**
 * Created by yuya on 2017/01/21.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class Ball extends View {
    int x, y, radius;
    Paint paint;

    public Ball(Context context) {
        super(context);
        radius = 30;
        x = y = 0;
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
    }

    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawCircle(x, y, radius, paint);
    }
}
