package com.rhino.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import com.rhino.editor.example.R;
import dalvik.system.DexFile;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.Adapter;
import android.sax.RootElement;
import android.app.ProgressDialog;

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
	private static Node nowNode;
	private static EditText edit;
	private static Node root;
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
	
	public static void print(final String t) {
		((Activity) octx).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private static void createEditDialog() {
		AlertDialog.Builder builder=new AlertDialog.Builder(ctx);
		builder.setTitle("执行代码");
		builder.setCancelable(true);
		LinearLayout layout=new LinearLayout(ctx);
		layout.setOrientation(LinearLayout.VERTICAL);
		Button open=new Button(ctx);
		layout.addView(open);
		open.setText("包视图");
		//nowNode.data.open();
		edit=new EditText(ctx);
		layout.addView(edit);
		edit.setHint("代码");
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
		builder.setView(layout);
		final AlertDialog dialog=builder.create();
		open.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					nowNode.open();
				} catch (Exception e) {
					err(e);
				}
			}
		});
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		dialog.show();
	}
	
	public static void create(Context context) {
		try {
		octx=context;
		ctx=context.getApplicationContext();
		am=ctx.getAssets();
		root=new Node();
		root.data=new ClassInfo();
		root.data.type=ClassInfo.TPACKAGE;
		nowNode=root;
		initPackage();
		StatesBarHeight=getStatesBarHeight();
		createFloatView();
		} catch (Exception e) {
			err(e);
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

		public void print(final Object what) {
			((Activity) octx).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(ctx, what.toString(), Toast.LENGTH_SHORT).show();
				}
			});
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
				Log.e(TAG, e.toString());
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
		//alert("错误！",e.toString());
		Log.e(TAG, "错误！"+e.toString());
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
				di.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				di.show();
			}
		});
	}
	
	static class ClassInfo {
		public static final String TCLASS="类💾";
		public static final String TFIELD="成员⭐";
		public static final String TMETHOD="方法💻";
		public static final String TCONSTRUCTOR="构造方法💡";
		public static final String TPACKAGE="包💼";
		private Object content;
		private String type;
		private String description;
		private String name;
		public ClassInfo() {}
		public ClassInfo(Object acon) {
			this.content=acon;
			update();
		}
		public String getType() {return type;}public void setContent(Object acon) {
			this.content=acon;
			update();
		}
		public String getName() {return name;}
		private void update() {
			if (content instanceof Class) type=TCLASS; else if (content instanceof Field) type=TFIELD; else if (content instanceof Method) type=TMETHOD; else if (content instanceof String) type=TPACKAGE; else if (content instanceof Constructor) type=TCONSTRUCTOR;
			int mod=0;
			boolean q=false;
			switch (type) {
				case TCLASS:
					Class c=(Class) content;
					mod=c.getModifiers();
					name=c.getName();
					break;
				case TFIELD:
					Field f=(Field) content;
					mod=f.getModifiers();
					name=f.getName();
					break;
				case TMETHOD:
					Method m=(Method) content;
					mod=m.getModifiers();
					name=m.getName();
					break;
				case TCONSTRUCTOR:
					Constructor co=(Constructor) content;
					mod=co.getModifiers();
					name=co.getName();
					break;
				case TPACKAGE:
					String s=(String) content;
					description="无";
					q=true;
					name=s;
					break;
			}
			if (!q) description=Modifier.toString(mod);
		}
		public Object get() {return content;}
		@Override public String toString() {
			if (type == TPACKAGE) return add(type," ",name); else return add(type,name);
		};
	}
	
	private static String add(String...a) {
		StringBuilder b=new StringBuilder();
		for (String one : a) {
			b.append(one);
		}
		return b.toString();
	}
	
	private static void initPackage() {
		try {
			nowNode=root;
			String path=ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), 0).sourceDir;
			DexFile dexfile=new DexFile(path);
			Enumeration entries=dexfile.entries();
			while (entries.hasMoreElements()) {
				Node q=null;
				try {
					String n=(String) entries.nextElement();
					q=nowNode.getSon(n);
				} catch (Exception e) {
					err(e);
				}
				try {
					if (q.allname.trim().equals("")) continue;
					if (q == null) continue;
					q.data.setContent(Class.forName(q.allname));
				} catch (Exception e) {
					err(e);
				}
			}
		} catch (Exception e) {
			err(e);
		}
	}
	
	static class Node {
		public Node parent=null;
		public List<Node> son;
		public ClassInfo data=null;
		public String allname;
		public Node() {
			son=new ArrayList<Node>();
			allname="";
		}
		public Node(Node paren, ClassInfo adata) {
			parent=paren;
			son=new ArrayList<Node>();
			parent.addSon(this);
			data=adata;
			if (parent.allname.equals("")) allname=data.getName(); else allname=parent.allname+"."+data.getName();
		}
		public void addSon(Node ason) {
			this.son.add(ason);
		}
		public Node getSingleSon(String aname) {
			for (int i=0;i<son.size();i++) {
				Node t=son.get(i);
				if (t.data.getName().equals(aname)) return t;
			}
			return new Node(this, new ClassInfo(aname));
		}
		public Node getSon(String ason) {
			if (ason.startsWith("org.mozilla.classfile")||ason.startsWith("org.mozilla.javascript")) return null;
			String[] d=ason.split("\\.");
			Node n=root;
			for (String o : d) n=n.getSingleSon(o);
			try {
				n.data.setContent(Class.forName(n.allname));
			} catch (Exception e) {
				err(e);
			}
			return n;
		}
		@Override public String toString() {return data.toString();}
		public void open() {
			switch (data.type) {
				case ClassInfo.TPACKAGE:
					AlertDialog.Builder b=new AlertDialog.Builder(ctx);
					String ti=nowNode.allname;
					if (ti == "") ti="包视图";
					b.setTitle(ti);
					final boolean r=(nowNode.allname == "");
					String[] q;
					int i;
					if (r) {q=new String[son.size()];i=0;} else {q=new String[son.size()+1]; q[0]="/.."; i=1;}
					for (Node one : son) {q[i]=one.data.toString();i++;}
					b.setItems(q, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int position) {
							if (r) {
								nowNode=nowNode.son.get(position);
								nowNode.open();
							} else {
								if (position == 0) {
									nowNode=nowNode.parent;
									nowNode.open();
								} else {
									nowNode=nowNode.son.get(position-1);
									nowNode.open();
								}
							}
						}
					});
					b.setPositiveButton("添加", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int position) {
							edit.setText(edit.getText().toString()+nowNode.allname);
							edit.setSelection(edit.length());
							nowNode=root;
						}
					});
					b.setCancelable(false);
					b.setNegativeButton("取消", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int position) {
							nowNode=root;
						}
					});
					b.setNeutralButton("更多", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int position) {
							String[] all={"创建实例","详情"};
							AlertDialog.Builder w=new AlertDialog.Builder(ctx);
							w.setTitle("更多");
							w.setItems(all, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int po) {
									switch (po) {
										case 1:
											showConfig();
											break;
									}
								}
							});
						}
					});
					AlertDialog d=b.create();
					d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
					d.show();
					break;
				case ClassInfo.TCLASS:
					edit.setText(edit.getText().toString()+nowNode.allname);
					edit.setSelection(edit.length());
					nowNode=root;
					break;
			}
		}
	}
	
	private static void showConfig() {
		String text="";
		switch (nowNode.data.type) {
			case ClassInfo.TCLASS:
				text=add("类名:",nowNode.allname,"\n独名:");
				String[] q=nowNode.allname.split("\\.");
				text=add(text,q[q.length-1],"\n修饰关键词:",nowNode.data.description);
				break;
			case ClassInfo.TPACKAGE:
				text=add("包名:",nowNode.allname,"\n独名:");
				String[] qq=nowNode.allname.split("\\.");
				text=add(text,qq[qq.length-1]);
				break;
		}
		alert("详情", text);
	}
}
