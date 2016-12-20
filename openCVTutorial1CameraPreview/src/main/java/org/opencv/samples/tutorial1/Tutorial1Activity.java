package org.opencv.samples.tutorial1;

import java.io.IOException;
import java.util.jar.Manifest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;


public class Tutorial1Activity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    // FlagDraw
    private boolean flagDraw = false;
    private String FLAGDRAW = "FLAGDRAW";

    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView imgTitle, imgDisp;
    private VerticalTextView imgTitle_p, imgDisp_p;

    // A key for storing the index of the active image size.
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX = "imageDetectionFilterIndex";

    // The filters.
    private ImageDetectionFilter[] mImageDetectionFilters;

    // The indices of the active filters.
    private int mImageDetectionFilterIndex;

    // Target found index.
    private int foundTargetIndex = -1;

    private int falseCount = 0;

    // The index of the active image size.
    private int mImgSizeIndex;

    private ImageProcessTask imageProcessTask;

    private static final int MY_PERMISSIONS_REQUEST_OPEN_CAMERA = 1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:


                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    final ImageDetectionFilter chengpo;
                    try {
                        chengpo = new ImageDetectionFilter(Tutorial1Activity.this,
                                R.drawable.chengpo);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load drawable: " + "chengpo");
                        e.printStackTrace();
                        break;
                    }

                    final ImageDetectionFilter chiayi;
                    try {
                        chiayi = new ImageDetectionFilter(
                                Tutorial1Activity.this,
                                R.drawable.chiayi);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load drawable: "
                                + "chiayi");
                        e.printStackTrace();
                        break;
                    }

                    final ImageDetectionFilter summer_street;
                    try {
                        summer_street = new ImageDetectionFilter(
                                Tutorial1Activity.this,
                                R.drawable.summer_street);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load drawable: "
                                + "summer_street");
                        e.printStackTrace();
                        break;
                    }

                    mImageDetectionFilters = new ImageDetectionFilter[]{summer_street, chengpo, chiayi};

                    break;

                default:

                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (savedInstanceState != null) {

            mImageDetectionFilterIndex = savedInstanceState.getInt(
                    STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
            mImgSizeIndex = savedInstanceState
                    .getInt(STATE_IMAGE_SIZE_INDEX, 0);

        } else {

            mImgSizeIndex = 0;
            mImageDetectionFilterIndex = 0;

        }

        setContentView(R.layout.tutorial1_surface_view);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);


        imgTitle = (TextView) findViewById(R.id.imgTitle);
        imgDisp = (TextView) findViewById(R.id.imgDisp);

        imgDisp_p = (VerticalTextView) findViewById(R.id.vImgDisp);
        imgTitle_p = (VerticalTextView) findViewById(R.id.vImgTitle);


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_OPEN_CAMERA);
        } else {
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setMaxFrameSize(1280, 720);


            mOpenCvCameraView.setCvCameraViewListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

        // 進入 AsyncTask 處理計算，避免 拖慢更新速度
        if (imageProcessTask != null && !imageProcessTask.isCancelled()) {
            imageProcessTask.cancel(true);
            imageProcessTask = null;
        }

        if (imageProcessTask == null) {
            imageProcessTask = new ImageProcessTask();
            imageProcessTask.execute(rgba); // 傳入 rgba
        }


        //Core.flip(rgba, rgba, 1);
        // Apply the active filters.

//   for(mImageDetectionFilterIndex = 0 ; mImageDetectionFilterIndex < mImageDetectionFilters.length;mImageDetectionFilterIndex++){
//
//    		if(mImageDetectionFilterIndex == mImageDetectionFilters.length){
//    			mImageDetectionFilterIndex = 0;
//   		}
//
//
//
//    		mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, rgba);
//        	flagDraw = mImageDetectionFilters[mImageDetectionFilterIndex].targetFound();
//
//        	Log.d(FLAGDRAW, "flagDraw : "+ mImageDetectionFilterIndex);
//        	Log.d(FLAGDRAW, "flagDraw : "+ flagDraw);
//
//
//        		if(flagDraw){
//
//        			foundTargetIndex = mImageDetectionFilterIndex;
//        			//mImageDetectionFilters[foundTargetIndex].apply(rgba, rgba);
//            		//flagDraw = mImageDetectionFilters[mImageDetectionFilterIndex].targetFound();
//
//            		Log.d(FLAGDRAW, "!!!!flagDraw : "+ foundTargetIndex);
//            		Log.d(FLAGDRAW, "!!!!flagDraw : "+ flagDraw);
//
//            		switch (foundTargetIndex) {
//            		case 2:
//						// 設定文字說明
//						Thread chiayi = new Thread(new Runnable() {
//
//							@Override
//							public void run() {
//
//								mHandler.sendEmptyMessage(2);
//
//							}
//
//						});
//						chiayi.start();
//
//						break;
//
//
//					case 1:
//						// 設定文字說明
//						Thread chengpo = new Thread(new Runnable() {
//
//							@Override
//							public void run() {
//
//								mHandler.sendEmptyMessage(1);
//
//							}
//
//						});
//						chengpo.start();
//
//						break;
//
//					case 0:
//						// 設定文字說明
//						Thread summer_street = new Thread(new Runnable() {
//
//							@Override
//							public void run() {
//
//								mHandler.sendEmptyMessage(0);
//
//							}
//
//						});
//						summer_street.start();
//						break;
//
//					}
//
//
//        		}
//
//    		}
        return rgba;
    }


    // 便是成功後 設定 擴增文字訊息
    Handler mHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 2) {

                imgTitle.setText("");
                imgDisp.setText("");

                imgTitle_p.setText(R.string.chiayi_title);
                imgDisp_p.setText(R.string.chiayi_des);

            }

            if (msg.what == 1) {

                imgTitle.setText("廟口");
                imgDisp.setText("藝術家： 陳澄波");
                imgTitle_p.setText("");
                imgDisp_p.setText("");
            }

            if (msg.what == 0) {

                imgTitle.setText("夏日街景");
                imgDisp.setText("藝術家： 陳澄波" + "\n" + "年代： 1927");
                imgTitle_p.setText("");
                imgDisp_p.setText("");
            }

            // 找不到把字清掉
            if (msg.what == -1) {
                imgTitle.setText("");
                imgDisp.setText("");
                imgTitle_p.setText("");
                imgDisp_p.setText("");
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        // TODO Auto-generated method stub


    }

    // ６．０ 請求權限
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_OPEN_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
                mOpenCvCameraView.setMaxFrameSize(1280, 720);


                mOpenCvCameraView.setCvCameraViewListener(this);
            } else {
                Toast.makeText(Tutorial1Activity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    // 使用 AsyncTask 處理 圖像偵測的部分
    private class ImageProcessTask extends AsyncTask<Mat, Mat, Mat> {

        private Mat bgMat;

        @Override
        protected Mat doInBackground(Mat... mats) {

            // 收到傳入的 Mat
            bgMat = mats[0];


            // 重複比對 標記圖像 與 手機鏡頭影像
            for (mImageDetectionFilterIndex = 0; mImageDetectionFilterIndex < mImageDetectionFilters.length; mImageDetectionFilterIndex++) {

                if (mImageDetectionFilterIndex == mImageDetectionFilters.length) {
                    mImageDetectionFilterIndex = 0;
                }

                mImageDetectionFilters[mImageDetectionFilterIndex].apply(bgMat, bgMat);
                flagDraw = mImageDetectionFilters[mImageDetectionFilterIndex].targetFound();

                Log.d(FLAGDRAW, "flagDraw : " + mImageDetectionFilterIndex);
                Log.d(FLAGDRAW, "flagDraw : " + flagDraw);

                // 找到的話就做下面的動作
                if (flagDraw) {
                    falseCount = 0;
                    foundTargetIndex = mImageDetectionFilterIndex;
                    //mImageDetectionFilters[foundTargetIndex].apply(rgba, rgba);
                    //flagDraw = mImageDetectionFilters[mImageDetectionFilterIndex].targetFound();

                    Log.d(FLAGDRAW, "!!!!flagDraw : " + foundTargetIndex);
                    Log.d(FLAGDRAW, "!!!!flagDraw : " + flagDraw);

                    switch (foundTargetIndex) {
                        case 2:
                            // 設定文字說明
                            Thread chiayi = new Thread(new Runnable() {

                                @Override
                                public void run() {

                                    mHandler.sendEmptyMessage(2);

                                }

                            });
                            chiayi.start();

                            break;


                        case 1:
                            // 設定文字說明
                            Thread chengpo = new Thread(new Runnable() {

                                @Override
                                public void run() {

                                    mHandler.sendEmptyMessage(1);

                                }

                            });
                            chengpo.start();

                            break;

                        case 0:
                            // 設定文字說明
                            Thread summer_street = new Thread(new Runnable() {

                                @Override
                                public void run() {

                                    mHandler.sendEmptyMessage(0);

                                }

                            });
                            summer_street.start();
                            break;
                    }

                } else {
                    falseCount = falseCount + 1;
                    if (falseCount > 5) {
                        Thread notfound = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mHandler.sendEmptyMessage(-1);
                            }
                        });
                        notfound.start();
                    }
                }
            }

            return null;
        }
    }


}
