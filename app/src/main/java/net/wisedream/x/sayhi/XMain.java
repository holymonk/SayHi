package net.wisedream.x.sayhi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.newInstance;

/**
 * Created by <a href="mailto:manwu91@gmail.com">monk</a> on 2019-11-27.
 */
public class XMain implements IXposedHookLoadPackage {
    static final String process = "com.tencent.mm";
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(process.equals(lpparam.processName)){
            HideXposed.hideModule(lpparam.classLoader);
            XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", lpparam.classLoader,
                    "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            final Context context = (Context)param.thisObject;
                            Menu menu = (Menu)param.args[0];
                            MenuItem item = menu.add("SayHi");
                            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    try {
                                        String verName = context.getPackageManager().getPackageInfo("com.tencent.mm", 0).versionName;
                                        if (!isVersionSupported(verName)) {
                                            Toast.makeText(context, "SayHi不支持当前版本: " + verName, Toast.LENGTH_SHORT).show();
                                            return true;
                                        }
                                    }catch (Exception e){
                                        return true;
                                    }
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true);
                                    builder.setTitle("对方wxid:");
                                    final EditText etInput = new EditText(context);
                                    builder.setView(etInput);
                                    builder.setPositiveButton("SayHi", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String wxid = etInput.getText().toString().trim();
                                            Log.d("xposed", "say hi to " + wxid);
                                            sayHi(wxid, lpparam.classLoader);
                                        }
                                    });
                                    builder.show();
                                    return true;
                                }
                            });
                        }
                    });
        }
    }
    private static boolean isVersionSupported(String verName){
        return "7.0.6".equals(verName);
    }

    private static void sayHi(String wxid, ClassLoader cl){
        if(wxid==null || wxid.length()<3){
            return;
        }
        Class msgRequestClz = findClass("com.tencent.mm.modelmulti.h", cl);
        Object msgRequest = newInstance(msgRequestClz,
                wxid, // 接收者wxid
                "Hi!", // 消息内容
                1, // 消息type, 文本 1
                0,
                new HashMap()
        );
        Object requestSenderObj = callStaticMethod(findClass("com.tencent.mm.kernel.g", cl), "Vs");
        callMethod(requestSenderObj, "a", msgRequest, 0);
    }

}
