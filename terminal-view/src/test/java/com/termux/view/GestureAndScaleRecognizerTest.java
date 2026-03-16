package com.termux.view;

import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GestureAndScaleRecognizerTest {

    private GestureAndScaleRecognizer recognizer;
    private RecordingListener listener;

    /** Minimal listener that records which callbacks were invoked. */
    static class RecordingListener implements GestureAndScaleRecognizer.Listener {
        boolean upCalled = false;
        boolean downCalled = false;

        @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
        @Override public boolean onDoubleTap(MotionEvent e) { return false; }
        @Override public boolean onScroll(MotionEvent e2, float dx, float dy) { return false; }
        @Override public boolean onFling(MotionEvent e, float vx, float vy) { return false; }
        @Override public boolean onScale(float focusX, float focusY, float scale) { return false; }
        @Override public boolean onDown(float x, float y) { downCalled = true; return false; }
        @Override public boolean onUp(MotionEvent e) { upCalled = true; return true; }
        @Override public void onLongPress(MotionEvent e) {}
    }

    @Before
    public void setUp() {
        listener = new RecordingListener();
        recognizer = new GestureAndScaleRecognizer(
                RuntimeEnvironment.getApplication(), listener);
    }

    /** ACTION_DOWN must reset isAfterLongPress so the next finger-lift triggers onUp. */
    @Test
    public void actionDownClearsIsAfterLongPress() {
        recognizer.isAfterLongPress = true;
        long t = android.os.SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 50f, 50f, 0);
        recognizer.onTouchEvent(down);
        down.recycle();

        assertFalse("ACTION_DOWN must clear isAfterLongPress", recognizer.isAfterLongPress);
    }

    /** A normal tap (down then up, no long press) must fire onUp. */
    @Test
    public void onUpFiredForNormalTap() {
        long t = android.os.SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(t, t,      MotionEvent.ACTION_DOWN, 50f, 50f, 0);
        MotionEvent up   = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP,   50f, 50f, 0);

        recognizer.onTouchEvent(down);
        recognizer.onTouchEvent(up);
        down.recycle();
        up.recycle();

        assertTrue("onUp must be called for a normal tap", listener.upCalled);
    }

    /**
     * After a long press, lifting the finger must NOT fire onUp.
     * This prevents accidental cursor movement when releasing after a long press
     * (e.g. in vim with mouse events enabled).
     */
    @Test
    public void onUpSkippedAfterLongPress() {
        // Directly set the flag as the gesture detector would after a long press fires
        recognizer.isAfterLongPress = true;

        long t = android.os.SystemClock.uptimeMillis();
        MotionEvent up = MotionEvent.obtain(t, t, MotionEvent.ACTION_UP, 50f, 50f, 0);
        recognizer.onTouchEvent(up);
        up.recycle();

        assertFalse("onUp must NOT be called after a long press", listener.upCalled);
    }

    /** isInProgress() must be false when no pinch-to-zoom gesture is active. */
    @Test
    public void isInProgressFalseWhenIdle() {
        assertFalse("Scale should not be in progress when no gesture is active",
                recognizer.isInProgress());
    }
}
