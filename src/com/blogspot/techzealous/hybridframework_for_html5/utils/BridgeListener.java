package com.blogspot.techzealous.hybridframework_for_html5.utils;

import android.view.View;
import android.webkit.WebView;

public interface BridgeListener {

	public void callJsFunction(String aFunctionAndArgs);

	public void callAddSubView(View aView);

	public WebView getWebViewMain();

	public void callShowCamera();
	public void callShowPhotoLibrary();

	public void callShowAddressBook();

	public void callBeginDeviceOrientationNotif();
	public void callEndDeviceOrientationNotif();
}
