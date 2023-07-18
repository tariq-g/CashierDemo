package com.wiseasy.cashier.demo;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import junit.framework.TestCase;

public class UiTest extends TestCase {
    public void testA() throws UiObjectNotFoundException {
        // 获取设备对象
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiDevice uiDevice = UiDevice.getInstance(instrumentation);
        // 获取上下文
        Context context = instrumentation.getContext();

        // 启动测试App
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.wiseasy.cashier.demo");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // 打开CollapsingToolbarLayout
        //String btnInitId = "com.wiseasy.cashier.demo:id/btn_init";
        //UiObject btnInit = uiDevice.findObject(new UiSelector().resourceId(btnInitId));
        //btnInit.click();

        String tvInitResultID = "com.wiseasy.cashier.demo:id/tv_init_result";
        UiObject tvInitResult = uiDevice.findObject(new UiSelector().resourceId(tvInitResultID));


        String etAmountID = "com.wiseasy.cashier.demo:id/et_amount";
        UiObject etAmount = uiDevice.findObject(new UiSelector().resourceId(etAmountID));

        String btnPurchaseID = "com.wiseasy.cashier.demo:id/btn_purchase";
        UiObject btnPurchase = uiDevice.findObject(new UiSelector().resourceId(btnPurchaseID));

        String tvTransResultID = "com.wiseasy.cashier.demo:id/tv_prompt";
        UiObject tvTransResult = uiDevice.findObject(new UiSelector().resourceId(tvTransResultID));

        /*while (true){
            String text = tvInitResult.getText();
            if (text.contains("Success")){
                break;
            }
        }*/

        etAmount.setText("1");

        btnPurchase.click();
        while (true){

            for(int i = 0; i < 500;++i){
                String text = tvTransResult.getText();
                if (text.contains("DECLINED,")){
                    break;
                }
            }
            btnPurchase.click();
        }




       /* for (int i = 0; i < 5; i++) {
            // 向上移动
            uiDevice.swipe(uiDevice.getDisplayHeight() / 2, uiDevice.getDisplayHeight(),
                    uiDevice.getDisplayHeight() / 2, uiDevice.getDisplayHeight() / 2, 10);

            // 向下移动
            uiDevice.swipe(uiDevice.getDisplayHeight() / 2, uiDevice.getDisplayHeight() / 2,
                    uiDevice.getDisplayHeight() / 2, uiDevice.getDisplayHeight(), 10);
        }

        // 点击应用返回按钮
        UiObject back = uiDevice.findObject(new UiSelector().description("Navigate up"));
        back.click();

        // 点击设备返回按钮
        uiDevice.pressBack();*/
    }
}
