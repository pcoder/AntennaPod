package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.NetworkUtils;

/** Refreshes all feeds when it receives an intent */
public class FeedUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "FeedUpdateReceiver";
	public static final String ACTION_REFRESH_FEEDS = "de.danoeh.antennapod.feedupdatereceiver.refreshFeeds";
	public static final String ACTION_DOWNLOAD_FEEDS = "de.danoeh.antennapod.feedupdatereceiver.downloadFeeds";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_REFRESH_FEEDS)) {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Received intent");
			boolean mobileUpdate = UserPreferences.isAllowMobileUpdate();
			if (mobileUpdate || connectedToWifi(context)) {
				DBTasks.refreshExpiredFeeds(context);
			} else {
				if (BuildConfig.DEBUG)
					Log.d(TAG,
							"Blocking automatic update: no wifi available / no mobile updates allowed");
			}
		} else if (intent.getAction().equals(ACTION_DOWNLOAD_FEEDS)) {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Received intent");
			boolean mobileUpdate = UserPreferences.isAllowMobileUpdate();
			if (mobileUpdate || connectedToWifi(context)) {
				// download feeds
				DBTasks.refreshExpiredFeeds(context);
				if (NetworkUtils.autodownloadNetworkAvailable(context)) {
					if (BuildConfig.DEBUG)
						Log.d(TAG,
								"Alarm activated - auto-dl network available, starting auto-download");
					DBTasks.autodownloadUndownloadedItems(context);
				} else { // if new network is Wi-Fi, finish ongoing downloads,
							// otherwise cancel all downloads
					ConnectivityManager cm = (ConnectivityManager) context
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo ni = cm.getActiveNetworkInfo();
					if (ni == null
							|| ni.getType() != ConnectivityManager.TYPE_WIFI) {
						if (BuildConfig.DEBUG)
							Log.i(TAG,
									"Alarm activated - Device is no longer connected to Wi-Fi. Cancelling ongoing downloads");
						DownloadRequester.getInstance().cancelAllDownloads(
								context);
					}
				}
			} else {
				if (BuildConfig.DEBUG)
					Log.d(TAG,
							"Alarm activated - Blocking download feeds: no wifi available / no mobile updates allowed");
			}

		}

	}

	private boolean connectedToWifi(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

}
