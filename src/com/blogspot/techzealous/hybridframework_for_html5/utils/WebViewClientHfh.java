package com.blogspot.techzealous.hybridframework_for_html5.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

public class WebViewClientHfh extends WebViewClient {

	private final String LOG = "WebViewClientHfh";
	private final String THREAD_WORKER_NAME = "WebViewClientHfh_worker";
	
	private Thread mThreadWorker;
	private Runnable mRunnableWorker;
	private Handler mHandlerWorker;
	private BridgeHelper mBridgeHelper;
	
	public WebViewClientHfh (Activity aAct, Context aCtx, LinearLayout aLinearLayoutMain, WebView aWebView) {
		super();
		Handler handlerMain = new Handler();
		mBridgeHelper = new BridgeHelper(aAct, aCtx, aLinearLayoutMain, aWebView, handlerMain);
	}
	
	@Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
    	Log.i(LOG, "shouldLoadUrl url=" + url);
    	if (url.startsWith(ConstantsHfh.HFH_SCHEMA_PREFIX)) {
            //do not load the page
    		final String strUrl = url;
    		mHandlerWorker.post(new Runnable() {
    			public void run() {
    				mBridgeHelper.callNativeMethod(strUrl);
    			}
    		});
            return true;
        }
        //load the page
        return false;
    }
	
	public Handler getHandlerWorker() {
		return mHandlerWorker;
	}
	
	public BridgeHelper getBridgeHelper() {
		return mBridgeHelper;
	}
	
	public void startThreadWorker() {
		mRunnableWorker = new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				mHandlerWorker = new Handler();
				Looper.loop();
			}
		};
		mThreadWorker = new Thread(mRunnableWorker, THREAD_WORKER_NAME);
		mThreadWorker.start();
	}
}
