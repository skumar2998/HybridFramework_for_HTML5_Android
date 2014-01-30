package com.blogspot.techzealous.hybridframework_for_html5;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.blogspot.techzealous.hybridframework_for_html5.utils.ConstantsHfh;
import com.blogspot.techzealous.hybridframework_for_html5.utils.WebViewClientHfh;

public class MainActivity extends Activity {

	private final String LOG = "MainActivity";
	private LinearLayout linearLayoutMain;
	private WebView webViewMain;
	
	private WebViewClientHfh mWebViewClient;
	private Runnable mRunnableStopWorkerWebViewClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		linearLayoutMain = (LinearLayout) findViewById(R.id.LinearLayoutMain);
		webViewMain = (WebView) findViewById(R.id.webViewMain);
		
		webViewMain.getSettings().setJavaScriptEnabled(true);
		mWebViewClient = new WebViewClientHfh(this, linearLayoutMain, webViewMain);
		webViewMain.setWebViewClient(mWebViewClient);
		
		//webViewMain.loadUrl("javascript:myFunction()");
		mRunnableStopWorkerWebViewClient = new Runnable() {
			public void run() {
				Looper.myLooper().quit();
			}
		};
		
		webViewMain.loadUrl(ConstantsHfh.PAGE_HOME);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mWebViewClient.getHandlerWorker().post(mRunnableStopWorkerWebViewClient);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mWebViewClient.startThreadWorker();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
