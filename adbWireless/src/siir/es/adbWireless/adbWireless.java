/**
 * siir.es.adbWireless.adbWireless.java
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package siir.es.adbWireless;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class adbWireless extends Activity {

	public static final String MSG_TAG = "ADBWIRELESS";
	public static final String PORT = "5555";

	private static NotificationManager mNotificationManager;

	public static boolean mState = false;
	public static boolean wifiState;

	private TextView tv_footer_1;
	private TextView tv_footer_2;
	private TextView tv_footer_3;
	private ImageView iv_button;

	private static final int MENU_PREFERENCES = 1;
	private static final int MENU_ABOUT = 2;
	private static final int MENU_EXIT = 3;

	private static final int START_NOTIFICATION_ID = 1;
	private static final int ACTIVITY_SETTINGS = 2;

	private static RemoteViews remoteViews = new RemoteViews("siir.es.adbWireless", R.layout.adb_appwidget);

	ProgressDialog spinner;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		spinner = new ProgressDialog(adbWireless.this);

		this.iv_button = (ImageView) findViewById(R.id.iv_button);

		this.tv_footer_1 = (TextView) findViewById(R.id.tv_footer_1);
		this.tv_footer_2 = (TextView) findViewById(R.id.tv_footer_2);
		this.tv_footer_3 = (TextView) findViewById(R.id.tv_footer_3);

		adbWireless.mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (!hasRootPermission()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.no_root)).setCancelable(true).setPositiveButton(getString(R.string.button_close), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					adbWireless.this.finish();
				}
			});
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.create();
			builder.setTitle(R.string.no_root_title);
			builder.show();
		}

		if (!checkWifiState(this)) {
			wifiState = false;
			saveWiFiState(this, wifiState);

			if (prefsWiFiOn(this)) {
				enableWiFi(this, true);
			} else {
				WiFidialog();
			}
		} else {
			wifiState = true;
			saveWiFiState(this, wifiState);
		}

		this.iv_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v)
			{
				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (prefsHaptic(adbWireless.this))
					vib.vibrate(45);

				try {
					if (!mState) {
						spinner.setMessage(getString(R.string.Turning_on));
						spinner.show();
						adbStart(adbWireless.this);
					} else {
						spinner.setMessage(getString(R.string.Turning_off));
						spinner.show();
						adbStop(adbWireless.this);
					}

					updateState();
					spinner.cancel();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

	}

	public static void saveWiFiState(Context context, boolean wifiState) {
		SharedPreferences settings = context.getSharedPreferences("wireless", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("wifiState", wifiState);
		editor.commit();
	}

	private void WiFidialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.no_wifi)).setCancelable(true).setPositiveButton(getString(R.string.button_exit), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				adbWireless.this.finish();
			}
		}).setNegativeButton(R.string.button_activate_wifi, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				enableWiFi(adbWireless.this, true);
				dialog.cancel();
			}
		});
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.create();
		builder.setTitle(R.string.no_wifi_title);
		builder.show();
	}

	@Override
	protected void onResume() {
		SharedPreferences settings = getSharedPreferences("wireless", 0);
		mState = settings.getBoolean("mState", false);
		wifiState = settings.getBoolean("wifiState", false);
		updateState();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		if (prefsWiFiOff(this) && !wifiState && checkWifiState(this)) {
			enableWiFi(this, false);
		}
		try {
			adbStop(this);
		} catch (Exception e) {}
		try {
			mNotificationManager.cancelAll();
		} catch (Exception e) {}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0, R.string.menu_prefs).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, MENU_EXIT, 0, R.string.menu_exit).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_PREFERENCES:
				Intent i = new Intent(this, ManagePreferences.class);
				startActivityForResult(i, ACTIVITY_SETTINGS);
				break;
			case MENU_ABOUT:
				showHelpDialog();
				return true;
			case MENU_EXIT:
				adbWireless.this.finish();
				return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void updateState() {
		if (mState) {
			tv_footer_1.setText(R.string.footer_text_1);
			try {
				tv_footer_2.setText("adb connect " + getWifiIp(this) + ":" + PORT);
			} catch (Exception e) {
				tv_footer_2.setText("adb connect unknowip:" + PORT);
			}
			tv_footer_2.setVisibility(View.VISIBLE);
			tv_footer_3.setVisibility(View.VISIBLE);
			iv_button.setImageResource(R.drawable.bt_off);

		} else {
			tv_footer_1.setText(R.string.footer_text_off);
			tv_footer_2.setVisibility(View.INVISIBLE);
			tv_footer_3.setVisibility(View.INVISIBLE);
			iv_button.setImageResource(R.drawable.bt_on);
		}
	}

	public static boolean adbStart(Context context) {
		try {
			setProp("service.adb.tcp.port", PORT);
			if (isProcessRunning("adbd")) {
				runRootCommand("stop adbd");
			}
			runRootCommand("start adbd");
			mState = true;
			SharedPreferences settings = context.getSharedPreferences("wireless", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("mState", mState);
			editor.commit();

			remoteViews.setImageViewResource(R.id.widgetButton, R.drawable.widgetoff);
			ComponentName cn = new ComponentName(context, adbWidgetProvider.class);
			AppWidgetManager.getInstance(context).updateAppWidget(cn, remoteViews);

			if (prefsNoti(context)) {
				showNotification(context, R.drawable.stat_sys_adb, context.getString(R.string.noti_text)+ " " + getWifiIp(context) + ":" + adbWireless.PORT);
			}
		} catch (Exception e) {return false;}
		return true;
	}

	public static boolean adbStop(Context context) throws Exception {
		try {
			setProp("service.adb.tcp.port", "-1");
			runRootCommand("stop adbd");
			runRootCommand("start adbd");
			mState = false;
			SharedPreferences settings = context.getSharedPreferences("wireless", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("mState", mState);
			editor.commit();
			remoteViews.setImageViewResource(R.id.widgetButton, R.drawable.widgeton);
			ComponentName cn = new ComponentName(context, adbWidgetProvider.class);
			AppWidgetManager.getInstance(context).updateAppWidget(cn, remoteViews);
			mNotificationManager.cancelAll();
		} catch (Exception e) {return false;}
		return true;
	}

	public static boolean isProcessRunning(String processName) throws Exception {
		boolean running = false;
		Process process = null;
		process = Runtime.getRuntime().exec("ps");
		BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		while ((line = in.readLine()) != null) {
			if (line.contains(processName)) {
				running = true;
				break;
			}
		}
		in.close();
		process.waitFor();
		return running;
	}

	public static boolean hasRootPermission() {
		Process process = null;
		DataOutputStream os = null;
		boolean rooted = true;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
			if (process.exitValue() != 0) {
				rooted = false;
			}
		} catch (Exception e) {
			Log.d(MSG_TAG, "hasRootPermission error: " + e.getMessage());
			rooted = false;
		} finally {
			if (os != null) {
				try {
					os.close();
					process.destroy();
				} catch (Exception e) {}
			}
		}
		return rooted;
	}

	public static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.d(MSG_TAG, "Unexpected error - Here is what I know: " + e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {}
		}
		return true;
	}

	public static boolean setProp(String property, String value) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("setprop " + property + " " + value + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {}
		}
		return true;
	}

	public static String getWifiIp(Context context) {
		WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int ip = mWifiManager.getConnectionInfo().getIpAddress();
		return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
	}

	public static void enableWiFi(Context context, boolean enable) {
		if (enable) {
			Toast.makeText(context, R.string.Turning_on_wifi, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(context, R.string.Turning_off_wifi, Toast.LENGTH_LONG).show();
		}
		WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mWifiManager.setWifiEnabled(enable);
	}

	public static boolean checkWifiState(Context context) {
		try {
			WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
			if (!mWifiManager.isWifiEnabled() || wifiInfo.getSSID() == null) {
				return false;
			}

			return true;
		} catch (Exception e) {return false;}
	}

	public static void showNotification(Context context, int icon, String text) {
		final Notification notifyDetails = new Notification(icon, text, System.currentTimeMillis());
		notifyDetails.flags = Notification.FLAG_ONGOING_EVENT;

		if (prefsSound(context)) {
			notifyDetails.defaults |= Notification.DEFAULT_SOUND;
		}
		
		if (prefsVibrate(context)) {
			notifyDetails.defaults |= Notification.DEFAULT_VIBRATE;
		}
		
		Intent notifyIntent = new Intent(context, adbWireless.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(context, 0, notifyIntent, 0);
		notifyDetails.setLatestEventInfo(context, context.getResources().getString(R.string.noti_title), text, intent);
		mNotificationManager.notify(START_NOTIFICATION_ID, notifyDetails);
	}

	public void showHelpDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.about)).setCancelable(true).setPositiveButton(getString(R.string.button_close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		});
		builder.setIcon(R.drawable.icon);
		builder.create();
		builder.setTitle(R.string.app_name);
		builder.show();
	}

	public static boolean prefsVibrate(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getResources().getString(R.string.pref_vibrate_key), true);
	}

	public static boolean prefsSound(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getResources().getString(R.string.pref_sound_key), true);
	}

	public static boolean prefsNoti(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getResources().getString(R.string.pref_noti_key), true);
	}

	public static boolean prefsHaptic(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getResources().getString(R.string.pref_haptic_key), true);
	}

	public static boolean prefsWiFiOn(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getResources().getString(R.string.pref_wifi_on_key), false);
	}

	public static boolean prefsWiFiOff(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getResources().getString(R.string.pref_wifi_off_key), true);

	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	// Do nothing, force them to hit the home button so it does not close the application.
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
}