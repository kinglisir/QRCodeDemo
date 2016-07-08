package com.example.qrcodedemo;


import com.example.qrcodedemo.third.BeepManager;
import com.example.qrcodedemo.third.CameraManager;
import com.example.qrcodedemo.third.CaptureActivityHandler;
import com.example.qrcodedemo.third.FinishListener;
import com.example.qrcodedemo.third.InactivityTimer;
import com.example.qrcodedemo.third.IntentSource;
import com.example.qrcodedemo.third.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.json.JSONException;

/**
 * ���activity��������ں�̨�߳��������ɨ�裻��������һ�����view��������ȷ����ʾ�����룬��ɨ���ʱ����ʾ������Ϣ��
 * Ȼ����ɨ��ɹ���ʱ�򸲸�ɨ����	
 * 	���ɨһɨ,��ת�������
 * 
 */
public final class QRcodeActvity extends Activity implements
SurfaceHolder.Callback {

	private static final String TAG = QRcodeActvity.class.getSimpleName();

	// �������
	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private IntentSource source;
	private Collection<BarcodeFormat> decodeFormats;
	private Map<DecodeHintType, ?> decodeHints;
	private String characterSet;
	// ��������
	private InactivityTimer inactivityTimer;
	// �������𶯿���
	private BeepManager beepManager;
	private String groupUuid;

	private String toUserName;
	private String groupIcon;
	private String companyUuid;//�������ҵȺ�����ع�˾��uuid



	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	/**
	 * OnCreate�г�ʼ��һЩ�����࣬��InactivityTimer�����ߣ���Beep���������Լ�AmbientLight������ƣ�
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// ����Activity���ڻ���״̬
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_qrcode);
		//showNav(true, R.string.scan);
		findViewById(R.id.bt_cancle).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});;
		hasSurface = false;

		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// CameraManager�����������ʼ������������onCreate()�С�
		// ���Ǳ���ģ���Ϊ�����ǵ�һ�ν���ʱ��Ҫ��ʾ����ҳ�����ǲ������Camera,������Ļ��С
		// ��ɨ���ĳߴ粻��ȷʱ�����bug
		cameraManager = new CameraManager(getApplication());

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);
		
		handler = null;

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// activity��pausedʱ������stopped,���surface�Ծɴ��ڣ�
			// surfaceCreated()������ã�����������ʼ��camera
			initCamera(surfaceHolder);
		} else {
			// ����callback���ȴ�surfaceCreated()����ʼ��camera
			surfaceHolder.addCallback(this);
		}

		beepManager.updatePrefs();
		inactivityTimer.onResume();

		source = IntentSource.NONE;
		decodeFormats = null;
		characterSet = null;
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		beepManager.close();
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/**
	 * ɨ��ɹ�����������Ϣ
	 * 
	 * @param rawResult
	 * @param barcode
	 * @param scaleFactor
	 */
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		inactivityTimer.onActivity();
		String resultString = rawResult.getText();
		if (resultString.equals("")) {
			Toast.makeText(this, "ɨ��ʧ��", Toast.LENGTH_SHORT).show();
		}else {	
			Toast.makeText(this, "ɨ��ɹ� ���ﴦ��ɨ����", Toast.LENGTH_SHORT).show();
		}
		finish();

	}


	/**
	 * ��ʼ��Camera
	 * 
	 * @param surfaceHolder
	 */
	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
			return;
		}
		try {
			// ��CameraӲ���豸
			cameraManager.openDriver(surfaceHolder);
			// ����һ��handler����Ԥ�������׳�һ������ʱ�쳣
			if (handler == null) {
				handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
			}
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit();
		}
	}
	/**
	 * ��ʾ�ײ������Ϣ���˳�Ӧ��
	 */
	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage("Sorry, the Android camera encountered a problem. You may need to restart the device.");
		builder.setPositiveButton("OK", new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

}
