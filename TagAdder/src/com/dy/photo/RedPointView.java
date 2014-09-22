package com.dy.photo;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RedPointView extends View
// implements OnGestureListener
{

	// 粘贴
	static final int NONE = 0;
	// 拖动中
	static final int DRAG = 1;
	// 缩放中
	static final int ZOOM = 2;
	// 放大ing
	static final int BIGGER = 3;
	// 缩小ing
	static final int SMALLER = 4;
	// 关闭缩放动画
	static final int OPENSCALE = 1;
	// 关闭移动动画
	static final int OPENTRANS = 2;
	// 当前的事件
	private int mode = NONE;
	// 两触点距离
	private float beforeLenght;
	// 两触点距离
	private float afterLenght;
	// 缩放的比例 X Y方向都是这个值 越大缩放的越快

	/* 处理拖动 变量 */
	private int start_x;
	private int start_y;
	private int stop_x;
	private int stop_y;

	float[] center;

	// matrix.getValues(matrixValues);

	// 粘贴end

	private String TAG = "RedPointView";
	int rawX;
	int rawY;
	Matrix transPic;
	Bitmap redPoint;
	Bitmap rawBitmap = null;
	Bitmap editedBitmap = null;
	Canvas cacheCanvas = null;
	float scale = 1;
	float initScale = 1;
	Context myContext;
	private final int RESOLUTION = 512;
	GestureDetector detector;
	float disScale;

	public RedPointView(Context context, AttributeSet attrs) {
		super(context, attrs);
		myContext = context;
		// 设定图片缓冲区
		editedBitmap = Bitmap.createBitmap(RESOLUTION, RESOLUTION,
				Config.ARGB_8888);
		// 设置缓冲区所用的画板
		cacheCanvas = new Canvas();
		// 设置抗锯齿
		cacheCanvas.setDrawFilter(new PaintFlagsDrawFilter(0,
				Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
		// detector = new GestureDetector(context, this);
		transPic = new Matrix();

		this.setOnTouchListener(new RedPointOnTouchListener());
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		// dy:将背景图设定为当前图片
		// 此处若传入一个不可修改的bitmap会报错
		// http://bbs.csdn.net/topics/370021698
		//Log.d(TAG, "绘制，调用onDraw");
		if (rawBitmap != null) {

			cacheCanvas.setBitmap(editedBitmap);
			cacheCanvas.drawColor(Color.BLACK);
			cacheCanvas.drawBitmap(rawBitmap, transPic, null);
			// 为背景图添加圆点
			Matrix transPoint = new Matrix();
			transPoint.setScale(0.75f, 0.75f);
			transPoint.postTranslate(325, -5);

			cacheCanvas.drawBitmap(redPoint, transPoint, null);

			Matrix viewMatrix = new Matrix();
			int disX = this.getWidth();
			disScale = disX / (float) RESOLUTION;
			viewMatrix.setScale(disScale, disScale);
			// 将缓冲区的图片放置于view中显示
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
					Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

			canvas.drawBitmap(editedBitmap, viewMatrix, null);
		}
	}

	public void setImageBitmap(Bitmap bm) {
		this.rawBitmap = bm;
		if (rawBitmap != null) {
			rawX = rawBitmap.getWidth();
			rawY = rawBitmap.getHeight();
			if (rawX > rawY) {
				initScale = ((float) RESOLUTION) / rawY;
			} else {
				initScale = ((float) RESOLUTION) / rawX;
			}
		}
		transPic.setScale(initScale, initScale);
		scale = initScale;
		invalidate();
	}

	public Bitmap getRedPoint() {
		return redPoint;
	}

	public void setRedPoint(Bitmap redPoint) {
		this.redPoint = redPoint;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return detector.onTouchEvent(event);

	}

	private final class RedPointOnTouchListener implements OnTouchListener {
		PointF firstPoint = new PointF();
		float newScale = 0;
		float[] matrixArray = new float[9];

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// 以下内容为粘贴
			case MotionEvent.ACTION_DOWN:
				firstPoint.set(event.getX(), event.getY());
				mode = DRAG;
				Log.d(TAG, "ACTION_DOWN  ACTION_DOWN,getRawX()" + stop_x
						+ "  getRawY()" + stop_y + "   getX()" + start_x
						+ "  getY()" + start_y);
				if (event.getPointerCount() == 2)
					beforeLenght = spacing(event);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				Log.d(TAG, "ACTION_DOWN  第二个手指落下");
				/** 下面三句用于计算缩放中心点位置 **/
				center = centerPostion(event);
				if (spacing(event) > 10f) {
					mode = ZOOM;
					beforeLenght = spacing(event);
				}
				break;
			case MotionEvent.ACTION_UP:
				mode = NONE;
				break;
			case MotionEvent.ACTION_POINTER_UP:
				mode = NONE;
				scale = newScale;
				break;
			case MotionEvent.ACTION_MOVE:

				if (mode == ZOOM) {
					//Log.d(TAG, "ACTION_MOVE ZOOM 开始处理缩放");
					if (spacing(event) > 10f) {
						afterLenght = spacing(event);
						newScale = (afterLenght / beforeLenght);
						transPic.getValues(matrixArray);
						Log.d(TAG, "transPic"+matrixArray);
						float newvalue = newScale * matrixArray[0];
						if (newvalue > initScale && newvalue < 1.5&&!isScaleOutOfBoundary(newScale)) {
							transPic.postScale(newScale, newScale, center[0]
									/ disScale, center[1] / disScale);
							beforeLenght = afterLenght;
						}
					}
					/* 处理拖动位移 */
				} else if (mode == DRAG) {
					Log.d(TAG, "ACTION_MOVE DRAG 开始处理单手拖动");
					float dx = event.getX() - firstPoint.x;
					float dy = event.getY() - firstPoint.y;
					float transx = testBoundaryX(dx);
					float transy = testBoundaryY(dy);
					transPic.postTranslate(transx, transy);
					firstPoint.set(event.getX(), event.getY());
				}

				break;
			}
			// TODO 把输出日志加上，检查调用了几次onDraw，为何每次的矩阵不一样，是否是检查到位zoom时候没把原先的post完成？
			invalidate();
			return true;
			/*
			 * case MotionEvent.ACTION_DOWN: firstPoint.set(event.getX(),
			 * event.getY()); break; case MotionEvent.ACTION_MOVE:
			 * if(event.getPointerCount()==1){ float dx = event.getX() -
			 * firstPoint.x; float dy = event.getY() - firstPoint.y;
			 * transPic.postTranslate(dx, dy); firstPoint.set(event.getX(),
			 * event.getY()); } else if(event.getPointerCount()==2){ Log.d(TAG,
			 * "双手在屏幕上移动，此时应缩放！"); } break; case MotionEvent.ACTION_UP: break;
			 * case MotionEvent.ACTION_POINTER_UP: Log.d(TAG, "第二个手指抬起来！");
			 * break; case MotionEvent.ACTION_POINTER_DOWN: //
			 * 如果已经有手指压在屏幕上，又有一个手指压在了屏幕上 Log.d(TAG, "第二个手指按下！"); break; }
			 * invalidate(); return true;
			 */
		}
		
		private boolean isScaleOutOfBoundary(float newScale) {
			// TODO 该方法用于判断缩放后图片是否完全显示。
			//通过缩放率，位移矩阵，原始图片高度等数据进行计算。
			transPic.getValues(matrixArray);
			//上边>0，越界
			if(matrixArray[5]*matrixArray[0]*newScale>0){
				Log.d(TAG, "上边界超出范围，无法继续拖动");
				return true;
			}
			//左边>0 越
			if(matrixArray[2]*matrixArray[0]*newScale>0){
				Log.d(TAG, "左边界超出范围，无法继续拖动");
				return true;
			}
			//下边<512 越界
			float bottom = (rawBitmap.getHeight()*matrixArray[0]+matrixArray[5])*newScale;
			if(bottom<RESOLUTION){
				Log.d(TAG, "下边界超出范围，无法继续拖动");
				return true;
			}
			//右边<512
			float right = (rawBitmap.getWidth()*matrixArray[0]+matrixArray[2])*newScale;
			if(right<RESOLUTION){
				Log.d(TAG, "右边界超出范围，无法继续拖动");
				return true;
			}
			return false;
		}

		private float testBoundaryY(float dy) {
			transPic.getValues(matrixArray);
			float bottom = rawBitmap.getHeight()*matrixArray[0]+matrixArray[5]+dy;
			if(matrixArray[5]+dy>0){
				Log.d(TAG, "上边界超出范围，无法继续拖动");
				return 0;
			}else if(bottom<RESOLUTION){
				Log.d(TAG, "下边界超出范围，无法继续拖动");
				return 0;
			}
			return dy;
		}

		private float testBoundaryX(float dx) {
			transPic.getValues(matrixArray);
			float right = rawBitmap.getWidth()*matrixArray[0]+matrixArray[2]+dx;
			if(matrixArray[2]+dx>0){
				Log.d(TAG, "左边界超出范围，无法继续拖动");
				return 0;
			
			}else if(right<RESOLUTION){
				Log.d(TAG, "右边界超出范围，无法继续拖动");
				return 0;
			}
			return dx;
		}

	
		
		
		

	}

	// 计算两点距离
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * 计算两点间的中心点
	 */
	private float[] centerPostion(MotionEvent event) {
		float[] center = new float[2];
		float x0 = event.getX(0);
		float y0 = event.getY(0);
		float x1 = event.getX(1);
		float y1 = event.getY(1);
		/* x,y分别的距离 */
		center[0] = (x1 + x0) / 2;
		center[1] = (y1 - y0) / 2;
		return center;
	}

	public void reset() {
		// TODO Auto-generated method stub
		transPic.setScale(initScale, initScale);
		scale = initScale;
		invalidate();
	}

}
