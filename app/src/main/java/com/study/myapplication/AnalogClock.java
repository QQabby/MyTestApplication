package com.study.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;

import java.util.TimeZone;


/**
 * Created by xuqianqian on 2016/8/22.
 */

public class AnalogClock extends View {


    //存放三张资源
    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mDial;

    private Time mCalendar;

    //用来记录表盘图片的宽和高，以便帮助我们在onMeasure中确定View的大小，毕竟我们的View中最大的一个Drawable就是它了
    private int mDialWidth;
    private int mDialHeight;

    //用来跟踪我们的View的尺寸的变化
    //当尺寸发生变化时，我们在绘制自己时要进行适当的缩放。
    private boolean mChanged;

    private float mMinutes;
    private float mHour;
    //用来记录View是否被加入到Window中，我们在View attached到
    //window时注册监听器，监听时间的变更，并根据时间的变更，改变自己的绘制
    //当View从Window中剥离时，解除注册，因为我们不需要再监听时间变更了，没人能看到我们的View了
    private boolean mAttached;

    public AnalogClock(Context context) {
        super(context);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        Resources r = context.getResources();
        if(mDial == null){

            mDial = r.getDrawable(R.mipmap.clock_dial);
        }

        if(mHourHand == null){

            mHourHand = r.getDrawable(R.mipmap.clock_hand_hour);
        }

        if(mMinuteHand == null){
//            mMinuteHand = context.getDrawable(R.mipmap.clock_hand_minute);

            mMinuteHand = r.getDrawable(R.mipmap.clock_hand_minute);
        }

        mCalendar = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();
    }

    //在该方法中，我们的View想要的尺寸当然就是与表盘一样大的尺寸，这样可以保证我们的View
    //有最佳的展示，可以如果如果ViewGroup给的尺寸比较小，我们就根据表盘图片的尺寸，进行适当的
    //按比例缩放。注意，这里我们没有直接使用ViewGroup给我们的较小的尺寸，而是对我们的表盘图片的
    //宽高进行相同比例的缩放后，设置的尺寸，这样的好处是，可以防止表盘图片绘制时的拉伸或者挤压变形

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if(widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth){
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if(heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight){
            vScale = (float) heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale,vScale);
        setMeasuredDimension(resolveSizeAndState((int)(mDialWidth*scale),widthMeasureSpec,0),
                        resolveSizeAndState((int)(mDialHeight * scale),heightMeasureSpec,0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    /**
     * 更新View中的mCalendar变量，自身的绘制
     */
    private void onTimeChanged(){

        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;

    }

    //定义一个广播接收器，接受系统发出的时间变化广播，然后更新该View的mCalendar，
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(Intent.ACTION_TIME_CHANGED)){

                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }
            //进行时间的更新
            onTimeChanged();
            //重绘View
            invalidate();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(!mAttached){
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver,filter);
        }

        mCalendar = new Time();
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mAttached){
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //当View尺寸变化后，我们用changed变量记录下来，同时回复mChanged为false，以便
        //继续监听View的尺寸变化
        boolean changed = mChanged;
        if(changed){
            mChanged = false;
        }

        int availableWidth = getRight() - getLeft();
        int availableHeight = getBottom() - getTop();

        int x = availableWidth / 2;
        int y = availableHeight / 2;
        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();
        boolean scaled = false;
        if(availableWidth < w || availableHeight < h){
            scaled = true;
            float scale = Math.min((float)availableWidth/(float)w
                    ,(float)availableHeight/(float)h);
            canvas.save();
            canvas.scale(scale,scale,x,y);
        }

        if(changed){
            dial.setBounds(x-(w/2),h-(h/2),x+(w/2),y+(h/2));
        }
        dial.draw(canvas);
        canvas.save();

        /*根据小时数，以点(x,y)为中心旋转坐标系。
            如果你对来回旋转的坐标系感到头晕，摸不着头脑，
            建议你看一下**徐宜生**《安卓群英传》中讲解2D绘图部分中的Canvas一节。*/
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        //同样，根据变化重新设置时针的Bounds
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            /* 仔细体会这里设置的Bounds，我们所画出的矩形，
                同样是以(x,y)为中心的
                矩形，时针图片放入该矩形后，时针的根部刚好在点(x,y)处，
                因为我们之前做时针图片时，
                已经让图片中的时针根部在图片的中心位置了，
                虽然，看起来浪费了一部分图片空间（就是时针下半部分是空白的），
                但却换来了建模的简单性，还是很值的。*/
            hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        hourHand.draw(canvas);
        canvas.restore();
        canvas.save();
        //根据分针旋转坐标系
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        minuteHand.draw(canvas);
        canvas.restore();
        //最后，我们把缩放的坐标系复原。
        if (scaled) {
            canvas.restore();
        }
    }
}
