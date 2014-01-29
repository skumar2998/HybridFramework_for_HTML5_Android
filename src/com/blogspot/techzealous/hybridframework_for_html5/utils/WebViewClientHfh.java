package com.blogspot.techzealous.hybridframework_for_html5.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

public class WebViewClientHfh extends WebViewClient {

	private final String LOG = "WebViewClientHfh";
	private final String THREAD_WORKER_NAME = "WebViewClient_ThreadWorker";
	
	private WeakReference<LinearLayout> mWeakLinearLayoutMain; 
	private WeakReference<WebView> mWeakWebView;
	private WeakReference<Context> mWeakCtx;
	private Handler mHandlerWebViewClient;
	private JSONObject mDictMethodInfo;
	private String mStrCallBackForDeviceOrientationChange;
	private SQLiteDatabase mDb;
	private Cursor mCursor;
	
	private Thread mThreadWorker;
	private Runnable mRunnableWorker;
	private Handler mHandlerWorker;
	
	public WebViewClientHfh (Context aCtx, LinearLayout aLinearLayoutMain, WebView aWebView) {
		super();
		mWeakLinearLayoutMain = new WeakReference<LinearLayout>(aLinearLayoutMain);
		mWeakCtx = new WeakReference<Context>(aCtx);
		mWeakWebView = new WeakReference<WebView>(aWebView);
		mHandlerWebViewClient = new Handler();
		//startThreadWorker();
	}
	
	@Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
    	Log.i(LOG, "shouldLoadUrl url=" + url);
    	if (url.startsWith(ConstantsHfh.HFH_SCHEMA_PREFIX)) {
            //do not load the page
    		final String strUrl = url;
    		mHandlerWorker.post(new Runnable() {
    			public void run() {
    				callNativeMethod(strUrl);
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
	
	private void callNativeMethod(String url) {
		String strJson = url.substring(ConstantsHfh.HFH_SCHEMA_PREFIX.length());
		JSONObject json = null;
		try {
			json = new JSONObject(strJson);
			//String strMethod = json.getString(ConstantsHfh.HFH_KEY_METHODNAME).replaceAll(":", "");
			String stringMethod = json.getString(ConstantsHfh.HFH_KEY_METHODNAME);
			String strMethod = stringMethod.substring(0, (stringMethod.length() - 2));
			
			Method method = WebViewClientHfh.this.getClass().getMethod(strMethod, new Class[] {JSONObject.class});
			method.invoke(WebViewClientHfh.this, new Object[] {json});
		} catch (JSONException e) {
			//call the JS error callback (if the HFH_KEY_METHODNAME does not exist an exception is thrown)
			try {
				String fName = json.getString(ConstantsHfh.HFH_KEY_ERROR_CALLBACK);
				callJsFunction(fName, "");
			} catch (JSONException e1) {
				//do nothing there seems to be no js error callback
				e1.printStackTrace();
			}
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			//call the JS error callback
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			//call the JS error callback
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			//call the JS error callback
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			//call the JS error callback
			e.printStackTrace();
		}
	}
	
	private void callPerformJavaScript(View aView) {
		int idx = InstanceData.getInstanceData().getArrayViews().indexOf(aView);
		String strJavaScript = InstanceData.getInstanceData().getArrayPerformJavaScript().get(idx);
		callJsFunction(strJavaScript);
	}

	public JSONObject getMethodInfo() {
		return mDictMethodInfo;
	}

	public String getCallBackForDeviceOrientationChange() {
		return mStrCallBackForDeviceOrientationChange;
	}

	public void callJsFunction(String aFunctionAndArgs) {
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				mWeakWebView.get().loadUrl(ConstantsHfh.JAVASCRIPT + "aFunctionAndArgs");
			}
		});
	}
	
	private void callJsFunction(String aFunctionName, String aArg) {
		StringBuilder sb = new StringBuilder(aFunctionName.length() + aArg.length() + 10);
		sb.append(aFunctionName);
		sb.append("('");
		sb.append(aArg);
		sb.append("')");
		callJsFunction(sb.toString());
	}

	public void callJsFunction(String aFunctionName, ArrayList<String> aArgs) {
		StringBuilder sb = new StringBuilder(aFunctionName.length() + (aArgs.size() * 10));
		sb.append(aFunctionName);
		sb.append("([");
		int arrayCount = aArgs.size();
		int arrayCountForLoop = aArgs.size();
		for(int x = 0; x < arrayCountForLoop; x++) {
			sb.append("'");
			sb.append(aArgs.get(x));
			sb.append("'");
			arrayCount--;
			if(arrayCount > 0) {
				sb.append(", ");
			} else {
				sb.append("])");
			}
		}
		callJsFunction(sb.toString());
	}
	
	/* Instance Variables Persistence */
	//'jn://{"method":"callGetInstanceVariable:", "name":"my_var_name", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callGetInstanceVariable(JSONObject aDict) throws JSONException {
		String val = InstanceData.getInstanceData().getDictInstanceVariables().get(aDict.getString(ConstantsHfh.HFH_KEY_NAME));
		String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
		callJsFunction(fName, val);
	}

	//'jn://{"method":"callSaveInstanceVariable:", "name":"my_var_name", "value":"var_value", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callSaveInstanceVariable(JSONObject aDict) throws JSONException {	
		String valName = aDict.getString(ConstantsHfh.HFH_KEY_NAME);
		String val = aDict.getString(ConstantsHfh.HFH_KEY_VALUE);
		InstanceData.getInstanceData().getDictInstanceVariables().put(valName, val);
			
		String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
		callJsFunction(fName, "");
	}

	//'jn://{"method":"callRemoveInstanceVariable:", "name":"my_var_name"}'
	public void callRemoveInstanceVariable(JSONObject aDict) throws JSONException {
		String valName = aDict.getString(ConstantsHfh.HFH_KEY_NAME);
		InstanceData.getInstanceData().getDictInstanceVariables().remove(valName);
	}

	/* Native View use */
	//'jn://{"method":"callSetSizeWebViewMain:", "x":"x_position", "y":"y_position", "width":"size_width", "height":"size_height"}'
	public void callSetSizeWebViewMain(JSONObject aDict) {
		
	}

	//'jn://{"method":callMakeButton:", "buttontype":"type_int", "red":"redcolorvalue", "green":"greencolorvalue", "blue":"bluecolorvalue", "alpha":"alpha_value", "x":"x_position", "y":"y_position", "width":"size_width", "height":"size_height", "onclick":"javascript_function_name", "title":"button_title"}'
	// Values for color - [0-255]
	public void callMakeButton(JSONObject aDict) {
		
	}

	//'jn://{"method":"setButtonTitle:", "title":"button_title", "viewidx":"index_in_the_instance_views_array"}'
	public void setButtonTitle(JSONObject aDict) throws JSONException {
		final String strTitle = aDict.getString(ConstantsHfh.HFH_KEY_TITLE);
		final Integer viewIdx = (Integer) aDict.get(ConstantsHfh.HFH_KEY_VIEWIDX);
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				Button button = (Button) InstanceData.getInstanceData().getArrayViews().get(viewIdx.intValue());
				button.setText(strTitle);
			}
		});	
	}

	//'jn://{"method":"setBackgroundColor:", "red":"color_value", "green":"color_value", "blue":"color_value", "alpha":"alpha_value", "viewidx":"index_in_the_instance_views_array"}'
	public void setBackgroundColor(JSONObject aDict) throws JSONException {
		int red = ((Integer) aDict.get(ConstantsHfh.HFH_KEY_RED)).intValue();
		int green = ((Integer) aDict.get(ConstantsHfh.HFH_KEY_GREEN)).intValue();
		int blue = ((Integer) aDict.get(ConstantsHfh.HFH_KEY_BLUE)).intValue();
		int alpha = ((Integer) aDict.get(ConstantsHfh.HFH_KEY_ALPHA)).intValue();
		final int viewIdx = ((Integer) aDict.get(ConstantsHfh.HFH_KEY_VIEWIDX)).intValue();
		final int color = Color.argb(alpha, red, green, blue);
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				View view = (View) InstanceData.getInstanceData().getArrayViews().get(viewIdx);
				view.setBackgroundColor(color);
			}
		});
	}

	//'jn://{"method":"setButtonBackgroundImage:", "img":"filepath_toimage_file", "viewidx":"index_in_the_instance_views_array"}'
	public void setButtonBackgroundImage(JSONObject aDict) throws JSONException {
		String strImg = aDict.getString(ConstantsHfh.HFH_KEY_IMAGE);
		final int viewIdx = ((Integer) aDict.get(ConstantsHfh.HFH_KEY_VIEWIDX)).intValue();
		final Drawable bg = BitmapDrawable.createFromPath(strImg);
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				Button button = (Button) InstanceData.getInstanceData().getArrayViews().get(viewIdx);
				button.setBackgroundDrawable(bg);
			}
		});
	}

	//'jn://{"method":"setPositionAndSize:", "x":"x_position", "y":"y_position", "width":"size_width", "height":"size_height", "viewidx":"index_in_the_instance_views_array"}'
	public void setPositionAndSize(JSONObject aDict) {
		
	}

	//'jn://{"method":"setButtonOnClickListener:", "onclick":"javascript_function_name", "viewidx":"index_in_the_instance_views_array"}'
	public void setButtonOnClickListener(JSONObject aDict) throws JSONException {
		String strOnClick = aDict.getString(ConstantsHfh.HFH_KEY_ONCLICK);
		int viewIdx = aDict.getInt(ConstantsHfh.HFH_KEY_VIEWIDX);
		InstanceData.getInstanceData().getArrayPerformJavaScript().add(viewIdx, strOnClick);
		Button button = ((Button) InstanceData.getInstanceData().getArrayViews().get(viewIdx));
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				callPerformJavaScript(v);
			}
		});
	}

	//'jn://{"method":"setViewHidden:", "viewidx":"index_in_the_instance_views_array"}'
	public void setViewHidden(JSONObject aDict) throws JSONException {
		final Integer viewIdx = (Integer) aDict.get(ConstantsHfh.HFH_KEY_VIEWIDX);
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				View view = InstanceData.getInstanceData().getArrayViews().get(viewIdx.intValue());
				view.setVisibility(View.GONE);
			}
		});
	}

	//'jn://{"method":"setViewShown:", "viewidx":"index_in_the_instance_views_array"}'
	public void setViewShown(JSONObject aDict) throws JSONException {
		final Integer viewIdx = (Integer) aDict.get(ConstantsHfh.HFH_KEY_VIEWIDX);
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				View view = InstanceData.getInstanceData().getArrayViews().get(viewIdx.intValue());
				view.setVisibility(View.VISIBLE);
			}
		});
	}

	//'jn://{"method":"callRemoveFromSuperView:", "viewidx":"index_in_the_instance_views_array"}'
	public void callRemoveFromSuperView(JSONObject aDict) throws JSONException {
		final int viewIdx = aDict.getInt(ConstantsHfh.HFH_KEY_VIEWIDX);
		mHandlerWebViewClient.post(new Runnable() {
			public void run() {
				mHandlerWebViewClient.post(new Runnable() {
					public void run() {
						View view = InstanceData.getInstanceData().getArrayViews().remove(viewIdx);
						mWeakLinearLayoutMain.get().removeView(view);
					}
				});
			}
		});
	}

	//'jn://{"method":"callReleaseView:", "viewidx":"index_in_the_instance_views_array"}'
	public void callReleaseView(JSONObject aDict) throws JSONException {
		int viewIdx = aDict.getInt(ConstantsHfh.HFH_KEY_VIEWIDX);
		InstanceData.getInstanceData().getArrayViews().remove(viewIdx);
		InstanceData.getInstanceData().getArrayPerformJavaScript().remove(viewIdx);
		callRemoveFromSuperView(aDict);
	}

	//'jn://{"method":"callRemoveView:", "viewidx":"index_in_the_instance_views_array"}'
	public void callRemoveView(JSONObject aDict) throws JSONException {
		int viewIdx = aDict.getInt(ConstantsHfh.HFH_KEY_VIEWIDX);
		InstanceData.getInstanceData().getArrayViews().remove(viewIdx);
		InstanceData.getInstanceData().getArrayPerformJavaScript().remove(viewIdx);
		callRemoveFromSuperView(aDict);
	}

	/* Disk storage */
	//'jn://{"method":"callGetPath:", "docsdir":"yes", "success":"callback_function_name"}'
	//callback should expect the path to be passed as a string argument
	//if key docsdir exists with some value the path to docsdir will be returned, if it does not exist, path temp dir will be returned.
	public void callGetPath(JSONObject aDict) {
		String fName = null;
		String path = null;
		try {
			fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
			path = aDict.getString(ConstantsHfh.HFH_KEY_DOCSDIR);
			File filePath = Environment.getExternalStorageDirectory();
			callJsFunction(fName, filePath.getAbsolutePath());
		} catch (JSONException e) {
			if(path == null && fName != null) {
				File tempDir = mWeakCtx.get().getExternalCacheDir();
				callJsFunction(fName, tempDir.getAbsolutePath());
			}
			e.printStackTrace();
		}
	}

	//'jn://{"method":"callGetPathToDocsDir:", "success":"callback_function_name"}'
	//callback should expect the path to be passed as a string argument
	public void callGetPathToDocsDir(JSONObject aDict) throws JSONException {
		String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
		File filePath = Environment.getExternalStorageDirectory();
		callJsFunction(fName, filePath.getAbsolutePath());
	}

	//'jn://{"method":"callGetPathToTempDir:", "success":"callback_function_name"}'
	//callback should expect the path to be passed as a string argument
	public void callGetPathToTempDir(JSONObject aDict) throws JSONException {
		String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);		
		File tempDir = mWeakCtx.get().getExternalCacheDir();
		callJsFunction(fName, tempDir.getAbsolutePath());
	}

	/* File command buffer */
	//'jn://{"method":"callGetCommandBuffer:", "success":"callback_function_name"}'
	//callback should expect the path to command buffer file to be passed as argument
	public void callGetCommandBuffer(JSONObject aDict) throws JSONException {
		try {
			File buffFile = File.createTempFile(ConstantsHfh.HFH_BUFFER_FILENAME, "");
			String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
			callJsFunction(fName, buffFile.getAbsolutePath());
		} catch (IOException e) {
			//call js error callback
			e.printStackTrace();
		}
	}

	//'jn://{"method":"callMethodWithCommandBuffer:", "hfh_bufferfile_path":"filepath_to_bufferfile", "error":"callback_function_name"}'
	//callback should expect error description to be passed as string argument
	public void callMethodWithCommandBuffer(JSONObject aDict) throws JSONException {
		try {
			String bufFilePath = aDict.getString(ConstantsHfh.HFH_KEY_BUFFER_FILEPATH);
			BufferedReader br = new BufferedReader(new FileReader(bufFilePath), 8192);
			String line = null;
			StringBuilder sb = new StringBuilder(500);
			while((line = br.readLine()) != null) {
				sb.append(line);
			}
			br.close();
			callNativeMethod(sb.toString());
		} catch (FileNotFoundException e) {
			//call js error callback
			e.printStackTrace();
		} catch (IOException e) {
			//call js error callback
			e.printStackTrace();
		}
	}

	/* SQLite */
	//'jn://{"method":"callOpenOrCreateDb:", "path":"file_path_to_dbfile", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callOpenOrCreateDb(JSONObject aDict) throws JSONException {
		String dbName = aDict.getString(ConstantsHfh.HFH_KEY_NAME);
		mDb = mWeakCtx.get().openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
		if(mDb != null) {
			String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
			callJsFunction(fName, "");
		} else {
			String fName = aDict.getString(ConstantsHfh.HFH_KEY_ERROR_CALLBACK);
			callJsFunction(fName, "");
		}
	}

	//'jn://{"method":"callOpenOrCreateDbInDocsDir:", "name":"db_file_name", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callOpenOrCreateDbInDocsDir(JSONObject aDict) throws JSONException {
		callOpenOrCreateDb(aDict);
	}

	//'jn://{"method":"callOpenOrCreateDbInTempDir:", "name":"db_file_name", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callOpenOrCreateDbInTempDir(JSONObject aDict) throws JSONException {
		callOpenOrCreateDb(aDict);
	}

	//'jn://{"method":"callExecSQL:", "sql":"sqlite_statement", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callExecSQL(JSONObject aDict) throws JSONException {
		try {
			String stmt = aDict.getString(ConstantsHfh.HFH_KEY_SQL);
			if(mDb == null) {
				String fName = aDict.getString(ConstantsHfh.HFH_KEY_ERROR_CALLBACK);
				String msg = LOG + "error - there is no database opened";
				callJsFunction(fName, msg);
				return;
			}
			mDb.execSQL(stmt);
			String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
			callJsFunction(fName, "");
		} catch (SQLiteException sqlEx) {
			//call js error callback
		}
	}

	//'jn://{"method":"callExecQuery:", "sql":"sqlite_query_statment", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callExecQuery(JSONObject aDict) throws JSONException {
		String query = aDict.getString(ConstantsHfh.HFH_KEY_SQL);
		if(mDb == null) {
			String fName = aDict.getString(ConstantsHfh.HFH_KEY_ERROR_CALLBACK);
			String msg = LOG + "error - there is no database opened";
			callJsFunction(fName, msg);
			return;
		}
		mCursor = mDb.rawQuery(query, null);
		if(mCursor != null && mCursor.moveToFirst()) {
			getRowDataAsJsonString(mCursor, aDict);
		} else {
			//call js error callback
		}
	}

	//'jn://{"method":"callMoveToNext:", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callMoveToNext(JSONObject aDict) {
		if(mCursor.moveToNext()) {
			getRowDataAsJsonString(mCursor, aDict);
		} else {
			//call js error callback
		}
	}

	//'jn://{"method":"callCloseDb:", "success":"callback_function_name", "error":"callback_function_name"}'
	public void callCloseDb(JSONObject aDict) throws JSONException {
		mDb.close();
		String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
		callJsFunction(fName, "");
	}

	/* Camera use (pics, pics in photolibrary, video) */
	//'jn://{"method":"callCamera:"}'
	public void callCamera(JSONObject aDict) {
		
	}

	//'jn://{"method":"callPhotoLibrary:", "success":"callback_function_name"}'
	public void callPhotoLibrary(JSONObject aDict) {
		
	}

	/* Motion Sensor */
	//'jn://{"method":"callStartMonitorDeviceOrientation:", "success":"callback_function_name"}'
	public void callStartMonitorDeviceOrientation(JSONObject aDict) {
		
	}

	//'jn://{"method":"callStopMonitorDeviceOrientation:"}'
	public void callStopMonitorDeviceOrientation(JSONObject aDict) {
		
	}

	/* Contacts */
	//'jn://{"method":"callShowContacts:", "success":"callback_function_name"}'
	public void callShowContacts(JSONObject aDict) {
		
	}

	/* Util methods */
	public String buildJsFunction(String aFunctionName, ArrayList<String> aArrayArgs) {
		StringBuilder sb = new StringBuilder(aFunctionName.length() + (aArrayArgs.size() * 10));
		sb.append(aFunctionName);
		sb.append("([");
		int arrayCount = aArrayArgs.size();
		int arrayCountForLoop = aArrayArgs.size();
		for(int x = 0; x < arrayCountForLoop; x++) {
			sb.append("'");
			sb.append(aArrayArgs.get(x));
			sb.append("'");
			arrayCount--;
			if(arrayCount > 0) {
				sb.append(", ");
			} else {
				sb.append("])");
			}
		}
		return sb.toString();
	}

	private void getRowDataAsJsonString(Cursor aCursor, JSONObject aDict) {
		JSONObject json = new JSONObject();
		int cols = aCursor.getColumnCount();
		for(int x = 0; x < cols; x++) {
			String colName = aCursor.getColumnName(x);
			String colVal = aCursor.getString(x);
			try {
				json.put(colName, colVal);
			} catch (JSONException e) {
				//call js error callback
				e.printStackTrace();
			}
		}
		try {
			String fName = aDict.getString(ConstantsHfh.HFH_KEY_SUCCESS_CALLBACK);
			callJsFunction(fName, json.toString());
		} catch (JSONException e) {
			//call js error callback
			e.printStackTrace();
		}
	}

	/* Additional Methods */
	public void callLoadHome(JSONObject aDict) {
		mHandlerWebViewClient.post(new Runnable() {
			@Override
			public void run() {
				mWeakWebView.get().loadUrl(ConstantsHfh.JAVASCRIPT + "window.document.location = 'index.html'");
			}
		});
	}
}
