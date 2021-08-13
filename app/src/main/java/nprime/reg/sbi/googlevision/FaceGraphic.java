/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nprime.reg.sbi.googlevision;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private Rect rect = null;

    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;
    private Paint mBoxPaint;
    private volatile Face mFace;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[2];//COLOR_CHOICES[mCurrentColorIndex];

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        //postInvalidate();
    }

    Rect getFaceRect() {
        return rect;
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        try {
            Face face = mFace;
            if (face == null) {
                return;
            }
            float x = translateX(face.getPosition().x + face.getWidth() / 2);
            float y = translateY(face.getPosition().y + face.getHeight() / 2);
            // Draws a bounding box around the face.
            float xOffset = scaleX(face.getWidth() / 2.0f);
            float yOffset = scaleY(face.getHeight() / 2.0f);
            float left = x - xOffset;
            float top = y - yOffset;
            float right = x + xOffset;
            float bottom = y + yOffset;
            //canvas.drawRect(left, top, right, bottom, mBoxPaint);
            int radius = (int)((3/2) * xOffset);
            canvas.drawCircle(x, y, radius, mBoxPaint);
            rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
        } catch (Exception ex) {ex.printStackTrace();}
    }
}
