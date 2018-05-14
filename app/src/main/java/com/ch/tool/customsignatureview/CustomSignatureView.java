package com.ch.tool.customsignatureview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


/**
 * Created by 今夜犬吠 on 2018/5/10.
 * 自定义签名控件
 */

public class CustomSignatureView extends View {

  /*屏幕像素密度-用于把dp转换成Px*/private float mDensity = getContext().getResources().getDisplayMetrics().density;

  /*签名-画笔*/private Paint mTextPaint;

  /*签名-字体色*/private int mTestColor = R.color.colorAccent;

  /*签名-画布背景色*/private int mCanvasColor = R.color.cardview_light_background;

  /*路径-笔画*/private Path mPath;

  /*是否识别文字*/private boolean mIsRecognizeText;

  /*签名文件名*/private String mSignatureFileName = "你的签名";

  /*上下文*/private Context mContext;

  public CustomSignatureView(Context context) {
    super(context);
    this.mContext = context;
    initVPaint();
    initPath();
  }


  public CustomSignatureView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
    initVPaint();
    initPath();
  }

  /*签名-画布*/private Canvas mSignatureCanvas;
  /*签名的图片*/private Bitmap mSignatureBitmap;
  /*画布中心-坐标点*/private PointF mCanvasCenterPointF = new PointF();

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    mCanvasCenterPointF.set(getWidth() / 2, getHeight() / 2);
    mSignatureBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
        Bitmap.Config.ARGB_8888);
    mSignatureCanvas = new Canvas(mSignatureBitmap);
    mSignatureCanvas.drawColor(ContextCompat.getColor(mContext, mCanvasColor));
  }

  /*变化矩阵*/private Matrix matrix;

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
    // canvas.drawBitmap(mSignatureBitmap,matrix,mTextPaint);
    canvas.drawBitmap(mSignatureBitmap, 0, 0, mTextPaint);
    canvas.drawPath(mPath, mTextPaint);
  }

  /*所有点的集合*/private ArrayList<PointF> mPointList;
  /*按下时的X坐标*/private float mPressingX;
  /*按下时的Y坐标*/private float mPressingY;
  /*距离中心位置-X*/private float mCentrifugalDistanceX;
  /*距离中心位置-Y*/private float mCentrifugalDistanceY;

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        mPressingX = event.getX();
        mPressingY = event.getY();
        mPath.moveTo(mPressingX, mPressingY);
        mPointList.add(new PointF(mPressingX, mPressingY));
        break;
      case MotionEvent.ACTION_MOVE:
        float mMoveX = event.getX();
        float mMoveY = event.getY();
        mPath.quadTo(mPressingX, mPressingY, (mMoveX + mPressingX) / 2, (mMoveY + mPressingY) / 2);
        mPressingX = mMoveX;
        mPressingY = mMoveY;
        mPointList.add(new PointF(mPressingX, mPressingY));
        break;
      case MotionEvent.ACTION_UP:
        // mSignatureCanvas.drawPath(mPath, mTextPaint);
        break;
      default:
        break;
    }
    invalidate();
    return true;
  }


  /**
   * 初始化画笔
   */
  private void initVPaint() {
    /**初始化*/mTextPaint = new Paint();
    /**设置画笔颜色*/mTextPaint.setColor(ContextCompat.getColor(mContext, mTestColor));
    /**设置画笔样式*/mTextPaint.setStyle(Paint.Style.STROKE);
    /**设置画笔粗细*/mTextPaint.setStrokeWidth(5);
    /**使用抗锯齿*/mTextPaint.setAntiAlias(true);
    /**使用防抖动*/mTextPaint.setDither(true);
    /**设置笔触样式-圆*/mTextPaint.setStrokeCap(Paint.Cap.ROUND);
    /**设置结合处为圆弧*/mTextPaint.setStrokeJoin(Paint.Join.ROUND);

    /**初始化点集合*/mPointList = new ArrayList<PointF>();

    /**初始化变换矩阵*/matrix = new Matrix();
  }

  /**
   * 初始化路径
   */
  private void initPath() {
    mPath = new Path();
  }


  /**
   * 清除画板
   */
  public void toolClearCanvas() {
    if (mPath != null) {
      mPath.reset();
     // mSignatureCanvas.drawColor(mCanvasColor, PorterDuff.Mode.CLEAR);
      invalidate();

    }
  }

  /**
   * 设置签名字体颜色
   */
  public CustomSignatureView tooSetTextColor(int mColor) {
    this.mTestColor = mColor;
    if (mTextPaint != null) {
      /**设置画笔颜色*/mTextPaint.setColor(ContextCompat.getColor(mContext, mColor));
    }
    return this;
  }


  /**
   * 设置画布背景色
   */
  public CustomSignatureView toolSetCanvasColor(int mColor) {
    this.mCanvasColor = mColor;
    invalidate();
    return this;
  }

  /**
   * 识别签名文字-设置回调函数
   */
  public CustomSignatureView toolRecognizeText(RecognizeTextBack mRecognizeTextBack) {
    this.mRecognizeTextBack = mRecognizeTextBack;
    mIsRecognizeText = true;
    toolLanguageLibrary();
    return this;
  }


  /**
   * 识别签名文字
   */
  public void toolRecognizeText(final File mFile) {
    if (mFile != null) {
            /*签名-识别工具*/
      TessBaseAPI mBaseAPI = new TessBaseAPI();
      mBaseAPI.init(Environment
          .getExternalStorageDirectory().getAbsolutePath()
          + "/SignatureView/", "chi_sim");
      mBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
      mBaseAPI.setImage(mFile);
      if (mRecognizeTextBack != null) {
        mRecognizeTextBack.toolToText(mBaseAPI.getUTF8Text());
      }
      mBaseAPI.end();
    }
  }

  /*中文语言库-路径*/private String mChPath = Environment
      .getExternalStorageDirectory().getAbsolutePath()
      + "/SignatureView/tessdata/";

  /**
   * 配置语言库-识别手写不好用
   */
  private void toolLanguageLibrary() {
    String mMapViewPath = "";
    InputStream mSignatureStream = null;
    try {
      /*assets有两个语言库-只引入中文*/
      mSignatureStream = mContext.getAssets().open("chi_sim.traineddata");

      File mSignatureFile = new File(mChPath + "chi_sim.traineddata");

      if (!mSignatureFile.exists()) {
        if (!new File(mSignatureFile.getParent()).exists()) {
          if (new File(mSignatureFile.getParent()).mkdirs()) {
            if (!mSignatureFile.exists()) {
              if (mSignatureFile.createNewFile()) {
                toolLanguageLibrary(mSignatureFile, mSignatureStream);
              }
            }
          }
        } else {
          if (!mSignatureFile.exists()) {
            if (mSignatureFile.createNewFile()) {
              toolLanguageLibrary(mSignatureFile, mSignatureStream);
            }
          }
        }
      } else {
        if (mSignatureFile != null && mSignatureStream != null) {
          toolLanguageLibrary(mSignatureFile, mSignatureStream);
        }
      }
    } catch (
        IOException e)

    {
      e.printStackTrace();
    } finally

    {
      if (mSignatureStream != null) {
        try {
          mSignatureStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }


  private void toolLanguageLibrary(File mSignatureFile, InputStream mSignatureStream) {
    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(mSignatureFile, false));
      byte data[] = new byte[1024];
      int len;
      while ((len = mSignatureStream.read(data, 0, 1024)) != -1) {
        os.write(data, 0, len);
      }
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 保存签名文件
   */
  public boolean toolSaveSignatureFile(File mFile) {
    mSignatureCanvas.drawColor(ContextCompat.getColor(mContext, mCanvasColor));
    if (mPointList != null && !mPointList.isEmpty()) {
      mPointList.clear();
    }
    setDrawingCacheEnabled(true);
    buildDrawingCache();
    Bitmap mBitmap = Bitmap.createBitmap(getDrawingCache());
    setDrawingCacheEnabled(false);
    if (mBitmap != null) {

      FileOutputStream out = null;
      try {
        if (!mFile.exists()) {
          if (!new File(mFile.getParent()).exists()) {
            if (new File(mFile.getParent()).mkdirs()) {
              if (!mFile.exists()) {
                if (mFile.createNewFile()) {
                  out = new FileOutputStream(mFile);
                }
              }
            }
          } else {
            if (!mFile.exists()) {
              if (mFile.createNewFile()) {
                out = new FileOutputStream(mFile);
              }
            }
          }
        } else {
          out = new FileOutputStream(mFile);
        }

      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return false;
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        if (null != out) {
          mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
          out.flush();
          out.close();
          return true;
        }
      } catch (IOException e) {
        return false;
      }
    }
    return false;
  }


  /*签名文件左下角-坐标点*/private PointF mLeftBottomPointF = new PointF();
  /*签名文件右上角-坐标点*/private PointF mRightTopPointF = new PointF();
  /*签名文件中心点-坐标点*/private PointF mCenterPointF = new PointF();

  /*前一点-X*/private float mBeforeX;
  /*前一点-Y*/private float mBeforeY;

  /**
   * 移动签名文件-至中心
   */
  public void toolMoveToCenter() {
    mPath.reset();
    if (mPointList != null && !mPointList.isEmpty()) {
      mLeftBottomPointF = mPointList.get(0);
      mRightTopPointF = mPointList.get(0);
      for (int i = 0; i < mPointList.size(); i++) {
        PointF mPointF = mPointList.get(i);
        if (mLeftBottomPointF.x > mPointList.get(i).x) {
          mLeftBottomPointF.set(mPointList.get(i).x, mLeftBottomPointF.y);
        }
        if (mLeftBottomPointF.y < mPointList.get(i).y) {
          mLeftBottomPointF.set(mLeftBottomPointF.x, mPointList.get(i).y);
        }

        if (mRightTopPointF.x < mPointList.get(i).x) {
          mRightTopPointF.set(mPointList.get(i).x, mRightTopPointF.y);
        }
        if (mRightTopPointF.y > mPointList.get(i).y) {
          mRightTopPointF.set(mRightTopPointF.x, mPointList.get(i).y);
        }
      }

        mCenterPointF.set((mLeftBottomPointF.x
                + (mRightTopPointF.x - mLeftBottomPointF.x) / 2),
            (mRightTopPointF.y + (mLeftBottomPointF.y - mRightTopPointF.y) / 2));

        mPath.moveTo(mPointList.get(0).x + (mCanvasCenterPointF.x - mCenterPointF.x)
            , mPointList.get(0).y + (mCanvasCenterPointF.y - mCenterPointF.y));
        mBeforeX = mPointList.get(0).x + (mCanvasCenterPointF.x - mCenterPointF.x);
        mBeforeY = mPointList.get(0).y + (mCanvasCenterPointF.y - mCenterPointF.y);

        for (int i = 0; i < mPointList.size(); i++) {
          mPath.quadTo(mPointList.get(i).x + (mCanvasCenterPointF.x - mCenterPointF.x)
              , mPointList.get(i).y + (mCanvasCenterPointF.y - mCenterPointF.y)
              , (mPointList.get(i).x + (mCanvasCenterPointF.x - mCenterPointF.x) + mBeforeX) / 2
              , (mPointList.get(i).y + (mCanvasCenterPointF.y - mCenterPointF.y) + mBeforeY) / 2);

          mBeforeX = mPointList.get(i).x + (mCanvasCenterPointF.x - mCenterPointF.x);
          mBeforeY = mPointList.get(i).y + (mCanvasCenterPointF.y - mCenterPointF.y);
        }

        invalidate();
    }
  }

  private RecognizeTextBack mRecognizeTextBack;

  /**
   * 识别文字回调
   */
  public interface RecognizeTextBack {
    void toolToText(String mText);
  }
}
