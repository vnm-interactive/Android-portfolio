package RemoteCtrl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import oscP5.OscEventListener;
import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public abstract class BaseActivity extends Activity {
	//
	public abstract void setLayout(int layoutId);

	protected String getAppAboutMe() {
		return "�����ƶ�����ƽ̨����Ȩ���У����չʾ(2011-2013)";
	}

	protected int getClientCount() {
		return 1;
	}

	// �������ip��ַ�����ı�ʱ����
	protected void onRemoteIpChanged(int idx) {
		// TODO Auto-generated method stub
	}

	private static final String LOGTAG = "CRE";

	/** Logging functions to generate ADB logcat messages. */

	public static final void LOGE(String nMessage) {
		Log.e(LOGTAG, nMessage);
	}

	public static final void LOGW(String nMessage) {
		Log.w(LOGTAG, nMessage);
	}

	public static final void LOGD(String nMessage) {
		Log.d(LOGTAG, nMessage);
	}

	public static final void LOGI(String nMessage) {
		Log.i(LOGTAG, nMessage);
	}

	final static Drawable transparent_drawable = new ColorDrawable(
			Color.TRANSPARENT);

	protected void sendExceptionToVinjn(Exception e, String mail_content) {
		String str_trace = e.getMessage() + "\n\n";
		StackTraceElement[] traces = e.getStackTrace();
		for (StackTraceElement tr : traces)
			str_trace += tr.toString() + "\n";

		sendEmail("���ʹ�����Ϣ��������", "vinjn.z@gmail.com", "[�쳣��־]"
				+ this.getClass().toString(), mail_content + "\n\n" + str_trace);
	}

	void sendEmail(String chooser_title, String mail_reciever,
			String mail_title, String mail_content) {
		// http://stackoverflow.com/questions/2197741/how-to-send-email-from-my-android-application
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { mail_reciever });
		i.putExtra(Intent.EXTRA_SUBJECT, mail_title);
		i.putExtra(Intent.EXTRA_TEXT, mail_content);
		try {
			startActivity(Intent.createChooser(i, chooser_title));
		} catch (android.content.ActivityNotFoundException ex) {
			MsgBox("û�а�װ�ʼ��ͻ���.", true);
		}
	}

	//
	OscP5 server;

	String STORE_NAME = "Settings";

	protected String remote_ips[];
	final int listen_port = 7001;
	protected final int client_port = 7000;

	final int IdeaW = 1280;
	final int IdeaH = 752;
	int DeviceW = 1280;
	int DeviceH = 752;

	protected AbsoluteLayout main_layout;

	Map<Integer, Integer> slider_map = new HashMap<Integer, Integer>();

	final int MSG_BLOCK = 0;
	final int MSG_UNBLOCK = 1;
	final int MSG_MSGBOX = 2;

	// Define the Handler that receives messages from the thread and update the
	// progress
	final Handler default_handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UNBLOCK:
				waiting_dialog.cancel();
				break;
			case MSG_BLOCK:
				waiting_dialog.show();
				break;
			case MSG_MSGBOX:
				Bundle bundle = msg.getData();
				if (bundle != null) {
					String info = bundle.getString("info");
					boolean short_one = bundle.getBoolean("short_one");
					MsgBox_(info, short_one);
				}
			default:
				break;
			}
		}
	};

	// Define the Handler that receives messages from the thread and update the
	// progress
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_UNBLOCK)
				waiting_dialog.cancel();
			else if (msg.what == MSG_BLOCK)
				waiting_dialog.show();
		}
	};

	// idea coord -> device coord
	int toDx(int ideaX) {
		return ideaX * DeviceW / IdeaW;
	}

	int toDy(int ideaY) {
		return ideaY * DeviceH / IdeaH;
	}

	public void sendCmd(String addr, int value) {
		OscMessage m = new OscMessage(addr);
		m.add(value);
		// server.send(m);
		for (String ip : remote_ips)
			server.send(m, ip, client_port);
	}
	
	/**
	 * @param id
	 */
	protected void removeView(int id)
	{
		main_layout.removeView(findViewById(id));
	}

	ProgressDialog waiting_dialog;

	protected static final int NO_OSC_CMD = -1;

	protected void setButtonClicked(int img_id) {
		View btn = findViewById(img_id);
		if (btn != null) {
			btn.setSelected(true);
			btn.setBackgroundResource(img_id);
		}
	}

	protected EditText addImageEditText(final int img, int x, int y,
			int offset_x, int offset_y) {

		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), img, o);
		int w = bmp.getWidth();
		int h = bmp.getHeight();

		x = toDx(x);
		y = toDy(y);
		w = toDx(w);
		h = toDy(h);

		ImageView imgView = new ImageView(this);
		imgView.setImageBitmap(bmp);
		main_layout.addView(imgView,
				new AbsoluteLayout.LayoutParams(w, h, x, y));

		EditText input = new EditText(this);
		input.setId(img);

		main_layout.addView(input);

		input.getBackground().setAlpha(0);
		input.setLayoutParams(new AbsoluteLayout.LayoutParams(w, h, x
				+ offset_x, y + offset_y));

		return input;
	}

	// ���Ӱ�ť���޳����л�
	protected ImageButton addButton(final String addr, final int osc_value,
			int x, int y, final int img_on, final int img) {
		return addButton(addr, osc_value, x, y, -1, -1, img_on, img, -1, null);
	}

	// ���Ӱ�ť���г����л�
	protected ImageButton addButton(final String addr, final int osc_value,
			int x, int y, final int img_on, final int img, final int new_layout) {
		return addButton(addr, osc_value, x, y, -1, -1, img_on, img,
				new_layout, null);
	}

	// ͨ������Ҫֱ�ӵ���
	protected ImageButton addButton(final String addr, final int osc_value,
			int x, int y, int w, int h, final int img_on, final int img,
			final int new_layout, final OnClickListener bonus_listener) {
		ImageButton btn = new ImageButton(this);
		btn.setId(img_on);
		main_layout.addView(btn);

		if (w <= 0 || h <= 0) {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
			Bitmap bmp = BitmapFactory
					.decodeResource(getResources(), img_on, o);
			w = bmp.getWidth();
			h = bmp.getHeight();
		}

		x = toDx(x);
		y = toDy(y);
		w = toDx(w);
		h = toDy(h);

		btn.setLayoutParams(new AbsoluteLayout.LayoutParams(w, h, x, y));

		btn.setTag(new Integer(img));
		btn.setBackgroundResource(img);

		btn.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_MOVE:
					v.setBackgroundResource(img_on);
					break;
				default:
					if (!v.isSelected())
						v.setBackgroundResource(img);
					break;
				}
				return false;
			}
		});

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (osc_value >= 0)
					sendCmd(addr, osc_value);

				// #3 test for main buttons
				if (new_layout >= 0) {
					setLayout(new_layout);
				} else {
					// #1 reset other buttons in the same group
					int n = main_layout.getChildCount();

					for (int i = 0; i < n; i++) {
						View child = main_layout.getChildAt(i);
						if (child.getId() != img_on) {
							child.setSelected(false);
							Integer tag = (Integer) child.getTag();
							if (tag != null)
								child.setBackgroundResource(tag.intValue());
						}
					}

					// #2 set current button
					v.setSelected(true);
					v.setBackgroundResource(img_on);
					v.bringToFront();
				}

				if (bonus_listener != null)
					bonus_listener.onClick(v);
			}
		});
		return btn;
	}

	protected ImageButton addToggleButton(final String addr, final int osc1,
			final int osc2, int x, int y, final int img1, final int img2) {
		ImageButton btn = addButton(addr, 0, x, y, img1, img2);

		btn.setOnTouchListener(null);

		// btn.setOnTouchListener(new OnTouchListener() {
		// public boolean onTouch(View v, MotionEvent event) {
		// switch (event.getAction()) {
		// case MotionEvent.ACTION_DOWN:
		// case MotionEvent.ACTION_MOVE:
		// v.setBackgroundResource(img_on);
		// break;
		// default:
		// if (!v.isSelected())
		// v.setBackgroundResource(img);
		// break;
		// }
		// return false;
		// }
		// });

		btn.setTag(new Boolean(true));
		btn.setBackgroundResource(img1);

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Boolean tag = (Boolean) v.getTag();

				if (tag.booleanValue()) {
					v.setBackgroundResource(img2);
					sendCmd(addr, osc1);

				} else {
					v.setBackgroundResource(img1);
					sendCmd(addr, osc2);
				}
				v.setTag(new Boolean(!tag.booleanValue()));
				v.bringToFront();
			}
		});
		return btn;
	}

	// ����ͼƬ
	public ImageView addImage(int x, int y, final int img) {
		return addImage(x, y, -1, -1, img);
	}

	protected ImageView addImage(int x, int y, int w, int h, final int img) {
		ImageView v = new ImageView(this);
		v.setId(img);
		v.setImageResource(img);
		main_layout.addView(v);

		if (w <= 0 || h <= 0) {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), img, o);
			w = bmp.getWidth();
			h = bmp.getHeight();
		}

		x = toDx(x);
		y = toDy(y);
		w = toDx(w);
		h = toDy(h);

		v.setLayoutParams(new AbsoluteLayout.LayoutParams(w, h, x, y));

		return v;
	}

	// maxΪ100��ȡֵ��0��100��defaultValueΪ0
	// yΪ����������λ�õ�y���꣬��һ����button�ǲ�ͬ��
	// remember_position��Ϊfalse��������λ��
	protected SeekBar addSlider(final String addr, int x, int y, int w, int h,
			final int slider_bg, final int thumb_res) {
		return addSlider(addr, 100, 0, true, x, y, w, h, slider_bg, thumb_res,
				false, false);
	}

	protected SeekBar addSlider(
			final String addr,
			int max,
			int defaultValue,
			final boolean forward, // if
			// 0->100
			int x, int y, int w, int h, final int slider_bg,
			final int thumb_res, final boolean remember_position,
			final boolean downside_thumb) {
		SeekBar bar = new SeekBar(this);
		bar.setId(slider_bg + x + y + w + h);
		main_layout.addView(bar);

		int thumb_w = 0;
		int thumb_h = 0;
		if (thumb_res == 0) {
			// bar.setThumb(null);
		} else {
			BitmapDrawable thumb = (BitmapDrawable) getResources().getDrawable(
					thumb_res);
			thumb_w = thumb.getBitmap().getWidth();
			thumb_h = thumb.getBitmap().getHeight();

			bar.setThumb(getResources().getDrawable(thumb_res));
		}

		x = toDx(x);
		if (downside_thumb)
			y = toDy(y - (Math.max(h / 2 ,thumb_h / 2)));
		else
			y = toDy(y - (h / 2 + thumb_h / 2));
		w = toDx(w);
		if (downside_thumb)
			h = toDy(h + thumb_h);
		else
			h = toDy(h + thumb_h / 3);

		// bar.setPadding(bar.getPaddingLeft(), bar.getPaddingTop(),
		// bar.getPaddingRight(), bar.getPaddingBottom());
		bar.setLayoutParams(new AbsoluteLayout.LayoutParams(w, h, x, y));

		bar.setThumbOffset(thumb_w / 4);
		// bar.setBackgroundDrawable(null);
		bar.setMax(max);

		final Integer key = new Integer(bar.getId());
		if (slider_map.containsKey(key)) {
			int v = slider_map.get(key).intValue();
			if (remember_position)
				bar.setProgress(v);
		} else {
			bar.setProgress(defaultValue);
		}

		LayerDrawable progressDrawable = (LayerDrawable) bar
				.getProgressDrawable();
		for (int i = 0; i < progressDrawable.getNumberOfLayers(); i++) {

			int layerId = progressDrawable.getId(i);
			switch (layerId) {
			case android.R.id.background:// ���ý���������
			case android.R.id.secondaryProgress:// ���ö���������
				progressDrawable.setDrawableByLayerId(layerId,
						transparent_drawable);
				break;
			case android.R.id.progress:// ���ý�����
				// http://stackoverflow.com/questions/7141469/android-seekbar-set-custom-style-using-nine-patch-images
				Drawable drawable = getResources().getDrawable(slider_bg);
				ClipDrawable proDrawable = new ClipDrawable(drawable,
						Gravity.LEFT, ClipDrawable.HORIZONTAL);
				progressDrawable.setDrawableByLayerId(layerId, proDrawable);
				break;
			default:
				break;
			}
		}

		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					slider_map.put(key, progress);
					if (!forward)
						progress = 100 - progress;
					sendCmd(addr, progress);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		return bar;
	}

	// public void oscEvent(OscMessage m) {
	// LOGI(" addr: " + m.addrPattern());
	// int block = -1;
	// if (m.checkAddrPattern("/block")) {
	// block = m.get(0).intValue();
	// } else if (m.checkAddrPattern("/deactivate")) {
	// block = 1;
	// } else if (m.checkAddrPattern("/activate")) {
	// block = 0;
	// }
	//
	// if (block == 1) {
	// handler.sendEmptyMessage(MSG_BLOCK);
	// final int MS_TIMEOUT = 7000;
	// handler.sendEmptyMessageDelayed(MSG_UNBLOCK, MS_TIMEOUT);
	// } else if (block == 0) {
	// // remove delayed messages
	// handler.removeMessages(MSG_UNBLOCK);
	// // send the message NOW
	// handler.sendEmptyMessage(MSG_UNBLOCK);
	// }
	// }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		try {
			Object service = getSystemService("statusbar");
			Class<?> statusbarManager = Class
					.forName("android.app.StatusBarManager");
			Method test = statusbarManager.getMethod("collapse");
			test.invoke(service);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void MsgBox(final String info, boolean short_one) {
		Bundle bundle = new Bundle();
		bundle.putString("info", info);
		bundle.putBoolean("short_one", short_one);

		Message msg = new Message();
		msg.what = MSG_MSGBOX;
		msg.setData(bundle);

		default_handler.sendMessage(msg);
	}

	protected void MsgBox_(final String info, boolean short_one) {
		Toast.makeText(BaseActivity.this, info,
				short_one ? Toast.LENGTH_SHORT : Toast.LENGTH_SHORT).show();
	}

	/**
	 * Invoked the first time when the options menu is displayed to give the
	 * Activity a chance to populate its Menu with menu items.
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		for (int i = 0; i < getClientCount(); i++)
			menu.add("����" + (i + 1));
		menu.add("����");
		return true;
	}

	/** Invoked when the user selects an item from the Menu */
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().length() == 3) {
			showInputBox(item.getTitle().charAt(2) - '1');
		} else if (item.getTitle() == "����") {
			MsgBox(getAppAboutMe(), false);
		}

		return true;
	}

	private void showInputBox(final int idx) {
		assert (idx >= 0 && idx < getClientCount());
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("");
		if (getClientCount() > 1)
			alert.setMessage("����Զ��IP " + (idx + 1) + " ��ַ");
		else
			alert.setMessage("����Զ��IP��ַ");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText(remote_ips[idx]);
		input.setInputType(InputType.TYPE_CLASS_PHONE);

		alert.setView(input);

		alert.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				remote_ips[idx] = value;
				// MsgBox("Զ��IP����Ϊ " + value, true);
				SharedPreferences settings = getSharedPreferences(STORE_NAME,
						MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("client_ip" + idx, value);
				editor.commit();
				onRemoteIpChanged(idx);
			}
		});

		alert.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
		// see
		// http://androidsnippets.com/prompt-user-input-with-an-alertdialog
	}

   	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        
        // fix potential udp connection bug
		server.stop();
	}

    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// hide title bar
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// hide status bar
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		// always light on
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		DeviceW = metrics.widthPixels;
		DeviceH = metrics.heightPixels;

		if (DeviceW == 1280)
			DeviceH = 752;

		waiting_dialog = new ProgressDialog(this);
		{
			waiting_dialog.setTitle("�ȴ�Զ�̼������Ӧ");
			waiting_dialog.setMessage("���Ժ�");
			waiting_dialog.setIndeterminate(true);
			waiting_dialog.setCancelable(false);
		}

		remote_ips = new String[getClientCount()];

		// preference
		SharedPreferences settings = getSharedPreferences(STORE_NAME,
				MODE_PRIVATE);
		for (int i = 0; i < getClientCount(); i++)
			remote_ips[i] = settings.getString("client_ip" + i, "192.168.1.10"
					+ i);
		MsgBox("Զ��IPΪ " + remote_ips[0] + ",����ѡ��˵����޸�", true);

		server = new OscP5(this, listen_port);
		server.addListener(new OscEventListener() {

			public void oscStatus(OscStatus theStatus) {
			}

			public void oscEvent(OscMessage m) {
				LOGI(" addr: " + m.addrPattern());
				int block = -1;
				if (m.checkAddrPattern("/block")) {
					block = m.get(0).intValue();
				} else if (m.checkAddrPattern("/deactivate")) {
					block = 1;
				} else if (m.checkAddrPattern("/activate")) {
					block = 0;
				}

				if (block == 1) {
					handler.sendEmptyMessage(MSG_BLOCK);
                    // remove delayed messages
					handler.removeMessages(MSG_UNBLOCK);
                    // send the delayed message
					final int MS_TIMEOUT = 7000;
					handler.sendEmptyMessageDelayed(MSG_UNBLOCK, MS_TIMEOUT);
				} else if (block == 0) {
					// remove delayed messages
					handler.removeMessages(MSG_UNBLOCK);
					// send the message NOW
					handler.sendEmptyMessage(MSG_UNBLOCK);
				}
			}
		});
	}
}