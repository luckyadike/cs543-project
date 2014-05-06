package edu.illinois.thirdeye;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.HOGDescriptor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ThirdEyeActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener
{
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	//private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	private static final String TAG = "ThirdEye";

	private CameraBridgeViewBase mOpenCvCameraView;
	private BaseLoaderCallback mLoaderCallback;
	private HOGDescriptor mDetector;
	private int idx;

	// private SystemUiHider mSystemUiHider;
	// private Handler mHideHandler;
	// private Runnable mHideRunnable;

	static
	{
		if(!OpenCVLoader.initDebug())
		{
	        Log.e(TAG, "Could not initialize OpenCV debug");
	    }
	}
	
	
	/**
	 * Initialize member variables
	 */
	public ThirdEyeActivity()
	{
		super();
		idx = 0;
		mLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status)
			{
				switch (status)
				{
					case LoaderCallbackInterface.SUCCESS:
					{
						Log.i(TAG, "OpenCV loaded successfully");
						mOpenCvCameraView.enableView();
					}
					break;
					default: super.onManagerConnected(status); break;
				}
			}
		};

		/*
		 * mHideHandler = new Handler(); mSystemUiHider = new ??? mHideRunnable
		 * = new Runnable() {
		 * 
		 * @Override public void run() { mSystemUiHider.hide(); } };
		 */
	}
	
	
	/****************************************************************************************************/


	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (AUTO_HIDE) {
			// delayedHide(AUTO_HIDE_DELAY_MILLIS);
		}
		return false;
	}
	
	
	/****************************************************************************************************/

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_third_eye);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.ThirdEyeView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

		MatOfFloat mySvmDetector = readSvmModel();
		mDetector = new HOGDescriptor(new Size(200,200), new Size(16,16), new Size(8,8), new Size(8,8), 9);
		long dsize = mDetector.getDescriptorSize();
		mDetector.setSVMDetector(mySvmDetector);
	}


	/**
	 * Reads the file containing the SVM model and stores its contents as a matrix of floating point values.
	 */
	private MatOfFloat readSvmModel()
	{
		ArrayList<Float> descriptors = new ArrayList<Float>();
		BufferedReader in = null;
		try
		{
			String descriptorVector;
			InputStream stream = this.getResources().openRawResource(R.raw.descriptorvector2);
			in = new BufferedReader(new InputStreamReader(stream));
			while((descriptorVector = in.readLine()) != null)
			{
				StringTokenizer tokenizer = new StringTokenizer(descriptorVector, " ");
				while(tokenizer.hasMoreTokens())
				{
					descriptors.add(Float.valueOf(tokenizer.nextToken()));
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace(); //handle error better for app
		}
		finally
		{
			try
			{
				if (in != null)
				{
					in.close();
				}
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
		float[] descripts = new float[descriptors.size()];
		for(int i = 0; i < descriptors.size(); ++i)
		{
			descripts[i] = descriptors.get(i).floatValue();
		}
		return new MatOfFloat(descripts);
	}
	
	
	/****************************************************************************************************/


	@Override
	public void onPause()
	{
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}
	
	
	/****************************************************************************************************/


	public void onDestroy()
	{
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}
	
	
	/****************************************************************************************************/


	public void onCameraViewStarted(int width, int height)
	{}
	
	
	/****************************************************************************************************/



	public void onCameraViewStopped()
	{}
	
	
	/****************************************************************************************************/


	public Mat onCameraFrame(CvCameraViewFrame inputFrame)
	{
		Mat compositeImg = null;
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			Mat m = inputFrame.rgba();
			compositeImg = m.reshape(compositeImg.rows(), compositeImg.cols());
			compositeImg = m.t();
		}
		else
		{
			compositeImg = inputFrame.rgba();
		}
		
		//if(idx % 30 == 0)
		//{
			MatOfRect foundLocations = new MatOfRect();
			MatOfDouble weights = new MatOfDouble();
			mDetector.detectMultiScale(inputFrame.gray(), foundLocations, weights);

			List<org.opencv.core.Rect> detections = foundLocations.toList();
			for(org.opencv.core.Rect rect : detections)
			{
				Core.rectangle(compositeImg, rect.tl(), rect.br(), new Scalar(255, 0, 0), 3);
			}
		//}
		//idx++;
		return compositeImg;
	}

	/*
	 * @Override protected void onCreate(Bundle savedInstanceState) {
	 * super.onCreate(savedInstanceState);
	 * 
	 * setContentView(R.layout.activity_third_eye);
	 * 
	 * final View controlsView = findViewById(R.id.ThirdEyeView); final View
	 * contentView = findViewById(R.id.ThirdEyeView);
	 * 
	 * // Set up an instance of SystemUiHider to control the system UI for //
	 * this activity. mSystemUiHider = SystemUiHider.getInstance(this,
	 * contentView, HIDER_FLAGS); mSystemUiHider.setup(); mSystemUiHider
	 * .setOnVisibilityChangeListener(new
	 * SystemUiHider.OnVisibilityChangeListener() { // Cached values. int
	 * mControlsHeight; int mShortAnimTime;
	 * 
	 * @Override
	 * 
	 * @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2) public void
	 * onVisibilityChange(boolean visible) { if (Build.VERSION.SDK_INT >=
	 * Build.VERSION_CODES.HONEYCOMB_MR2) { // If the ViewPropertyAnimator API
	 * is available // (Honeycomb MR2 and later), use it to animate the //
	 * in-layout UI controls at the bottom of the // screen. if (mControlsHeight
	 * == 0) { mControlsHeight = controlsView.getHeight(); } if (mShortAnimTime
	 * == 0) { mShortAnimTime = getResources().getInteger(
	 * android.R.integer.config_shortAnimTime); } controlsView .animate()
	 * .translationY(visible ? 0 : mControlsHeight)
	 * .setDuration(mShortAnimTime); } else { // If the ViewPropertyAnimator
	 * APIs aren't // available, simply show or hide the in-layout UI //
	 * controls. controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
	 * }
	 * 
	 * if (visible && AUTO_HIDE) { // Schedule a hide().
	 * delayedHide(AUTO_HIDE_DELAY_MILLIS); } } });
	 * 
	 * // Set up the user interaction to manually show or hide the system UI.
	 * contentView.setOnClickListener(new View.OnClickListener() {
	 * 
	 * @Override public void onClick(View view) { if (TOGGLE_ON_CLICK) {
	 * mSystemUiHider.toggle(); } else { mSystemUiHider.show(); } } });
	 * 
	 * // Upon interacting with UI controls, delay any scheduled hide() //
	 * operations to prevent the jarring behavior of controls going away //
	 * while interacting with the UI. //
	 * findViewById(R.id.dummy_button).setOnTouchListener( //
	 * mDelayHideTouchListener); }
	 */
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		// delayedHide(100);
	}
	
	
	/****************************************************************************************************/


	@Override
	public void onResume()
	{
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this,
				mLoaderCallback);
	}
	

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	// private void delayedHide(int delayMillis)
	// {
	// mHideHandler.removeCallbacks(mHideRunnable);
	// mHideHandler.postDelayed(mHideRunnable, delayMillis);
	// }
}
