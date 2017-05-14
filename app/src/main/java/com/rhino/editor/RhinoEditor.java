package com.rhino.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.IOException;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import android.app.Activity;
import android.content.DialogInterface;
import android.view.Window;

public class RhinoEditor
{
	public static final String RD_ICON="Rhino_icon.png";
	public static final float alpha=(float) 0.77;
	public static final String TAG="RhinoEditor";
	public static final int buttonBounds=200;
	
	private static Context octx;
	private static Context ctx;
	private static AssetManager am;
	private static LinearLayout floatLayout;
	private static WindowManager.LayoutParams floatParams;
	private static WindowManager windowManager;
	private static Button tempButton;
	private static int floatx=0;
	private static int floaty=0;
	private static int StatesBarHeight;

	private static void createFloatView() {
		floatParams=new WindowManager.LayoutParams();
		windowManager=(WindowManager) ctx.getSystemService(ctx.WINDOW_SERVICE);
		floatParams.type=LayoutParams.TYPE_PHONE;
		floatParams.format=PixelFormat.RGBA_8888; 
		floatParams.flags=LayoutParams.FLAG_NOT_FOCUSABLE;      
		floatParams.gravity=Gravity.LEFT | Gravity.TOP;       
		floatParams.x=floatx;
		floatParams.y=floaty;
		floatParams.width=LayoutParams.WRAP_CONTENT;
		floatParams.height=LayoutParams.WRAP_CONTENT;
		
		floatLayout=new LinearLayout(ctx);
		windowManager.addView(floatLayout, floatParams);
		tempButton=new Button(ctx);
		Bitmap image=null;
		try {
			image=BitmapFactory.decodeStream(am.open(RD_ICON));
		} catch (IOException e) {
			e.printStackTrace();
		}
		tempButton.setBackground(new BitmapDrawable(image));
		tempButton.setWidth(buttonBounds);
		tempButton.setHeight(buttonBounds);
		tempButton.setAlpha(alpha);
		floatLayout.addView(tempButton);
        floatLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		tempButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				floatx=(int) event.getRawX()-tempButton.getMeasuredWidth()/2;
				floaty=(int) event.getRawY()-tempButton.getMeasuredHeight()/2 - StatesBarHeight;
				floatParams.x=floatx;
				floatParams.y=floaty;
				windowManager.updateViewLayout(floatLayout, floatParams);
				return false;
			}
		});	
		tempButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				windowManager.removeView(floatLayout);
				createEditDialog();
			}
		});
	}
	
	private static void createEditDialog() {
		AlertDialog.Builder builder=new AlertDialog.Builder(ctx);
		builder.setTitle("执行代码");
		final EditText edit=new EditText(ctx);
		edit.setHint("代码");
		builder.setView(edit);
		builder.setCancelable(true);
		builder.setPositiveButton("执行", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
				JAVAScriptEngine.runJS(edit.getText().toString());
				createFloatView();
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				createFloatView();
			}
		});
		AlertDialog dialog=builder.create();
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		dialog.show();
	}
	
	public static void create(Context context) {
		octx=context;
		ctx=context.getApplicationContext();
		am=ctx.getAssets();
		StatesBarHeight=getStatesBarHeight();
		try {
		createFloatView();
		} catch (Exception e) {
			Toast.makeText(ctx, e.toString(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private static int getStatesBarHeight() {
		int result=0;
		int resourceId=ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) result=ctx.getResources().getDimensionPixelSize(resourceId);
		return result;
	}
	
	static class JSHostFunction extends ImporterTopLevel
	{
		private String name;
		public String[] fc={};

		public JSHostFunction(String n) {
			this.name=n;
			initFunction();
		}

		public String getName() {
			return this.name;
		}

		public void setName(String n) {
			this.name=n;
		}

		public void initFunction() {
			String[] con={"print","tc","exit"};
			fc=new String[]{};
			add(con);
		}

		private void add(String[] he) {
			String[] hee=fc;
			fc=new String[hee.length+he.length];
			System.arraycopy(hee, 0, fc, 0, hee.length);
			System.arraycopy(he, 0, fc, 0, he.length);
		}

		public String[] getHostFunction() {
			return fc;
		}

		/*
		 hehe
		 hehe
		 hehe
		 hehe
		 hehe
		 hehe
		 hehe
		 hehe
		 hehe
		 hehe*/

		public void print(Object what) {
			Toast.makeText(ctx, what.toString(), Toast.LENGTH_SHORT).show();
		}

		public void tc(String title, String msg) {
			alert(title, msg);
		}

		public void exit() {
			System.exit(0);
		}
	}
	
	static class JAVAScriptEngine
	{
		private static org.mozilla.javascript.Context cx=null;
		private static ScriptableObject scope=null;
		private static JSHostFunction host;
		
		public static void init() {
			try {
				cx=org.mozilla.javascript.Context.enter();
				cx.setOptimizationLevel(-1);
				host=new JSHostFunction(ctx.getApplicationContext().getString(R.string.app_name));
				scope=cx.initStandardObjects(host, false);
				scope.defineFunctionProperties(host.getHostFunction(), JSHostFunction.class, ScriptableObject.DONTENUM);
			} catch (Exception e) {
				err(e);
			}
		}

		public static void runJS(final String content) {
			try {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Looper.prepare();
							init();
							cx.evaluateString(scope, content, host.getName(), 1, null);
							flush();
						} catch (Exception e) {
							alert("错误！", e.getMessage());
							Log.e(TAG, e.toString());
						}
					}
				}).start();
			} catch (Exception e) {
				alert("错误！", e.getMessage());
				err(e);
			}
		}

		public static void flush() {
			cx.exit();
		}

		public static Scriptable getScope() {
			return scope;
		}
	}
	
	private static void err(Exception e) {
		Log.e(TAG, e.toString());
	}
	
	private static void alert(final String title, final String msg) {
		((Activity) octx).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder b=new AlertDialog.Builder(ctx);
				b.setTitle(title);
				b.setMessage(msg);
				b.setPositiveButton("确定", null);
				AlertDialog di=b.create();
				di.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
				di.show();
			}
		});
	}
}
