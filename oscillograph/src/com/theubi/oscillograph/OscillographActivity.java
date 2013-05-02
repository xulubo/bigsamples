package com.theubi.oscillograph;  
import android.app.Activity;  
import android.graphics.Color;  
import android.graphics.Paint;  
import android.media.AudioFormat;  
import android.media.AudioRecord;  
import android.media.MediaRecorder;  
import android.os.Bundle;  
import android.view.MotionEvent;  
import android.view.SurfaceView;  
import android.view.View;  
import android.view.View.OnTouchListener;  
import android.widget.Button;  
import android.widget.ZoomControls;  
public class OscillographActivity extends Activity {  
    /** Called when the activity is first created. */  
    Button btnStart,btnExit;  
    SurfaceView sfv;  
    ZoomControls zctlX,zctlY;  
      
    Oscillograph clsOscilloscope=new Oscillograph();  
      
    static final int frequency = 8000;//分辨率  
    static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;  
    static final int xMax = 16;//X轴缩小比例最大值,X轴数据量巨大，容易产生刷新延时   
    static final int xMin = 8;//X轴缩小比例最小值  
    static final int yMax = 10;//Y轴缩小比例最大值  
    static final int yMin = 1;//Y轴缩小比例最小值  
      
    int recBufSize;//录音最小buffer大小  
    AudioRecord audioRecord;  
    Paint mPaint;  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.oscillograph);  
        //录音组件  
        recBufSize = AudioRecord.getMinBufferSize(frequency,  
                channelConfiguration, audioEncoding);  
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,  
                channelConfiguration, audioEncoding, recBufSize);  
        //按键  
        btnStart = (Button) this.findViewById(R.id.btnStart);  
        btnStart.setOnClickListener(new ClickEvent());  
        btnExit = (Button) this.findViewById(R.id.btnExit);  
        btnExit.setOnClickListener(new ClickEvent());  
        //画板和画笔  
        sfv = (SurfaceView) this.findViewById(R.id.SurfaceView01);   
        sfv.setOnTouchListener(new TouchEvent());  
        mPaint = new Paint();    
        mPaint.setColor(Color.GREEN);// 画笔为绿色    
        mPaint.setStrokeWidth(1);// 设置画笔粗细   
        //示波器类库  
        clsOscilloscope.initOscilloscope(xMax/2, yMax/2, sfv.getHeight()/2);  
          
        //缩放控件，X轴的数据缩小的比率高些  
        zctlX = (ZoomControls)this.findViewById(R.id.zctlX);  
        zctlX.setOnZoomInClickListener(new View.OnClickListener() {  
            @Override  
            public void onClick(View v) {  
                if(clsOscilloscope.mRateX>xMin)  
                    clsOscilloscope.mRateX--;  
                setTitle("X轴缩小"+String.valueOf(clsOscilloscope.mRateX)+"倍"  
                        +","+"Y轴缩小"+String.valueOf(clsOscilloscope.mRateY)+"倍");  
            }  
        });  
        zctlX.setOnZoomOutClickListener(new View.OnClickListener() {  
            @Override  
            public void onClick(View v) {  
                if(clsOscilloscope.mRateX<xMax)  
                    clsOscilloscope.mRateX++;      
                setTitle("X轴缩小"+String.valueOf(clsOscilloscope.mRateX)+"倍"  
                        +","+"Y轴缩小"+String.valueOf(clsOscilloscope.mRateY)+"倍");  
            }  
        });  
        zctlY = (ZoomControls)this.findViewById(R.id.zctlY);  
        zctlY.setOnZoomInClickListener(new View.OnClickListener() {  
            @Override  
            public void onClick(View v) {  
                if(clsOscilloscope.mRateY>yMin)  
                    clsOscilloscope.mRateY--;  
                setTitle("X轴缩小"+String.valueOf(clsOscilloscope.mRateX)+"倍"  
                        +","+"Y轴缩小"+String.valueOf(clsOscilloscope.mRateY)+"倍");  
            }  
        });  
          
        zctlY.setOnZoomOutClickListener(new View.OnClickListener() {  
            @Override  
            public void onClick(View v) {  
                if(clsOscilloscope.mRateY<yMax)  
                    clsOscilloscope.mRateY++;      
                setTitle("X轴缩小"+String.valueOf(clsOscilloscope.mRateX)+"倍"  
                        +","+"Y轴缩小"+String.valueOf(clsOscilloscope.mRateY)+"倍");  
            }  
        });  
    }  
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        android.os.Process.killProcess(android.os.Process.myPid());  
    }  
      
    /** 
     * 按键事件处理 
     * @author GV 
     * 
     */  
    class ClickEvent implements View.OnClickListener {  
        @Override  
        public void onClick(View v) {  
            if (v == btnStart) {  
                clsOscilloscope.mBaseLine=sfv.getHeight()/2;  
                clsOscilloscope.Start(audioRecord,recBufSize,sfv,mPaint);  
            } else if (v == btnExit) {  
                clsOscilloscope.Stop();  
            }  
        }  
    }  
    /** 
     * 触摸屏动态设置波形图基线 
     * @author GV 
     * 
     */  
    class TouchEvent implements OnTouchListener{  
        @Override  
        public boolean onTouch(View v, MotionEvent event) {  
            clsOscilloscope.mBaseLine=(int)event.getY();  
            return true;  
        }  
          
    }  
}  