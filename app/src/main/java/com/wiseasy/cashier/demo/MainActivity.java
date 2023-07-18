package com.wiseasy.cashier.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wiseasy.cashier.demo.util.CLog;
import com.wiseasy.cashier.demo.util.LedUtils;
import com.wiseasy.cashier.demo.util.StringUtil;
import com.wiseasy.emvprocess.Acquirer;
import com.wiseasy.emvprocess.KernelInit;
import com.wiseasy.emvprocess.LibInit;
import com.wiseasy.emvprocess.Logger;
import com.wiseasy.emvprocess.Process;
import com.wiseasy.emvprocess.SDKInstance;
import com.wiseasy.emvprocess.TransProcess;
import com.wiseasy.emvprocess.TransResult;
import com.wiseasy.emvprocess.bean.AIDData;
import com.wiseasy.emvprocess.bean.AcquirerKey;
import com.wiseasy.emvprocess.bean.BaseAIDData;
import com.wiseasy.emvprocess.bean.CAPKData;
import com.wiseasy.emvprocess.bean.CAPKRevoke;
import com.wiseasy.emvprocess.bean.CLAMEXAIDData;
import com.wiseasy.emvprocess.bean.CLDiscoverAIDData;
import com.wiseasy.emvprocess.bean.CLJcbAIDData;
import com.wiseasy.emvprocess.bean.CLMirAIDData;
import com.wiseasy.emvprocess.bean.CLPayPassAIDData;
import com.wiseasy.emvprocess.bean.CLPayWaveAIDData;
import com.wiseasy.emvprocess.bean.CLPureAIDData;
import com.wiseasy.emvprocess.bean.CLRupayAIDData;
import com.wiseasy.emvprocess.bean.CLqPBOCAIDData;
import com.wiseasy.emvprocess.bean.CVMItem;
import com.wiseasy.emvprocess.bean.DRLAmexData;
import com.wiseasy.emvprocess.bean.DRLData;
import com.wiseasy.emvprocess.bean.ExceptionPan;
import com.wiseasy.emvprocess.bean.OnlineResult;
import com.wiseasy.emvprocess.bean.OutComeData;
import com.wiseasy.emvprocess.bean.PINSetting;
import com.wiseasy.emvprocess.bean.PaypassTerminalConfig;
import com.wiseasy.emvprocess.bean.PinPadConfig;
import com.wiseasy.emvprocess.bean.TermParamData;
import com.wiseasy.emvprocess.bean.TermParamDisCover;
import com.wiseasy.emvprocess.bean.TerminalSetting;
import com.wiseasy.emvprocess.bean.TrackData;
import com.wiseasy.emvprocess.bean.TransAmount;
import com.wiseasy.emvprocess.bean.TransInfoBean;
import com.wiseasy.emvprocess.common.RspEMVCode;
import com.wiseasy.emvprocess.interfaces.ProcessInterface;
import com.wiseasy.emvprocess.tlv.TlvUtil;
import com.wiseasy.emvprocess.utils.ByteUtil;
import com.wiseasy.emvprocess.utils.EMVUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import wangpos.sdk4.libbasebinder.BankCard;
import wangpos.sdk4.libbasebinder.Core;
import wangpos.sdk4.libbasebinder.HEX;
import wangpos.sdk4.libbasebinder.Version;
import wangpos.sdk4.libkeymanagerbinder.Key;


/* TODO
            devicve id card number
            card information


data -> usecase -> repos -> services
        1. config initialization
        2. reading tags and values, map to data classes

today working on getting a github repo

card number
card exp
name on card
amount
currency

just under it you have a textfield

*/

class TLVDecoder {
    public static void main(String[] args) {
        String tlvData = "01010C48656C6C6F2C20544C5621";

        TLVItem tlvItem = decodeTLV(tlvData);

        System.out.println("Tag: " + tlvItem.getTag());
        System.out.println("Length: " + tlvItem.getLength());
        System.out.println("Value: " + tlvItem.getValue());
    }

    public static TLVItem decodeTLV(String tlvData) {
        String tag = tlvData.substring(0, 2);
        int length = Integer.parseInt(tlvData.substring(2, 4), 16);
        String value = tlvData.substring(4);

        return new TLVItem(tag, length, value);
    }

    public static class TLVItem {
        private String tag;
        private int length;
        private String value;

        public TLVItem(String tag, int length, String value) {
            this.tag = tag;
            this.length = length;
            this.value = hexToString(value);
        }

        public String getTag() {
            return tag;
        }

        public int getLength() {
            return length;
        }

        public String getValue() {
            return value;
        }

        private String hexToString(String hexString) {
            int length = hexString.length();
            byte[] bytes = new byte[length / 2];

            for (int i = 0; i < length; i += 2) {
                bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) +
                        Character.digit(hexString.charAt(i + 1), 16));
            }

            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final String CONFIG_FILE = "/sdcard/" + "/Cashier/";//Environment.getExternalStorageDirectory().getAbsolutePath() + "/Cashier/";

    private TextView mTVCardNo;
    private TextView mTvTransResult;
    private TextView mTvInitResult;
    private TextView mTvPrompt;
    private TextView mTvVersion;
    private EditText mEtAmount,mEtTagType,mEtCountryCode,mEtCurrencyCode;
    private CheckBox cbEnableScript;
    private ProgressBar mPb;
    private int cardType = -1;

    private boolean isInit;
    private String df8116Value;
    /**
     * Card read thread
     */
    private ReadCardThread mReadCardThread;
    /**
     * A marker that indicates whether the reader thread has stopped and is used to exit the loop
     */
    private boolean mIsStopReadCard = false;

    // =============================================== Init ================================================= //
    /**
     * Password keyboard configuration
     */
    private PinPadConfig pinPadConfig;
    /**
     * Password keyboard dialog
     */
    private Dialog pinPadDialog;
    /**
     * 密码键盘的密码显示框
     */
    private TextView mTvPwd;
    /**
     * OutCome
     */
    private OutComeData outCome;
    /**
     * Transaction amount
     */
    private long amount;
    /**
     * Cash back amount
     * - can be null
     */
    private long otherAmount;
    /**
     * pin
     */
    private String pin;
    /**
     * cardNo
     */
    private String cardNo;
    /**
     * transaction result
     */
    private int result;
    /**
     * pinpad Rotation
     * last rotation degree
     */
    private int mLastRotation;
    /**
     * pinpad Rotation
     * OrientationEventListener
     */
    private OrientationEventListener mOrientationEventListener;
    /**
     * pinpad Rotation
     * send rotation command flag,just pinpad prepared it is useful.
     */
    private boolean mAllowSendRotationCommand;

    /**
     * 接触和非接的处理回调接口
     * - OnlineResult processOnline()，处理联机交易，仅能在该回调中调用SDK的方法进行交易相关处理。
     * - 由于交易流程比较复杂，所以将涉及到与SDK交互的所有逻辑、界面处理均放在该接口中处理。根据MVP的设计思想，
     * 界面刷新需要放到View层（即Activity）进行，但是根据本项目的实际情况，放到此处，会使得代码可读性更好，
     * 所以，暂时这么处理。
     * <p>
     * <p>
     * Contact and disconnected processing callback interfaces
     * - OnlineResult processOnline(), For online transactions, only methods of the SDK can be invoked in this callback for transaction related processing.
     * - Due to the complexity of the transaction process, all logic and interface processing involved in the interaction with SDK are processed in this interface.
     * According to the design philosophy of MVP, Interface refresh needs to be carried out in the View layer (Activity), However,
     * depending on the actual situation of this project, it will make the code more readable. So, let's do that for now.
     */
    private ProcessInterface processInterface = new ProcessInterface() {

        @Override
        public void onUIDisplay(int i) {
            Log.i(TAG,"==== Please Tie Card Again ====");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvPrompt.setText("Please Swipe Card Again.");
                }
            });
        }

        /**
         * 设置密码键盘参数
         * - 目前如果是内核调起的脱机PIN密码键盘，那么PINSetting不起作用。
         * - 也可以在TransInfoBean中传入
         * @return PINSetting
         *
         * Set password keyboard parameters
         * - at present, if it is the offline PIN password keyboard set by the kernel, pinsitting does not work.
         * @return PINSetting
         */
        @Override
        public PINSetting ConfigTransTPK() {
            CLog.log(TAG, "ProcessInterface|pinpad|ConfigTransTPK");
            PINSetting pinSetting = new PINSetting();

            /*
                设置了一组密钥的情况
                - 密钥设置详见 {@link InitService#addAcquirerKey(String)}

                A set of keys is set
                - See key Settings {@link InitService#addAcquirerKey(String)}
             */
            pinSetting.setPackageName("com.wiseasy.cashier.demo");

            // 设置了多组密钥的情况：根据不同的处理内核，写不同的包名，就是 InitService#addAcquirerKey() 中三组密钥使用的包名
            // The case where multiple sets of keys are set: depending on the processing kernel, write different package names,
            // which are the package names used by the three sets of keys in InitService#addAcquirerKey()
//            pinSetting.setPackageName("com.wiseasy.cashier" + ".visa");

            pinSetting.setBypass(true);
            pinSetting.setMinPINLength(4);
            pinSetting.setMaxPINLength(6);
            pinSetting.setTimeOut(15);
            pinSetting.setDUKPT(false);
            pinSetting.setClearButtonNotCannel(false);
            pinSetting.setPan("1234567890123456");
            return pinSetting;
        }

        /**
         * 设置金额
         * set transaction amount
         */
        @Override
        public TransAmount setTransAmount() {
            CLog.log(TAG, "ProcessInterface|setTransAmount");
            TransAmount transAmount = new TransAmount();
            transAmount.setAmount(amount);
            return transAmount;
//            return null;
        }

        /**
         * 联机流程
         * The online process.
         */
        @Override
        public OnlineResult processOnline() {
            CLog.log(TAG, "ProcessInterface|processOnline");

            byte[] macData = new byte[128];
            int[] macLen = new int[1];

            try {


                SDKInstance.mEmvCore.getTLV(0x9F24,macData,macLen);
                Logger.log(TAG, "ProcessInterface|gettlv 0x9f24 "  +ByteUtil.bytes2HexString(Arrays.copyOf(macData,macLen[0])));

                SDKInstance.mEmvCore.getTLV(0xDF7C,macData,macLen);
                Logger.log(TAG, "ProcessInterface|gettlv 0xDF7C "  +ByteUtil.bytes2HexString(Arrays.copyOf(macData,macLen[0])));
                SDKInstance.mEmvCore.getTLV(0xdf15, macData, macLen);
                SDKInstance.mEmvCore.getTLV(0x5A, macData, macLen);
                SDKInstance.mEmvCore.getTLV(0x5F20, macData, macLen);
                SDKInstance.mEmvCore.getTLV(0x5F240, macData, macLen);
                CLog.log(TAG, "processContactLess| 0xdf15 result=" + ByteUtil.bytes2HexString(macData));
                CLog.log(TAG, "processContactLess| 0xdf15 tmplen=" + macLen[0]);
                int mac = SDKInstance.mCore.getMac(new byte[16],16,macData, macLen);
                Logger.log(TAG, "ProcessInterface|getMac " + mac +ByteUtil.bytes2HexString(Arrays.copyOf(macData,macLen[0])));
            } catch (RemoteException e) {
                e.printStackTrace();
            }



//            test();

            // 准备联机交易数据      Prepare online transaction data
            int result = onlineProcess();

            // 向内核返回需要的数据      Return the required data to the kernel
            // TODO 需要添加注释说明
            OnlineResult onlineResult = new OnlineResult();
            onlineResult.setTransStatus(Process.ONLINE_APPROVAL);
//            onlineResult.setTransStatus(Process.ONLINE_FAILED);
//            onlineResult.setTransStatus(3);
            onlineResult.setDE39("00");
//            onlineResult.setDE38("111111");
            onlineResult.setTag91Data("01020304050607080000000000000000");// Issuer Authentication Data
//            onlineResult.setTag71Data("860d842400000834d0ef01638732de");//Issuer Script Command
//            onlineResult.setTag71Data("9F180441424344860D841E0000084F3B8A34F2CA5C0E860D841E00000825ED3CB34A0605A8860D841E0000086578F896402D6DED860D841E00000824754DBF9E5D77E0860D841E000008DDE5432BE9D765D2860D841E000008A50C770A452768A9860D841E0000089772AA666CFB71B6");//Issuer Script Command
//            onlineResult.setTag72Data("9F180400000001860F0CDADF040A8E081122334455667788");//Issuer Script Command
            if(cbEnableScript.isChecked()){
                //onlineResult.setTag91Data("f7b7151094a92f18001221");// Issuer Authentication Data
                onlineResult.setTag71Data("860d842400000834d0ef01638732de");//Issuer Script Command
//            onlineResult.setTag71Data("9F180441424344860D841E0000084F3B8A34F2CA5C0E860D841E00000825ED3CB34A0605A8860D841E0000086578F896402D6DED860D841E00000824754DBF9E5D77E0860D841E000008DDE5432BE9D765D2860D841E000008A50C770A452768A9860D841E0000089772AA666CFB71B6");//Issuer Script Command
//            onlineResult.setTag72Data("1234567890");//Issuer Script Command
            }
            return onlineResult;
        }

        /**
         * 内核advice流程
         * The kernel process
         */
        @Override
        public void processAdvice(byte[] adviceData) {
            CLog.log(TAG, "ProcessInterface|processAdvice");
        }

        /**
         * 处理脱机pin校验结果
         * Process offline pin verification results
         */
        @Override
        public void displayOfflinePinVerifyResult() {
            CLog.log(TAG, "ProcessInterface|pinpad|displayOfflinePinVerifyResult");

            // TODO: 2019/5/14 脱机PIN校验

        }

        /**
         * 多应用选择
         * Multiple application selection
         */
        @Override
        public int processMutilAID(final String[] AIDList) {
            CLog.log(TAG, "ProcessInterface|processMultiAID");
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final AtomicInteger selectItem = new AtomicInteger(0);
            final boolean[] choice = {false};
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(MainActivity.this).setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            choice[0] = false;
                            mTvPrompt.setText("process breakOffCommand");
                            try {
                                SDKInstance.mBankCard.breakOffCommand();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            countDownLatch.countDown();
                            return false;
                        }
                    }).setSingleChoiceItems(AIDList, 0, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    choice[0] = true;
                                    selectItem.set(which);
                                    dialog.dismiss();
                                    countDownLatch.countDown();
                                }
                            }).show();
                }
            });

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (choice[0] == false){
                return -1;
            }else {
                return selectItem.get();
            }

        }



        /**
         * 获取PinPadButton
         * get pinPadButton
         */
        @Override
        public PinPadConfig getPINPADButton() {
            CLog.log(TAG, "ProcessInterface|pinpad|getPINPADButton");
            return pinPadConfig;
        }

        /**
         * 显示密码输入框
         * show password dialog
         */
        @Override
        public void displayPINPAD() {
            CLog.log(TAG, "ProcessInterface|pinpad|displayPINPAD");
            final CountDownLatch countDownLatch = new CountDownLatch(1);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (pinPadDialog == null) {
                        pinPadDialog = new Dialog(MainActivity.this, R.style.BaseDialog);
                        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_pin, null);
                        pinPadConfig = new PinPadConfig();
                        pinPadConfig.btn1 = view.findViewById(R.id.button1);
                        pinPadConfig.btn2 = view.findViewById(R.id.button2);
                        pinPadConfig.btn3 = view.findViewById(R.id.button3);
                        pinPadConfig.btn4 = view.findViewById(R.id.button4);
                        pinPadConfig.btn5 = view.findViewById(R.id.button5);
                        pinPadConfig.btn6 = view.findViewById(R.id.button6);
                        pinPadConfig.btn7 = view.findViewById(R.id.button7);
                        pinPadConfig.btn8 = view.findViewById(R.id.button8);
                        pinPadConfig.btn9 = view.findViewById(R.id.button9);
                        pinPadConfig.btn0 = view.findViewById(R.id.button0);
                        pinPadConfig.btncancel = view.findViewById(R.id.btn_cancel);
                        pinPadConfig.btnconfirm = view.findViewById(R.id.btn_confirm);
                        pinPadConfig.btnclean = view.findViewById(R.id.btn_clean);
                        mTvPwd = view.findViewById(R.id.tv_pwd);
                        Window dialogWindow = pinPadDialog.getWindow();
                        if (dialogWindow != null) {
                            dialogWindow.setGravity(Gravity.CENTER);
                            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                            lp.x = 0;
                            lp.y = 0;
                            view.measure(0, 0);
                            lp.height = view.getMeasuredHeight();
                            lp.alpha = 9f;
                            dialogWindow.setAttributes(lp);
                            pinPadDialog.setContentView(view);
                            pinPadDialog.show();
                        }

                        mLastCount = 0;
                    }

                    updatePINPAD(mLastCount);
                    countDownLatch.countDown();
                }
            });

            try {
                countDownLatch.await();
                mAllowSendRotationCommand = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * 刷新布局
         * - 接收到内核返回的混淆后的数字，重新显示到界面上，即「随机数字按键位置」；
         *
         * The refresh layout
         * - receive the confused number returned by the kernel and display it on the interface again, that is, "random number key position";
         */
        @Override
        public void refreshPINPADLayout(final int pinType, int offlinePinTotalTryTime, int offlinePinRemainTryTime, final String numberOrder) {
            CLog.log(TAG, "ProcessInterface|pinpad|relayoutPINPAD, pinType=" + pinType + ", offlinePinTotalTryTime=" + offlinePinTotalTryTime + ", offlinePinRemainTryTime=" + offlinePinRemainTryTime + ", numberOrder=" + numberOrder);

            final CountDownLatch countDownLatch = new CountDownLatch(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {

                        mTvPwd.setText("");

                        if (pinType == 1) {
                            mTvPwd.setHint("Please input online pin");
                        } else if (pinType == 2) {
                            mTvPwd.setHint("Please input offline pin");
                        }

                        pinPadConfig.btn1.setText(numberOrder.charAt(0) + "");
                        pinPadConfig.btn2.setText(numberOrder.charAt(1) + "");
                        pinPadConfig.btn3.setText(numberOrder.charAt(2) + "");
                        pinPadConfig.btn4.setText(numberOrder.charAt(3) + "");
                        pinPadConfig.btn5.setText(numberOrder.charAt(4) + "");
                        pinPadConfig.btn6.setText(numberOrder.charAt(5) + "");
                        pinPadConfig.btn7.setText(numberOrder.charAt(6) + "");
                        pinPadConfig.btn8.setText(numberOrder.charAt(7) + "");
                        pinPadConfig.btn9.setText(numberOrder.charAt(8) + "");
                        pinPadConfig.btn0.setText(numberOrder.charAt(9) + "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    countDownLatch.countDown();
                }
            });

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * 刷新布局
         * - 接收到按键按下的指令，根据已经输入的密码长度，刷新密码输入框的*位数；
         *
         * The refresh layout
         * - receive the instruction pressed by the key and refresh the * digits in the password input box according to the password length already entered;
         */
        @Override
        public void updatePINPAD(final int count) {
            CLog.log(TAG, "ProcessInterface|pinpad|updatePINPAD, The length of the password is " + count);
            mLastCount = count;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < mLastCount; i++) {
                        stars.append("●");
                    }
                    mTvPwd.setText(stars.toString());
                }
            });
        }

        /**
         * 处理密码输入结果
         * .@param isPinUpload 脱机PIN值为0，联机PIN值为2
         *
         * Process password input results
         * .@param isPinUpload offline PIN value is 0, online PIN value is 2
         */
        @Override
        public void pinPadResult(int pinInputResult, int isPinUpload, int pinLen, byte[] pin) {
            CLog.log(TAG, "ProcessInterface|pinpad|pinPadResult, pinInputResult=" + pinInputResult + ", isPinUpload=" + isPinUpload + ", pinLen=" + pinLen);
            String msg = "";
            switch (pinInputResult) {
                case Core.PIN_QUIT_SUCCESS:
                    CLog.log(TAG, "ProcessInterface|pinPadResult, SUCCESS");
                    if (isPinUpload != 0x00 && pinLen > 0) {
                        MainActivity.this.pin = HEX.bytesToHex(pin);
                        CLog.log(TAG, "ProcessInterface|pinPadResult, pin=" + MainActivity.this.pin);
                        if (cardType == Process.CARD_TYPE_MAG) {
                            onlineProcess();
                            showTransactionSuccess();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTvPrompt.setText("");
                                    LedUtils.getInstance().success();
                                }
                            });

                        }
                    }
                    return;
                case Core.PIN_QUIT_CANCEL:
                    msg = "CANCEL";
                    break;
                case Core.PIN_QUIT_BYPASS:
                    CLog.log(TAG, "BYPASS");
//                    // TODO: 2019-06-28 逻辑同success
                    return;
                case Core.PIN_QUIT_ERROR:
                    msg = "ERROR";
                    break;
                case Core.PIN_QUIT_TIMEOUT:
                    msg = "TIMEOUT";
                    break;
                case Core.PIN_QUIT_ERRORPAN:
                    msg = "ERROR";
                    break;
                default:
                    break;
            }
            CLog.log(TAG, "ProcessInterface|msg=" + msg);
        }

        /**
         * 关闭密码输入框
         * close password dialog
         */
        @Override
        public void dismissPINPAD() {
            CLog.log(TAG, "ProcessInterface|pinpad|dismissPINPAD");
            if (pinPadDialog != null) {
                pinPadDialog.dismiss();
                pinPadDialog = null;
            }
            mAllowSendRotationCommand = false;
        }

        @Override
        public ArrayList<CVMItem> customCVM() {
            return null;
        }

        @Override
        public int offLinePinOnFailure(int i, CountDownLatch countDownLatch) {
            return 0;
        }

        /**
         * 显示卡号
         * show card No.
         */
        @Override
        public void displayPan(final String pan) {
            CLog.log(TAG, "ProcessInterface|displayPan, pan=" + pan);
            MainActivity.this.cardNo = pan;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTVCardNo.setText("Card No.:" + pan);
                }
            });
        }

        @Override
        public int interruptibleEMV(String s) {
            return 0;
        }

        @Override
        public void onAmexOutCome(byte[] bytes) {
            CLog.log(TAG, "ProcessInterface|onAmexOutCome,");
            // TODO

            try {
                Map<String, String> tlvOutcome = TlvUtil.unPack(bytes);
                String df8116 = tlvOutcome.get("DF8116");
                String df8129 = tlvOutcome.get("DF8129");
                if (!TextUtils.isEmpty(df8116)) {
                    df8116Value = EMVUtil.getDf8116((ByteUtil.hexString2Bytes(df8116)[0]));
                }
//                Logger.log(TAG, "amex tlvOutcome DF8116"+df8116);
//                Logger.log(TAG, "amex tlvOutcome DF8129"+df8129);

            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!TextUtils.isEmpty(df8116Value)) {
                        mTvPrompt.setText(df8116Value);
//                            mTvPrompt.postInvalidate();
                    } else {
                        mTvPrompt.setText("");
                    }
                }
            });

        }
    };
    private int mLastCount;
    private boolean enableGetAid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CLog.log(TAG, "initWiseasySDK");

                    int year = Calendar.getInstance().get(Calendar.YEAR);
                    if (year <= 1970) {
                        throw new Exception("System time error, please connect the network to get the time automatically!");
                    }

                    /*
                        1.SDK4 instance object initialization
                     */
                    CLog.log(TAG, "initWiseasySDK|SDKInstance init");
                    SDKInstance.initSDK(getApplicationContext());

                    /*
                        2.应用启动后进行库初始化
                     */
                    CLog.log(TAG, "initWiseasySDK|LibInit init");
                    LibInit.init(true);

                    // Other
                    LedUtils.getInstance().init(SDKInstance.mCore);

                    SDKInstance.mBankCard.breakOffCommand();
                    SDKInstance.mCore.setSupportSM(false);
                    isInit = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        findViewById(R.id.btn_init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
            }
        });
        final View ll0b10 = findViewById(R.id.ll_0b10);
        ((CheckBox)findViewById(R.id.cbox_enable_getAidData)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                enableGetAid = b;
                ll0b10.setVisibility(b?View.VISIBLE:View.GONE);
            }
        });
        findViewById(R.id.btn_purchase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                purchase();
            }
        });

        mTVCardNo = findViewById(R.id.tv_card_no);
        mTvTransResult = findViewById(R.id.tv_trans_result);
        mTvInitResult = findViewById(R.id.tv_init_result);
        mTvPrompt = findViewById(R.id.tv_prompt);
        mEtAmount = findViewById(R.id.et_amount);
        mEtTagType = findViewById(R.id.et_tagtype);
        mEtCountryCode = findViewById(R.id.et_countrycode);
        mEtCurrencyCode = findViewById(R.id.et_currencycode);
        mTvVersion = findViewById(R.id.tv_version);
        cbEnableScript = findViewById(R.id.cbox_enablescript);
        mPb = findViewById(R.id.pb);

        mTvVersion.setText(StringUtil.getVerName(this));

        // pinpad Rotation 1.init rotation
        mLastRotation = getWindowManager().getDefaultDisplay().getRotation();
        // pinpad Rotation 2. instance a OrientationEventListener
        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
//                CLog.log(TAG, "onOrientationChanged new orientation: " + orientation);
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    // The phone is lying flat and no effective angle is detected
                    CLog.log(TAG, "onOrientationChanged ORIENTATION_UNKNOWN");
                    return;
                }
                int newRotation = getWindowManager().getDefaultDisplay().getRotation();
                if (mLastRotation != newRotation) {
                    CLog.log(TAG, "onOrientationChanged last Rotation: " + mLastRotation);
                    CLog.log(TAG, "onOrientationChanged new Rotation: " + newRotation);
                    mLastRotation = newRotation;
                    if (mAllowSendRotationCommand) {
                        try {
                            /** pinpad Rotation 4. if rotation has changed,
                             * notify sdk, and then will callback refreshPINPADLayout
                             */
                            SDKInstance.mCore.pinPadRotation();
                            CLog.log(TAG, "onOrientationChanged pinPadRotation");
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        // pinpad Rotation 3. enable Orientation
        mOrientationEventListener.enable();
        CLog.log(TAG, "CashierDemo version: " + StringUtil.getVerName(this));

        findViewById(R.id.tv_get_jar_version).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String versionList = Version.JAR_VERSION_NAME + "\n" + sdk4.wangpos.libemvbinder.Version.JAR_VERSION_NAME+"\n"
                        + wangpos.sdk4.libkeymanagerbinder.Version.JAR_VERSION_NAME + "\n" + com.wiseasy.emvprocess.Version.JAR_VERSION_NAME;

                simpleDialog(MainActivity.this,versionList);
            }
        });
    }


    public void simpleDialog(final Context mContext,String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(R.mipmap.ic_launcher)
                .setTitle("Jar Version")
                .setMessage(message)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }


    /**
     * We can get new Configuration from this function.
     * but cannot use it, suggest to use field mLastRotation and mOrientationEventListener
     * <p>
     * notice: need to add android:configChanges="keyboardHidden|orientation|screenSize"
     * into AndroidManifest.xml refers current activity(MainActivity)
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

//    private void addPureTerminalConfig() throws Exception {
//        CLog.log(TAG, "addPureTerminalConfig");
//        FileInputStream fis = null;
//        String dataStr = "";
//        File file = new File(CONFIG_FILE + "PureTerminalConfig.json");
//        if (!file.exists()) {
//            CLog.loge(TAG, "File PureTerminalConfig.json does not exist!");
//            dataStr = StringUtil.getAssetsJson("PureTerminalConfig.json", this);
//            CLog.loge(TAG, "addPureTerminalConfig|The Assets file PureTerminal : " + dataStr);
//        } else {
//
//            fis = new FileInputStream(file);
//            byte[] ret = new byte[fis.available()];
//            int result = fis.read(ret);
//            fis.close();
//            dataStr = new String(ret, StandardCharsets.UTF_8);
//        }
//        if (TextUtils.isEmpty(dataStr)) {
//            return;
//        }
//        PureTermParamData termParamData = new Gson().fromJson(dataStr, PureTermParamData.class);
//        CLog.log(TAG, "addPureTerminalConfig|termParamData=" + termParamData.toString());
//        int addResult = KernelInit.setTermParamPure(termParamData);
//        CLog.log(TAG, "addPureTerminalConfig|result=" + addResult);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInit) {
            LedUtils.getInstance().close();
            try {
                SDKInstance.mBankCard.breakOffCommand();
                CLog.log("application exit close search card");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    // =============================================== Read Card ================================================= //

    /**
     * Init
     */
    private void init() {

        mTvInitResult.setText("Init Result:");
        mPb.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    CLog.log(TAG, "initWiseasySDK");

                    int year = Calendar.getInstance().get(Calendar.YEAR);
                    if (year <= 1970) {
                        throw new Exception("System time error, please connect the network to get the time automatically!");
                    }

                    /*
                        1.SDK4 instance object initialization
                     */
                    CLog.log(TAG, "initWiseasySDK|SDKInstance init");
                    SDKInstance.initSDK(getApplicationContext());

                    /*
                        2.应用启动后进行库初始化
                     */
                    CLog.log(TAG, "initWiseasySDK|LibInit init");
                    LibInit.init(true);

                    /*
                        3.调用内核初始化接口添加内核参数
                    */
                    doPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,1);

                } catch (Exception e) {
                    e.printStackTrace();
                    initFinish(e);
                }
            }
        }).start();
    }

    private void initFinish(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPb.setVisibility(View.GONE);

                if (e != null) {
                    mTvInitResult.append("Fail, " + e.getMessage());
                } else {
                    mTvInitResult.append("Success");
                }

            }
        });
    }

    /**
     * add AID param
     */
    private void addAID() throws Exception {
        CLog.log(TAG, "addAID");
        String aidsStr = "";
        CLog.log(TAG, "addAID|delAllAID");
        SDKInstance.mEmvCore.delAllAID();

        FileInputStream fis = null;

        File file = new File(CONFIG_FILE + "AID.json");
        CLog.log(TAG, "addAID|AID.json path: " + file.getAbsolutePath());
        if (!file.exists()) {
            CLog.loge(TAG, "addAID|The file AID.json does not exist!");

            aidsStr = StringUtil.getAssetsJson("AID.json", this);
            CLog.loge(TAG, "addAID|The Assets file aid : " + aidsStr);
        } else {
            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            aidsStr = new String(ret, StandardCharsets.UTF_8);
//            CLog.loge(TAG, "addAID|The cashier file aid : " + aidsStr);
        }
        if (TextUtils.isEmpty(aidsStr)) {
            return;
        }
        JsonArray aids = new Gson().fromJson(aidsStr, JsonArray.class);
        CLog.log(TAG, "addAID|The number of AIDs is " + aids.size());
        if (aids.size() > 0) {
            for (int j = 0; j < aids.size(); j++) {
                JsonObject aid = aids.get(j).getAsJsonObject();
                AIDData aidData = new AIDData();
                // MasterCard AID
                if (aid.has("MasterCard")) {
                    JsonObject masterCard = aid.get("MasterCard").getAsJsonObject();
                    if (masterCard.has("common")) {
                        JsonElement common = masterCard.get("common");
                        BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                        aidData.setBaseAIDData(baseAIDData);
                    }
                    if (masterCard.has("paypass")) {
//                        JsonElement paypass = masterCard.get("paypass");
                        JsonArray paypass = masterCard.getAsJsonArray("paypass");
                        for (int i = 0; i < paypass.size(); i++) {
                            CLPayPassAIDData clPayPassAIDData = new Gson().fromJson(paypass.get(i), CLPayPassAIDData.class);
                            aidData.setClAIDData(clPayPassAIDData);
                            if (i != paypass.size() - 1) {
                                int addpaypassResult = KernelInit.addAID(aidData);
                            }
                            CLog.log(TAG, "addAID|MasterCard, position = " + i);
                        }
                    }
                    CLog.log(TAG, "addAID|MasterCard, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                } else
                    // Visa AID
                    if (aid.has("Visa")) {
                        JsonObject paywave = aid.get("Visa").getAsJsonObject();
                        if (paywave.has("common")) {
                            JsonElement common = paywave.get("common");
                            BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                            aidData.setBaseAIDData(baseAIDData);
                        }
                        if (paywave.has("paywave")) {
                            JsonElement paywaveAID = paywave.get("paywave");
                            CLPayWaveAIDData clPayWaveAIDData = new Gson().fromJson(paywaveAID, CLPayWaveAIDData.class);
                            aidData.setClAIDData(clPayWaveAIDData);
                        }
                        CLog.log(TAG, "addAID|Visa, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                    } else
                        // PBOC AID
                        if (aid.has("PBOC")) {
                            JsonObject pboc = aid.get("PBOC").getAsJsonObject();
                            if (pboc.has("common")) {
                                JsonElement common = pboc.get("common");
                                BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                aidData.setBaseAIDData(baseAIDData);
                            }
                            if (pboc.has("qpboc")) {
                                JsonElement qpboc = pboc.get("qpboc");
                                CLqPBOCAIDData cLqPBOCAIDData = new Gson().fromJson(qpboc, CLqPBOCAIDData.class);
                                aidData.setClAIDData(cLqPBOCAIDData);
                            }
                            CLog.log(TAG, "addAID|PBOC, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                        } else
                            // AMEX AID initPOSParams ysh
                            if (aid.has("AMEX")) {
                                JsonObject AMEX = aid.get("AMEX").getAsJsonObject();
                                if (AMEX.has("common")) {
                                    JsonElement common = AMEX.get("common");
                                    BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                    aidData.setBaseAIDData(baseAIDData);
                                }
                                if (AMEX.has("contactLess")) {
                                    JsonElement contactLess = AMEX.get("contactLess");
                                    CLAMEXAIDData clAMEXAIDData = new Gson().fromJson(contactLess, CLAMEXAIDData.class);
                                    aidData.setClAIDData(clAMEXAIDData);
                                }
                                CLog.log(TAG, "addAID|AMEX, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                            } else if (aid.has("Discover")) {
                                JsonObject Discover = aid.get("Discover").getAsJsonObject();
                                if (Discover.has("common")) {
                                    JsonElement common = Discover.get("common");
                                    BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                    aidData.setBaseAIDData(baseAIDData);
                                }
                                // ----------------------------------------------------------------------------------
                                JsonArray DiscoverArray = Discover.getAsJsonArray("contactLess");
                                for (int i=0;i<DiscoverArray.size();i++) {
                                    CLDiscoverAIDData clDiscoverAIDData = new Gson().fromJson(DiscoverArray.get(i), CLDiscoverAIDData.class);
                                    aidData.setClAIDData(clDiscoverAIDData);
                                    if (i != DiscoverArray.size()-1) {
                                        int adddiscoverResult = KernelInit.addAID(aidData);
                                    }
                                    CLog.log(TAG, "addAID|discover, position = " + i);
                                }
                                // ----------------------------------------------------------------------------------
                                CLog.log(TAG, "addAID|discover, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                            }else if (aid.has("PURE")) {
                                JsonObject PURE = aid.get("PURE").getAsJsonObject();
                                if (PURE.has("common")) {
                                    JsonElement common = PURE.get("common");
                                    BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                    aidData.setBaseAIDData(baseAIDData);
                                }
                                if (PURE.has("contactLess")) {
//                        JsonElement paypass = masterCard.get("paypass");
                                    JsonArray contactLess = PURE.getAsJsonArray("contactLess");
                                    for (int i = 0; i < contactLess.size(); i++) {
                                        CLPureAIDData clPureAIDData = new Gson().fromJson(contactLess.get(i), CLPureAIDData.class);
                                        aidData.setClAIDData(clPureAIDData);
                                        if (i != contactLess.size() - 1) {
                                            int addPUREResult = KernelInit.addAID(aidData);
                                        }
                                        CLog.log(TAG, "addAID|PURE, position = " + i);
                                    }
                                }
                                CLog.log(TAG, "addAID|PURE, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                            } else if (aid.has("MIR")) {
                                JsonObject MIR = aid.get("MIR").getAsJsonObject();
                                if (MIR.has("common")) {
                                    JsonElement common = MIR.get("common");
                                    BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                    aidData.setBaseAIDData(baseAIDData);
                                }
                                if (MIR.has("contactLess")) {
//                        JsonElement paypass = masterCard.get("paypass");
                                    JsonArray MIRarray = MIR.getAsJsonArray("contactLess");
                                    for (int i = 0; i < MIRarray.size(); i++) {
                                        CLMirAIDData clMIRIDData = new Gson().fromJson(MIRarray.get(i), CLMirAIDData.class);
                                        aidData.setClAIDData(clMIRIDData);
                                        if (i != MIRarray.size() - 1) {
                                            int addpaypassResult = KernelInit.addAID(aidData);
                                        }
                                        CLog.log(TAG, "addAID|MIR, position = " + i);
                                    }
                                }
                                CLog.log(TAG, "addAID|MIR, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                            } else if (aid.has("JCB")) {
                                JsonObject JCB = aid.get("JCB").getAsJsonObject();
                                if (JCB.has("common")) {
                                    JsonElement common = JCB.get("common");
                                    BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                    aidData.setBaseAIDData(baseAIDData);
                                }
                                if (JCB.has("contactLess")) {
//                        JsonElement paypass = masterCard.get("paypass");
                                    JsonArray JCBarray = JCB.getAsJsonArray("contactLess");
                                    for (int i = 0; i < JCBarray.size(); i++) {
                                        CLJcbAIDData clJCBIDData = new Gson().fromJson(JCBarray.get(i), CLJcbAIDData.class);
                                        aidData.setClAIDData(clJCBIDData);
                                        if (i != JCBarray.size() - 1) {
                                            int addpaypassResult = KernelInit.addAIDJCB(aidData);
                                        }
                                        CLog.log(TAG, "addAID|JCB, position = " + i);
                                    }
                                }
                                CLog.log(TAG, "addAID|JCB, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                            } else if (aid.has("RuPay")) {
                                JsonObject RuPay = aid.get("RuPay").getAsJsonObject();
                                if (RuPay.has("common")) {
                                    JsonElement common = RuPay.get("common");
                                    BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                    aidData.setBaseAIDData(baseAIDData);
                                }
                                if (RuPay.has("contactLess")) {
//                        JsonElement paypass = masterCard.get("paypass");
                                    JsonArray RuPayarray = RuPay.getAsJsonArray("contactLess");
                                    for (int i = 0; i < RuPayarray.size(); i++) {
                                        CLRupayAIDData clRuPayAIDData = new Gson().fromJson(RuPayarray.get(i), CLRupayAIDData.class);
                                        aidData.setClAIDData(clRuPayAIDData);
                                        if (i != RuPayarray.size() - 1) {
                                            int addpaypassResult = KernelInit.addAIDRupay(aidData);
                                            CLog.log(TAG, "addAID|RuPay, addRupayResult = " + addpaypassResult);
                                        }
                                        CLog.log(TAG, "addAID|RuPay, position = " + i);
                                    }
                                }
                                CLog.log(TAG, "addAID|RuPay, AID_9F06=" + aidData.getBaseAIDData().getAID_9F06());
                            } else {
                                if (aid.has("OtherBrand")) {
                                    JsonObject OtherBrand = aid.get("OtherBrand").getAsJsonObject();
                                    if (OtherBrand.has("common")) {
                                        JsonElement common = OtherBrand.get("common");
                                        BaseAIDData baseAIDData = new Gson().fromJson(common, BaseAIDData.class);
                                        aidData.setBaseAIDData(baseAIDData);
                                    }
                                }
                            }
                int addResult = KernelInit.addAID(aidData);
                CLog.log(TAG, "addAID|result=" + addResult + "(" + StringUtil.intToHexString(addResult) + ")");
            }
        }

    }

    /**
     * 设置CAPK
     */
    private void addCAPK() throws Exception {
        CLog.log(TAG, "addCAPK");

        CLog.log(TAG, "addCAPK|delAllCAPK");
        SDKInstance.mEmvCore.delAllCAPK();
        FileInputStream fis = null;
        String capksStr = "";
        File file = new File(CONFIG_FILE + "CAPK.json");
        CLog.log(TAG, "addCAPK|CAPK.json path: " + file.getAbsolutePath());
        if (!file.exists()) {
            CLog.loge(TAG, "addCAPK|The file CAPK.json does not exist!");
            capksStr = StringUtil.getAssetsJson("CAPK.json", this);
            CLog.loge(TAG, "addCAPK|The Assets file CAPK : " + capksStr);
        } else {

            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            capksStr = new String(ret, StandardCharsets.UTF_8);
        }
        if (TextUtils.isEmpty(capksStr)) {
            return;
        }
        CLog.log(TAG, "addCAPK|The number of capksStr " + capksStr);
        JsonArray capks = new Gson().fromJson(capksStr, JsonArray.class);
        CLog.log(TAG, "addCAPK|The number of CAPKs is " + capks.size());
        if (capks.size() > 0) {
            for (int j = 0; j < capks.size(); j++) {
                CAPKData capkData = new Gson().fromJson(capks.get(j), CAPKData.class);
                CLog.log(TAG, "addCAPK|capkData=" + capkData.toString());
                CLog.log(TAG, "addCAPK|capkData getRID=" + capkData.getRID());
                int addResult = KernelInit.addCAPK(capkData);
                CLog.log(TAG, "addCAPK|result=" + addResult + "(" + StringUtil.intToHexString(addResult) + ")");
            }
        }

    }

    /**
     * initPOSParams ExceptionPan
     */
    private void addExceptionPan() throws Exception {
        CLog.log(TAG, "addExceptionPan");
        FileInputStream fis = null;

        File file = new File(CONFIG_FILE + "ExceptionPan.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File ExceptionPan.json does not exist!");
            return;
        }

        fis = new FileInputStream(file);
        byte[] ret = new byte[fis.available()];
        int result = fis.read(ret);
        fis.close();
        String dataStr = new String(ret, StandardCharsets.UTF_8);

        JsonArray ExceptionPans = new Gson().fromJson(dataStr, JsonArray.class);
        if (ExceptionPans.size() > 0) {
            for (int j = 0; j < ExceptionPans.size(); j++) {
                JsonObject exceptionPan = ExceptionPans.get(j).getAsJsonObject();
                ExceptionPan exceptionPanBean = new Gson().fromJson(exceptionPan, ExceptionPan.class);
                CLog.log(TAG, "addExceptionPan|exceptionPanBean=" + exceptionPanBean.toString());
                int addResult = KernelInit.addExceptionPan(exceptionPanBean);
                CLog.log(TAG, "addExceptionPan|result=" + addResult);
            }
        }
    }

    /**
     * initPOSParams RemovedCAPK
     */
    private void addRemovedCAPK() throws Exception {
        CLog.log(TAG, "addRemovedCAPK");
        FileInputStream fis = null;
        File file = new File(CONFIG_FILE + "RemovedCAPK.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File RemovedCAPK.json does not exist!");
            return;
        }

        fis = new FileInputStream(file);
        byte[] ret = new byte[fis.available()];
        int result = fis.read(ret);
        fis.close();
        String dataStr = new String(ret, StandardCharsets.UTF_8);

        JsonArray RemovedCAPKs = new Gson().fromJson(dataStr, JsonArray.class);
        if (RemovedCAPKs.size() > 0) {
            for (int j = 0; j < RemovedCAPKs.size(); j++) {
                JsonObject removedCAPK = RemovedCAPKs.get(j).getAsJsonObject();
                CAPKRevoke removedCAPKBean = new Gson().fromJson(removedCAPK, CAPKRevoke.class);
                CLog.log(TAG, "addRemovedCAPK|removedCAPKBean=" + removedCAPKBean.toString());
                int addResult = KernelInit.addRemovedCAPK(removedCAPKBean);
                CLog.log(TAG, "addRemovedCAPK|result=" + addResult);
            }
        }
    }

    /**
     * set PaypassTerminalConfig
     */
    private void addPayPassTerminalConfig() throws Exception {
        CLog.log(TAG, "addPayPassTerminalConfig");
        FileInputStream fis = null;
        String dataStr = "";
        File file = new File(CONFIG_FILE + "PayPassTerminalConfig.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File PayPassTerminalConfig.json does not exist!");
            dataStr = StringUtil.getAssetsJson("PayPassTerminalConfig.json", this);
            CLog.loge(TAG, "addPayWaveDRLData|The Assets file DRL : " + dataStr);
        } else {
            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            dataStr = new String(ret, StandardCharsets.UTF_8);
        }
        if (TextUtils.isEmpty(dataStr)) {
            return;
        }
        JsonArray terminalConfigs = new Gson().fromJson(dataStr, JsonArray.class);
        if (terminalConfigs.size() > 0) {
            for (int j = 0; j < terminalConfigs.size(); j++) {
                JsonObject terminalConfig = terminalConfigs.get(j).getAsJsonObject();
                PaypassTerminalConfig terminalConfigBean = new Gson().fromJson(terminalConfig, PaypassTerminalConfig.class);
                CLog.log(TAG, "addPayPassTerminalConfig|PaypassTerminalConfig=" + terminalConfigBean.toString());
                int addResult = KernelInit.addPaypassTerminalConfig(terminalConfigBean);
                CLog.log(TAG, "addPayPassTerminalConfig|result=" + addResult);
            }
        }
    }

    /**
     * initPOSParams DRLData
     */
    private void addPayWaveDRLData() throws Exception {
        CLog.log(TAG, "addPayWaveDRLData");
        FileInputStream fis = null;
        String dataStr = "";
        File file = new File(CONFIG_FILE + "PayWaveDRLData.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File PayWaveDRLData.json does not exist!");
            dataStr = StringUtil.getAssetsJson("PayWaveDRLData.json", this);
            CLog.loge(TAG, "addPayWaveDRLData|The Assets file DRL : " + dataStr);
        } else {
            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            dataStr = new String(ret, StandardCharsets.UTF_8);
        }
        if (TextUtils.isEmpty(dataStr)) {
            return;
        }
        JsonArray DRLDatas = new Gson().fromJson(dataStr, JsonArray.class);
        if (DRLDatas.size() > 0) {
            for (int j = 0; j < DRLDatas.size(); j++) {
                JsonObject dRLData = DRLDatas.get(j).getAsJsonObject();
                DRLData dRLDataBean = new Gson().fromJson(dRLData, DRLData.class);
                CLog.log(TAG, "addPayWaveDRLData|dRLDataBean=" + dRLDataBean.toString());
                int addResult = KernelInit.addDRLPayWave(dRLDataBean);
                CLog.log(TAG, "addPayWaveDRLData|result=" + addResult);
            }
        }
    }

    /**
     * initPOSParams DRLData_AMEX
     */
    private void addAMEXDRLData() throws Exception {
        CLog.log(TAG, "addAMEXDRLData");
        FileInputStream fis = null;
        String dataStr = "";
        File file = new File(CONFIG_FILE + "AMEXDRLData.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File AMEXDRLData.json does not exist!");
            dataStr = StringUtil.getAssetsJson("AMEXDRLData.json", this);
            CLog.loge(TAG, "addPayWaveDRLData|The Assets file DRL : " + dataStr);
        } else {
            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            dataStr = new String(ret, StandardCharsets.UTF_8);
        }
        if (TextUtils.isEmpty(dataStr)) {
            return;
        }
        JsonArray DRLDatas = new Gson().fromJson(dataStr, JsonArray.class);
        if (DRLDatas.size() > 0) {
            for (int j = 0; j < DRLDatas.size(); j++) {
                JsonObject dRLData = DRLDatas.get(j).getAsJsonObject();
                DRLAmexData amexDRL = new Gson().fromJson(dRLData, DRLAmexData.class);
                CLog.log(TAG, "addAMEXDRLData|value=" + amexDRL.toString());
                int addResult = KernelInit.addDRLAmex(amexDRL);
                CLog.log(TAG, "addAMEXDRLData|result=" + addResult);
            }
        }
    }

    /**
     * initPOSParams DRLData_AMEX
     */
    private void addAMEXTerminalConfig() throws Exception {
        CLog.log(TAG, "addAMEXTerminalConfig");
        FileInputStream fis = null;
        String dataStr = "";
        File file = new File(CONFIG_FILE + "AMEXTerminalConfig.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File AMEXTerminalConfig.json does not exist!");
            dataStr = StringUtil.getAssetsJson("AMEXTerminalConfig.json", this);
            CLog.loge(TAG, "addAMEXTerminalConfig|The Assets file AMEXTerminal : " + dataStr);
        } else {

            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            dataStr = new String(ret, StandardCharsets.UTF_8);
        }
        if (TextUtils.isEmpty(dataStr)) {
            return;
        }
        TermParamData termParamData = new Gson().fromJson(dataStr, TermParamData.class);
        CLog.log(TAG, "addAMEXTerminalConfig|termParamData=" + termParamData.toString());
        int addResult = KernelInit.setTermParamAmex(termParamData);
        CLog.log(TAG, "addAMEXTerminalConfig|result=" + addResult);
    }

    /**
     * initPOSParams discover
     */
    private void addDiscoverTerminalConfig() throws Exception {
        CLog.log(TAG, "addAMEXTerminalConfig");
        FileInputStream fis = null;
        String dataStr = "";
        File file = new File(CONFIG_FILE + "DiscoverTerminalConfig.json");
        if (!file.exists()) {
            CLog.loge(TAG, "File DiscoverTerminalConfig.json does not exist!");
            dataStr = StringUtil.getAssetsJson("DiscoverTerminalConfig.json", this);
            CLog.loge(TAG, "adddiscoverTerminalConfig|The Assets file discoverTerminal : " + dataStr);
        } else {

            fis = new FileInputStream(file);
            byte[] ret = new byte[fis.available()];
            int result = fis.read(ret);
            fis.close();
            dataStr = new String(ret, StandardCharsets.UTF_8);
        }
        if (TextUtils.isEmpty(dataStr)) {
            return;
        }
        TermParamDisCover termParamData_discover = new Gson().fromJson(dataStr, TermParamDisCover.class);
        CLog.log(TAG, "addDiscoverTerminalConfig|termParamData=" + termParamData_discover.toString());
        int addResult = KernelInit.setTermParamDisCover(termParamData_discover);
        CLog.log(TAG, "addDiscoverTerminalConfig|result=" + addResult);
    }

    /**
     * 添加AcquirerKey
     * 每个包下需要添加一组MK/SK和一组IPEK
     * <p>
     * - TMK: Terminal main key, 终端主密钥
     * - KCV: Key check value, 密钥校验值
     *
     * @param keyPackageName visa/mastercard/qpboc
     */
    public void addAcquirerKey(String keyPackageName) throws Exception {
        CLog.log(TAG, "addAcquirerKey:" + keyPackageName);
        // MK
        AcquirerKey key01 = new AcquirerKey();
        key01.setPackageName(keyPackageName);
        key01.setKeyType(Key.KEY_REQUEST_TMK);
        key01.setAzKeyValue("BA6710D04A625D389D46024525FB7F10");
        key01.setAlgorithmType(Core.ALGORITHM_3DES);
        key01.setAlgorithmType(0x00);
        key01.setIsPlainText(true);
        key01.setAzKCVValue("C8F7C5A8F7712BB0");
        Acquirer.getInstance().addAcquireKey(key01);
        // SK:DEK
        AcquirerKey key02 = new AcquirerKey();
        key02.setPackageName(keyPackageName);
        key02.setKeyType(Key.KEY_REQUEST_DEK);
        key02.setAzKeyValue("5CCFD5353C42FFC7F64F92D112575212");
//        key02.setAlgorithmType(Core.ALGORITHM_3DES);
        key02.setAlgorithmType(0x00);
        key02.setAzKCVValue("795319D9");
        key02.setIsPlainText(false);
        Acquirer.getInstance().addAcquireKey(key02);
        // SK:PEK
        AcquirerKey key03 = new AcquirerKey();
        key03.setPackageName(keyPackageName);
        key03.setKeyType(Key.KEY_REQUEST_PEK);
        key03.setAzKeyValue("815C023BC84F16CCB8453CA21C808263");
//        key03.setAlgorithmType(Core.ALGORITHM_3DES);
        key03.setAlgorithmType(0x00);
        key03.setAzKCVValue("C35EF51E");
        key03.setIsPlainText(false);
        Acquirer.getInstance().addAcquireKey(key03);
//        SDKInstance.mKey.erasePED();
        // IPEK
        AcquirerKey key04 = new AcquirerKey();
        key04.setPackageName(keyPackageName);
        key04.setKeyType(Key.KEY_REQUEST_IPEK); //TODO
        key04.setAzKeyValue("C1D0F8FB4958670DBA40AB1F3752EF0D");
//        key04.setAlgorithmType(Core.ALGORITHM_3DES);
        key04.setAlgorithmType(0x00);
        key04.setAzKCVValue("2DAF031DA77D92AC");
        key04.setAzKSN("FFFF9876543210E10004");
        key04.setIsPlainText(true);
        Acquirer.getInstance().addAcquireKey(key04);

        // SK MAK
        AcquirerKey key05 = new AcquirerKey();
        key05.setPackageName(keyPackageName);
        key05.setKeyType(Key.KEY_REQUEST_MAK);
        key05.setAzKeyValue("28EBDF2B72A32B15D7399E33B4C3876B");
//        key04.setAlgorithmType(Core.ALGORITHM_3DES);
        key05.setAlgorithmType(0x00);
        key05.setAzKCVValue("949ED390");
        key05.setIsPlainText(false);
        Acquirer.getInstance().addAcquireKey(key05);


    }

    /**
     * Purchase process
     */
    private void purchase() {

        // hide the soft input window
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

        if (!isInit) {
            Toast.makeText(this, "Please initialize it first", Toast.LENGTH_SHORT).show();
//            return;
        }

        // Init UI
        mTvTransResult.setText("Transaction Result:");
        mTVCardNo.setText("Card No.:");

        // check the transaction amount
        String amountStr = mEtAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Please input the transaction amount", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            amount = Long.parseLong(amountStr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Turn on the lights waiting to read the card
        LedUtils.getInstance().close();
        LedUtils.getInstance().waitting();

        mTvPrompt.setText("Please tap/insert/swipe card");


        // Start to read card
        mReadCardThread = new ReadCardThread();
        mReadCardThread.start();
    }

    void test() {
        try {
            String tagValue = TransResult.getTagValue(0x57);
            CLog.log(TAG, "ProcessInterface|processOnline, tagValue=" + tagValue);
            //从内核获取需要的数据，TLV格式      Get the required data from the kernel, in TLV format
            String tagList = TransResult.getTagList();
            CLog.log(TAG, "ProcessInterface|processOnline, tagList=" + tagList);
            Map<String, String> tags = TlvUtil.unPack(HEX.hexToBytes(tagList));
            CLog.log(TAG, "ProcessInterface|processOnline, tags=" + tags.toString());
            int ret = SDKInstance.mKey.IncreaseKSN("com.wiseasy.cashier.demo");
            CLog.log(TAG, "IncreaseKSN result :" + ret);
            byte[] outData = new byte[16];
            int[] outDataLen = new int[1];
//            byte[] outData = new byte[8];
//            int[] outDataLen = new int[1];
            int ret1 = SDKInstance.mKey.GetKSN("com.wiseasy.cashier.demo", outData, outDataLen);
//            int  ret1 =  SDKInstance.mCore.getDeviceStatus(outData,outDataLen);
//            int  ret1 =  SDKInstance.mCore.get(outData,outDataLen);
            CLog.log(TAG, "GetKSN outData:" + ByteUtil.bytes2HexString(outData));
            CLog.log(TAG, "GetKSN outDataLen :" + outDataLen[0]);
            CLog.log(TAG, "GetKSN result :" + ret1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    void test() {
//        KeyControlUtil.setPropForControlHomeKey(true);
//        KeyControlUtil.setPropForControlMenuKey(false);
//        KeyControlUtil.setPropForControlBackKey(false);
//        boolean homeKey = KeyControlUtil.isAvailable_HomeKey();
//        boolean backKey = KeyControlUtil.isAvailable_BackKey();
//        boolean menuKey = KeyControlUtil.isAvailable_MenuKey();
//        Log.d(TAG, "homeKey: getProperty "+ homeKey);
//        Log.d(TAG, "backKey: getProperty "+ backKey);
//        Log.d(TAG, "menuKey: getProperty "+ menuKey);
//
//    }

    /**
     * 磁条卡处理流程
     * Handle magnetic strip card flow
     *
     * @param outData
     */
    private void processMSC(byte[] outData) {
        CLog.log(TAG, "processMSC:" + ByteUtil.bytes2HexString(outData));
        final TrackData trackData = TransProcess.getInstance().unpackTrackData(outData);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (trackData != null) {
                    String track2 = trackData.getTrack2();
                    if (!TextUtils.isEmpty(track2)) {
                        String[] pan = track2.split("=");
                        mTVCardNo.setText("Card No.:" + pan[0]);
                    }
                }
            }
        });


        CLog.log(TAG, "processMSC|trackData.toString=" + trackData.toString());
        PINSetting pinSetting = new PINSetting();
        pinSetting.setPackageName("com.wiseasy.cashier.demo");
        pinSetting.setBypass(false);
        pinSetting.setMinPINLength(4);
        pinSetting.setMaxPINLength(6);
        pinSetting.setTimeOut(15);
        pinSetting.setDUKPT(false);
        pinSetting.setFixLayout(true);
        pinSetting.setPan("1234567890123456"); //cardNumber
        pinSetting.setClearButtonNotCannel(true);
        // 弹密
        Acquirer.getInstance().startPINInput(MainActivity.this, pinSetting, processInterface);

        // TODO: 2019-06-28 密码输入成功后，开始联机


//        processOnline();
    }

    /**
     * 处理接触流程
     * Handle contact flow
     */
    private void processContact() throws RemoteException {
        CLog.log(TAG, "processContact");
        // 卡片处理中，黄灯      Card processing, yellow light
        LedUtils.getInstance().cardData();
        SDKInstance.mEmvCore.enableAppSelGetAIDData(enableGetAid);
        // 调用EMVProcessSDK的处理接触交易的接口      Call EMVProcessSDK's interface for handling contact transactions
        TransInfoBean transInfoBean = buildSDKCommonTransInfo();

        byte[] macData = new byte[128];
        int[] macLen = new int[1];
        try {
            int mac = SDKInstance.mCore.getMac(new byte[16],16,macData, macLen);
            Logger.log(TAG, "ProcessInterface|getMac " + mac +ByteUtil.bytes2HexString(Arrays.copyOf(macData,macLen[0])));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        int result = TransProcess.getInstance().contactTrans(this, transInfoBean, processInterface);
//        int result = TransProcess.getInstance().doContactTransAsyncOnline(this, transInfoBean, processInterface);
//        int result = TransProcess.getInstance().doContactSimplify(this, transInfoBean);
        String resultHex = StringUtil.intToHexString(result);
        CLog.log(TAG, "processContact|result=" + result + "(" + resultHex + ")");

        String tagList = TransResult.getTagList();
        CLog.log(TAG, "processContact|tagList = " + tagList);

        try {
            //从内核获取需要的数据，TLV格式      Get the required data from the kernel, in TLV format
            byte[] tmpret = new byte[100];
            int[] tmplen = new int[1];
            SDKInstance.mEmvCore.getTLV(0x95, tmpret, tmplen);
//            SDKInstance.mEmvCore.getTLV(0x8A, tmpret, tmplen);
            CLog.log(TAG, "processContact| 0x95 result=" + ByteUtil.bytes2HexString(tmpret));
            CLog.log(TAG, "processContact| 0x95 tmplen=" + tmplen[0]);
            // 安全获取数据，需要先导入DUKPT
            String strSecurityData = TransProcess.getInstance().getSecurityDataList("com.wiseasy.cashier.demo");
            CLog.log(TAG, "processContact| strSecurityData result=" + strSecurityData);
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            //从内核获取需要的数据，TLV格式      Get the required data from the kernel, in TLV format
            byte[] tmpret = new byte[100];
            int[] tmplen = new int[1];
            SDKInstance.mEmvCore.getTLV(0x95, tmpret, tmplen);
            CLog.log(TAG, "processContactLess| 0x95 result=" + ByteUtil.bytes2HexString(tmpret));
            CLog.log(TAG, "processContactLess| 0x95 tmplen=" + tmplen[0]);

            byte[] tmprett = new byte[100];
            int[] tmplent = new int[1];
            SDKInstance.mEmvCore.getTLV(0x9C, tmprett, tmplent);
            CLog.log(TAG, "processContactLess| 0x9C resultt=" + ByteUtil.bytes2HexString(tmprett));
            CLog.log(TAG, "processContactLess| 0x9C tmplent=" + tmplent[0]);

            byte[] tmpret0 = new byte[8];
            int[] tmplen0 = new int[1];
            SDKInstance.mEmvCore.getTLV(0x5A, tmpret0, tmplen0);//card no.
            CLog.log(TAG, "processContact| 0x5A result=" + ByteUtil.bytes2HexString(tmpret0));
            CLog.log(TAG, "processContact| 0x5A tmplen=" + tmplen0[0]);

            byte[] tmpret1 = new byte[50];
            int[] tmplen1 = new int[1];
            SDKInstance.mEmvCore.getTLV(0x5F20, tmpret1, tmplen1);//name **** EXCEPTION ****
            CLog.log(TAG, "processContact| 0x5F20 result=" + ByteUtil.bytes2HexString(tmpret1));
            CLog.log(TAG, "processContact| 0x5F20 result 1 =" + ByteUtil.fromUtf8(tmpret1));
            CLog.log(TAG, "processContact| 0x5F20 tmplen=" + tmplen1[0]);


            byte[] tmpret2 = new byte[3];
            int[] tmplen2 = new int[1];
            SDKInstance.mEmvCore.getTLV(0x5F24, tmpret2, tmplen2);//expiration date
            CLog.log(TAG, "processContact| 0x5F24 result=" + ByteUtil.bytes2HexString(tmpret2));
            CLog.log(TAG, "processContact| 0x5F24 tmplen=" + tmplen2[0]);


            SDKInstance.mEmvCore.getTLV(0x9F2A, tmpret, tmplen);
            CLog.log(TAG, "processContactLess| 0x9F2A result=" + ByteUtil.bytes2HexString(tmpret));
            CLog.log(TAG, "processContactLess| 0x9F2A tmplen=" + tmplen[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }


        closeCardReader();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvPrompt.setText("");
            }
        });

        /*
            交易结果      transaction results
            - 0：成功     success
            - 非0(non-0)：失败     fail
         */
        if (result == RspEMVCode.TRANSPROCESS_CONTACTTRANS_SUCCESS) {
            showTransactionSuccess();
        } else {
            showTransactionFail(resultHex);
        }
    }

    /**
     * 处理非接流程
     * Handle non-incoming processes
     */
    private void processContactLess() throws RemoteException {
        CLog.log(TAG, "processContactLess");
        // 卡片处理中，黄灯      Card processing, yellow light
        LedUtils.getInstance().cardData();
        SDKInstance.mEmvCore.enableAppSelGetAIDData(enableGetAid);
        // 调用EMVProcessSDK的处理非接交易的接口      An interface that calls the EMVProcessSDK to handle non-incoming transactions
        TransInfoBean transInfoBean = buildSDKCommonTransInfo();
//        int result = TransProcess.getInstance().contactTransLess(this, transInfoBean, processInterface);
        int result = TransProcess.getInstance().doCLTransAsyncOnline(this, transInfoBean, processInterface);
//        int result = TransProcess.getInstance().doCLSimplify(this,transInfoBean);
        int kernalPath = SDKInstance.mEmvCore.getPath();
        CLog.log(TAG, "processContactLess|getPath = " + kernalPath);
        String resultHex = StringUtil.intToHexString(result);
        CLog.log(TAG, "processContactLess|result=" + result + "(" + resultHex + ")");
//        String strSecurityData = TransProcess.getInstance().getSecurityDataList("com.wiseasy.cashier.demo");
//        CLog.log(TAG, "processContactLess| strSecurityData result=" + strSecurityData);
        String tagList = TransResult.getTagList();
        CLog.log(TAG, "processContactLess|tagList = " + tagList);


//        data -> usecase -> repos -> services
//        1. config initialization
//        2. reading tags and values, map to data classes


        // TODO is used for test
        try {
            //从内核获取需要的数据，TLV格式      Get the required data from the kernel, in TLV format
            byte[] tmpret = new byte[100];
            int[] tmplen = new int[1];
            SDKInstance.mEmvCore.getTLV(0x95, tmpret, tmplen);



//            SDKInstance.mEmvCore.getTLV(0x9F02, tmpret, tmplen);
//            Arrays.fill(tmpret, (byte) '0');
//            SDKInstance.mEmvCore.getTLV(0x5A, tmpret, tmplen);
//            Arrays.fill(tmpret, (byte) '0');
//            SDKInstance.mEmvCore.getTLV(0x5F24, tmpret, tmplen);
//            Arrays.fill(tmpret, (byte) '0');
//            SDKInstance.mEmvCore.getTLV(0x5F2A, tmpret, tmplen);
//            Arrays.fill(tmpret, (byte) '0');
//            SDKInstance.mEmvCore.getTLV(0x9F1A, tmpret, tmplen);
            CLog.log(TAG, "processContactLess| 0x95 result=" + ByteUtil.bytes2HexString(tmpret));
            CLog.log(TAG, "processContactLess| 0x95 tmplen=" + tmplen[0]);




            SDKInstance.mEmvCore.getTLV(0x9F2A, tmpret, tmplen);
            CLog.log(TAG, "processContactLess| 0x9F2A result=" + ByteUtil.bytes2HexString(tmpret));
            CLog.log(TAG, "processContactLess| 0x9F2A tmplen=" + tmplen[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeCardReader();

        /*
            OutCome：内核交易处理结果，只有非接需要有该流程
            - 目前只有PayPass有该数据，PayWave后期会加上
            - 涉及到的TAG：
              DF8129: 结果参数集
              DF8115: 出错指示
              DF8116: 用户界面请求数据

            OutCome：Kernel transaction processing results, only non - calls need to have this process.
            - Currently only the PayPass has this data, which PayWave will add later
            - TAG involved:
              DF8129: Outcome Parameter Set
              DF8115: Error Indication
              DF8116: User Interface Request Data
        */
        outCome = TransResult.getOutCome();
        CLog.log(TAG, "processContactLess|outCome=" + outCome);

        final String outComeStr = getOutCome(outCome);
        CLog.log(TAG, "processContactLess|outComeStr=" + outComeStr);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(outComeStr)) {
                    mTvPrompt.setText(outComeStr);
                } else {
                    mTvPrompt.setText("");
                }
            }
        });

        /*
            交易结果      transaction results
            - 0：成功     success
            - 非0(non-0)：失败     fail
         */
        if (result == RspEMVCode.TRANSPROCESS_CONTACTLESSTRANS_SUCCESS) {
            showTransactionSuccess();
        } else {
            showTransactionFail(resultHex);
        }
    }

    /**
     * 通过OutComeData获取需要显示到界面上的数据
     * Get the data that needs to be displayed on the interface through OutComeData
     */
    private String getOutCome(OutComeData outCome) {
        StringBuilder sb = new StringBuilder();
        if (outCome != null) {
            String df8129_msg = outCome.getDF8129_Msg();
            if (!TextUtils.isEmpty(df8129_msg)) {
                sb.append(df8129_msg).append(",");
            }
            String df8116_msg = outCome.getDF8116_Msg();
            if (!TextUtils.isEmpty(df8116_msg)) {
                sb.append(df8116_msg).append(",");
            }
            String df8115_msg = outCome.getDF8115_Msg();
            if (!TextUtils.isEmpty(df8115_msg)) {
                sb.append(df8115_msg);
            }
        }
        String temp = sb.toString();
        if (!TextUtils.isEmpty(temp)) {
            if (temp.endsWith(",")) {
                temp = temp.substring(0, temp.length() - 1);
            }
        }
        return temp;
    }

    /**
     * 关闭读卡器
     * - 交易结束后，需要关闭读卡器。对于某些测试工具，如果不关闭读卡器，会导致用例失败。
     * <p>
     * Close Card Reader
     * - After the transaction is over, you need to close the reader. For some test tools, if you
     * do not close the reader, the use case will fail.
     */
    private void closeCardReader() throws RemoteException {
        CLog.log(TAG, "closeCardReader");
        SDKInstance.mBankCard.openCloseCardReader(BankCard.CARD_NMODE_PICC | BankCard.CARD_NMODE_ICC | BankCard.CARD_NMODE_MAG, BankCard.CARD_READ_CLOSE);
    }

    /**
     * 构建通用的交易参数
     * Build common transaction parameters
     */
    private TransInfoBean buildSDKCommonTransInfo() {
        TransInfoBean transInfoBean = new TransInfoBean();
        // 交易金额      Transaction amount
        TransAmount transAmount = new TransAmount();
        CLog.log(TAG, "buildSDKCommonTransInfo, amount=" + amount);
        transAmount.setAmount(amount);
        if (otherAmount > 0) {
            CLog.log(TAG, "buildSDKCommonTransInfo, otherAmount=" + otherAmount);
            transAmount.setOtherAmount(otherAmount);
        }
        transInfoBean.transAmount = null;
        // 银行卡凭证号，也叫POS流水号，从1～999999循环使用      Voucher number, Also known as POS serial number, it circulates from 1 to 999999
        transInfoBean.setTransNumber(25478);
        // 交易类型      Transaction type
        transInfoBean.setTransType(Process.TYPE_SALE);
        //transInfoBean.enableSetAmountAfter(true);
//        transInfoBean.setTransType9C("00");
        // 终端设置      Terminal Setting
        TerminalSetting terminalSetting = new TerminalSetting(getApplicationContext());
        terminalSetting.setMerchantId_9F16("100000000000009");  // 商户号 M
        terminalSetting.setMerchantName("Wise Cashier Demo");   // 商户名   M
        terminalSetting.setTerminalId_9F1C("99999989"); // 终端号  M
        terminalSetting.setTerminalType_9F35("22");// 终端类型  0x22 M
        String editText = mEtTagType.getText().toString();
        if(editText!=null && !editText.equals("")){
            transInfoBean.setTransType9C(editText);
        }else{
            transInfoBean.setTransType9C("1");
        }
        editText = mEtCountryCode.getText().toString();
        if(editText!=null && !editText.equals("")){
            terminalSetting.setCountryCode_9F1A(editText);// 国家代码 M   156-China, 076-Brazil, 854-美元, 144-斯里兰卡
        }else{
            terminalSetting.setCountryCode_9F1A("643");// 国家代码 M   156-China, 076-Brazil, 854-美元, 144-斯里兰卡
        }
        editText = mEtCurrencyCode.getText().toString();
        if(editText!=null && !editText.equals("")){
            terminalSetting.setTransCurrencyCode_5F2A(editText); // 交易货币代码 M 每笔交易可不同（Every deal is different）
        }else{
            terminalSetting.setTransCurrencyCode_5F2A("643"); // 交易货币代码 M 每笔交易可不同（Every deal is different）
        }
        terminalSetting.setTerminalCap_9F33("0xE0D8C8");  // 终端性能  9f33 M
        terminalSetting.setExTerminalCap_9F40("F000F0A001");  // 扩展终端性能 9F40 M
        terminalSetting.setTTQ_9F66("77000000"); // TTQ PayWave M
        terminalSetting.setDisplayOfflinePINRetryTime("1");
        terminalSetting.setTransType("1");
//        terminalSetting.setDRLSupportFlag("1");
        transInfoBean.setTerminalSetting(terminalSetting);

        return transInfoBean;
    }

    /**
     * 联机请求
     * - 该方法必须同步返回结果，所以处理方式有两个：
     * 1.全部处理逻辑均使用同步操作：准备数据，同步请求服务端，然后同步返回交易处理结果；
     * 2.使用CountDownLatch阻塞当前交易处理线程，然后准备数据，异步请求服务端，交易处理完成后，再释放CountDownLatch。
     * <p>
     * Online request
     * - this method must return results synchronously, so there are two processing methods:
     * 1. All processing logic USES synchronous operation: prepare data, synchronize the request server,
     * and then synchronously return the transaction processing results;
     * 2. Block the current transaction processing thread with CountDownLatch, then prepare the data, asynchronously request the server,
     * and release the CountDownLatch after the transaction processing is completed.
     */
    private int onlineProcess() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvPrompt.setText("Transaction in progress, please wait...");
            }
        });

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        // 模拟网络请求      Analog network request
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            }
        }).start();

        try {
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Show successful transaction
     */
    private void showTransactionSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvTransResult.setText("Transaction Result:Success");
                LedUtils.getInstance().success();
            }
        });
    }

    /**
     * Show unsuccessful transaction
     */
    private void showTransactionFail(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvTransResult.setText("Transaction Result:Fail, " + msg);
                LedUtils.getInstance().error();
            }
        });
    }

    /**
     * Read card thread
     */
    class ReadCardThread extends Thread {

        ReadCardThread() {
        }

        @Override
        public void run() {
            try {
                CLog.log(TAG, "ReadCardThread|run");

                // 蜂鸣器 短鸣      The buzzer blared
                SDKInstance.mCore.buzzer();
                // 中断命令，强制结束之前没有正常返回的操作      Interrupt command to force the end of an operation that did not return normally
                SDKInstance.mBankCard.breakOffCommand();

                // SDK文档写的是 256      The SDK documentation says 256
                byte[] outData = new byte[512];
                int[] outDataLen = new int[1];
                int readCardResult = -1;

                mIsStopReadCard = false;

                while (true) {
                    /*
                        如果停止读卡标记改为true，则会跳出循环，停止读卡。
                        - 如果点击返回按钮，退出界面时；

                        If the stop reading tag is changed to true, the loop breaks and the card is stopped.
                        - if the back button is clicked, exit the interface;
                     */
                    if (mIsStopReadCard) {
                        break;
                    }

                    /*
                        首次等待读卡，直到读卡完成或超时结束，该方法默认会打开读卡器哦～
                        入参：
                        1. int cardType  卡类型，0x00：银行卡，0x01：行业卡
                        2. int cardMode  读卡模式
                            - 0x0100：接触卡
                            - 0x200：非接触卡
                            - 0x400：磁条卡
                        3. int timeOut       超时时间，单位s 60s
                        4. String appName    应用包名
                        出参：
                        1. byte[] Data      读卡数据
                        2. int[] retlen2    数据长度

                        Waiting for reading card
                        Params:
                        1. int cardType  card type, 0x00：bank card, 0x01：Business card
                        2. int cardMode  read card mode
                            - 0x0100:Integrated circuit(s) card
                            - 0x200:Non-Contacted IC Card
                            - 0x400:Magnetic Stripe Card
                        3. int timeOut       timeOut, Unit second
                        4. String appName    Application package name
                        Result:
                        1. byte[] Data      Card data
                        2. int[] retlen2    Data length
                     */
                    byte cardType;
                    byte CARD_TYPE_PRIORITY_MSG = 0x40;
                    cardType = BankCard.CARD_TYPE_NORMAL;
                    // nepal  特别定制读卡优先读取磁条卡信息 500ms后会检查非接 add 2019.08.02 by ysh
//                    cardType  = CARD_TYPE_PRIORITY_MSG;
                    readCardResult = SDKInstance.mBankCard.readCard(cardType, BankCard.CARD_MODE_PICC | BankCard.CARD_MODE_ICC | BankCard.CARD_MODE_MAG, 60, outData, outDataLen, "com.wiseasy.cashier.demo");
                    int i = BankCard.CARD_MODE_PICC | BankCard.CARD_MODE_ICC;
                    Log.d(TAG, "run:   " + i);
                    /*
                        如果读到卡数据，则会退出循环。
                        If the card data is read, the loop will be exited.
                     */
                    if (outData[0] == 0x00 || outData[0] == 0x05 || outData[0] == 0x07) {
                        break;
                    }
                }
                /*
                    readCardResult: 表示接口是否调用成功，0-成功，非0-失败，一般不会失败
                    outData[0]： 表示本次读卡结果

                    readCardResult: Indicates whether the interface call is successful, 0-success, non-0-failure, and generally does not fail
                    outData[0]： Represents the card reading result

                 */
                CLog.log(TAG, "readCardResult=" + readCardResult + ", outData[0]=" + outData[0]);
                if (readCardResult == 0) {
                    switch (outData[0]) {
                        case 0x00:
                            // 刷磁条卡成功      Brush magnetic strip card successfully
                            cardType = Process.CARD_TYPE_MAG;
                            processMSC(outData);
                            break;
                        case 0x01:
                            // 读卡失败     Card reading failed
                            showTransactionFail("Read card error");
                            break;
                        case 0x02:
                            // 刷磁条卡成功，加密处理失败    Swipe magnetic stripe card succeeded, encryption processing failed
                            showTransactionFail("Read card error");
                            break;
                        case 0x03:
                            // 没有寻到卡可以开始下次寻卡，意思是读卡超时 Time out
                            showTransactionFail("Time out");
                            break;
                        case 0x04:
                            // 取消读卡      Cancel card reading
                            CLog.log(TAG, "Cancel card reading");
                            break;
                        case 0x05:
                            // 接触式IC卡已插入      IC Card has been inserted
                            cardType = Process.CARD_TYPE_IC;
                            processContact();
                            break;
                        case 0x07:
                            // 检测到非接触式IC卡      Non-Contacted IC Card detected
                            cardType = Process.CARD_TYPE_PICC;
                            processContactLess();
                            break;
                        case 0x27:
                            // 多卡冲突：检测到多张非接IC卡      Multi-card conflict: multiple non-connected IC CARDS have been detected
                            showTransactionFail("Multiple Non-Contacted IC cards detected");
                            break;
                        default:
                            break;
                    }
                } else {
                    CLog.log(TAG, "Oh,My God! The mBankCard.readCard() interface call failed!!!");
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
    public boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);
    }

    private boolean isGranted_(String permission) {
        int checkSelfPermission = ActivityCompat.checkSelfPermission(this, permission);
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
    private void doPermissions(String permission, int requestCode) {

        if (!isGranted(permission)) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
//                Log.d(TAG, "doPermissions: shouldShowRequestPermission");
//            } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
//            }
        } else
        {
            try {
                addAID();
//                    SDKInstance.mEmvCore.delAllAID();
                addCAPK();
//                    addExceptionPan();com.wiseasy.cashier
//                    addRemovedCAPK();
                // Visa
                addPayWaveDRLData();
                // Master
                addPayPassTerminalConfig();
//                     AMEX
                addAMEXDRLData();
                addAMEXTerminalConfig();
                addDiscoverTerminalConfig();
//                    addPureTerminalConfig();
  /*
                        4.调用收单参数接口添加密钥
                    */

                    /*
                        方式一：配置一组密钥
                        - 密钥索引直接使用包名
                     */
                addAcquirerKey("com.wiseasy.cashier.demo");
                    /*
                        方式二：配置多组密钥，不同内核使用不同密钥
                        - 每个密钥的索引均不同，建议使用"包名+后缀"
                          a."visa"包下添加 mk/sk 及 ipek 各一组
                          b."mastercard"包下添加 mk/sk 及 ipek 各一组
                          c."pboc"包下添加 mk/sk 及 ipek 各一组
                     */
                //      addAcquirerKey("com.wiseasy.cashier.demo" + ".visa");
                //      addAcquirerKey("com.wiseasy.cashier.demo" + ".mastercard");
                //      addAcquirerKey("com.wiseasy.cashier.demo" + ".qpboc");

                // Other
                LedUtils.getInstance().init(SDKInstance.mCore);

                isInit = true;

                CLog.log(TAG, "Init Finish");

                initFinish(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    addAID();
//                    SDKInstance.mEmvCore.delAllAID();
                    addCAPK();
//                    addExceptionPan();com.wiseasy.cashier
//                    addRemovedCAPK();
                    // Visa
                    addPayWaveDRLData();
                    // Master
                    addPayPassTerminalConfig();
//                     AMEX
                    addAMEXDRLData();
                    addAMEXTerminalConfig();
                    addDiscoverTerminalConfig();
//                    addPureTerminalConfig();
  /*
                        4.调用收单参数接口添加密钥
                    */

                    /*
                        方式一：配置一组密钥
                        - 密钥索引直接使用包名
                     */
                    addAcquirerKey("com.wiseasy.cashier.demo");
                    /*
                        方式二：配置多组密钥，不同内核使用不同密钥
                        - 每个密钥的索引均不同，建议使用"包名+后缀"
                          a."visa"包下添加 mk/sk 及 ipek 各一组
                          b."mastercard"包下添加 mk/sk 及 ipek 各一组
                          c."pboc"包下添加 mk/sk 及 ipek 各一组
                     */
                    //      addAcquirerKey("com.wiseasy.cashier.demo" + ".visa");
                    //      addAcquirerKey("com.wiseasy.cashier.demo" + ".mastercard");
                    //      addAcquirerKey("com.wiseasy.cashier.demo" + ".qpboc");

                    // Other
                    LedUtils.getInstance().init(SDKInstance.mCore);

                    isInit = true;

                    CLog.log(TAG, "Init Finish");

                    initFinish(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Permission Denied
                Toast.makeText(MainActivity.this, "Please authorize", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}