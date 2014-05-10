package edu.illinois.thirdeye;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import edu.illinois.thirdeye.util.SystemUiHider;

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
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */

	private static final String TAG = "ThirdEye";

	private CameraBridgeViewBase mOpenCvCameraView;
	private BaseLoaderCallback mLoaderCallback;
	private HOGDescriptor mDetector;
	
	private CascadeClassifier mClassifier;

	private RadioGroup mRadioGroup;
	private RadioButton mRadioSelected;
	private Button mBtnSelect;
	
	private enum DetectionMethod{
		HOG,
		HAAR
	};
	
	private DetectionMethod mDetectionMethod;
	
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
		
		mDetectionMethod = DetectionMethod.HOG;
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

		MatOfFloat mySvmDetector = readSvmModel(R.raw.descriptorvector96gray);
		mDetector = new HOGDescriptor(new Size(96,96), new Size(16,16), new Size(8,8), new Size(8,8), 9);
		mDetector.setSVMDetector(mySvmDetector);
		
		// load cascade file
		try{
			InputStream is = getResources().openRawResource(R.raw.cascade);
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			File mCascadeFile = new File(cascadeDir, "cascade.xml");
			FileOutputStream os = new FileOutputStream(mCascadeFile);
			
			byte[] buffer = new byte[4096];
			int bytesRead;
			while((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();
			
			mClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
			if (mClassifier.empty()){
				Log.e(TAG, "Failed to load cacade classifier");
				mClassifier = null;
			}
			else {
				Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
			}
			
			cascadeDir.delete();
		}
		catch (IOException e){
			e.printStackTrace();
			Log.e(TAG, "Failed to load cacade classifier. Exception thrown: " + e);
		}
				
		addListenerOnButton();
	}


	public void addListenerOnButton() {
		mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
		mBtnSelect = (Button) findViewById(R.id.radio_select);
		
		mBtnSelect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int selectedBtnId = mRadioGroup.getCheckedRadioButtonId();
				mRadioSelected = (RadioButton) findViewById(selectedBtnId);
				
				if (selectedBtnId == R.id.radio_hog){
					mDetectionMethod = DetectionMethod.HOG;
				}
				else
				{
					mDetectionMethod = DetectionMethod.HAAR;
				}
				
				Toast.makeText(ThirdEyeActivity.this,
						mRadioSelected.getText(), Toast.LENGTH_SHORT).show();
			}
		});
	}


	/**
	 * Reads the file containing the SVM model and stores its contents as a matrix of floating point values.
	 */
	private MatOfFloat readSvmModel(int id)
	{
		ArrayList<Float> descriptors = new ArrayList<Float>();
		BufferedReader in = null;
		try
		{
			String descriptorVector;
			InputStream stream = this.getResources().openRawResource(id);
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
		Mat compositeImg = inputFrame.rgba();
		MatOfRect foundLocations = new MatOfRect();
		MatOfDouble weights = new MatOfDouble();
		
		if (mDetectionMethod == DetectionMethod.HOG){
			mDetector.detectMultiScale(inputFrame.gray(), foundLocations, weights);
		}
		else {
			mClassifier.detectMultiScale(inputFrame.gray(), foundLocations, 1.1, 3, Objdetect.CASCADE_DO_CANNY_PRUNING | Objdetect.CASCADE_DO_ROUGH_SEARCH, new Size(200,200), new Size());
		}
		
		List<org.opencv.core.Rect> detections = foundLocations.toList();
		for(org.opencv.core.Rect rect : detections)
		{
			Core.rectangle(compositeImg, rect.tl(), rect.br(), new Scalar(255, 0, 0), 3);
		}
		return compositeImg;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
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
}
