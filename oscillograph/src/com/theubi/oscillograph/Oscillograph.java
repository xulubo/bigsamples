package com.theubi.oscillograph;

import java.util.ArrayList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.view.SurfaceView;

public class Oscillograph {
	private ArrayList<short[]> mInBuf = new ArrayList<short[]>();
	private boolean mIsRecording = false;

	public int mRateX = 4;
	public int mRateY = 4;
	public int mBaseLine = 0;

	public void initOscilloscope(int rateX, int rateY, int baseLine) {
		this.mRateX = rateX;
		this.mRateY = rateY;
		this.mBaseLine = baseLine;
	}

	/**
	 * @param recBufSize
	 *            AudioRecord的MinBufferSize
	 */
	public void Start(AudioRecord audioRecord, int recBufSize, SurfaceView sfv,
			Paint mPaint) {
		mIsRecording = true;
		new RecordThread(audioRecord, recBufSize).start();// 开始录制线程
		new DrawThread(sfv, mPaint).start();// 开始绘制线程
	}

	/**
	 * 停止
	 */
	public void Stop() {
		mIsRecording = false;
		mInBuf.clear();// 清除
	}

	/**
	 * 负责从MIC保存数据到inBuf
	 * 
	 * @author GV
	 * 
	 */
	class RecordThread extends Thread {
		private int recBufSize;
		private AudioRecord audioRecord;

		public RecordThread(AudioRecord audioRecord, int recBufSize) {
			this.audioRecord = audioRecord;
			this.recBufSize = recBufSize;
		}

		public void run() {
			try {
				short[] buffer = new short[recBufSize];
				audioRecord.startRecording();// 开始录制
				while (mIsRecording) {
					// 从MIC保存数据到缓冲区
					int bufferReadResult = audioRecord.read(buffer, 0,
							recBufSize);
					short[] tmpBuf = new short[bufferReadResult / mRateX];
					for (int i = 0, ii = 0; i < tmpBuf.length; i++, ii = i
							* mRateX) {
						tmpBuf[i] = buffer[ii];
					}
					synchronized (mInBuf) {//
						mInBuf.add(tmpBuf);// 添加数据
					}
				}
				audioRecord.stop();
			} catch (Throwable t) {
			}
		}
	};

	/**
	 * 负责绘制inBuf中的数据
	 * 
	 * @author GV
	 * 
	 */
	class DrawThread extends Thread {
		private int oldX = 0;// 上次绘制的X坐标
		private int oldY = 0;// 上次绘制的Y坐标
		private SurfaceView sfv;// 画板
		private int X_index = 0;// 当前画图所在屏幕X轴的坐标
		private Paint mPaint;// 画笔

		public DrawThread(SurfaceView sfv, Paint mPaint) {
			this.sfv = sfv;
			this.mPaint = mPaint;
		}

		public void run() {
			while (mIsRecording) {
				ArrayList<short[]> buf = new ArrayList<short[]>();
				synchronized (mInBuf) {
					if (mInBuf.size() == 0)
						continue;
					buf = (ArrayList<short[]>) mInBuf.clone();// 保存
					mInBuf.clear();// 清除
				}
				for (int i = 0; i < buf.size(); i++) {
					short[] tmpBuf = buf.get(i);
					SimpleDraw(X_index, tmpBuf, mRateY, mBaseLine);// 把缓冲区数据画出来
					X_index = X_index + tmpBuf.length;
					if (X_index > sfv.getWidth()) {
						X_index = 0;
					}
				}
			}
		}

		/**
		 * @param start
		 *            start point of X axis
		 * @param buffer
		 * @param rate
		 *            scale rate to compress Y axis
		 * @param baseLine
		 *            baseline of Y axis
		 */
		void SimpleDraw(int start, short[] buffer, int rate, int baseLine) {
			if (start == 0)
				oldX = 0;
			Canvas canvas = sfv.getHolder().lockCanvas(
					new Rect(start, 0, start + buffer.length, sfv.getHeight()));// 关键:获取画布
			canvas.drawColor(Color.BLACK);
			int y;
			for (int i = 0; i < buffer.length; i++) {
				int x = i + start;
				y = buffer[i] / rate + baseLine;
				canvas.drawLine(oldX, oldY, x, y, mPaint);
				oldX = x;
				oldY = y;
			}
			sfv.getHolder().unlockCanvasAndPost(canvas);
		}
	}
}