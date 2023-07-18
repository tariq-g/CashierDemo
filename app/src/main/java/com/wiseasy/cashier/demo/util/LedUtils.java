package com.wiseasy.cashier.demo.util;

import android.os.RemoteException;

import wangpos.sdk4.libbasebinder.Core;

public class LedUtils {

    public static final LedUtils LEDUTILS = new LedUtils();

    public static LedUtils getInstance() {
        return LEDUTILS;
    }

    /**
     * BIT0: LED 1，绿 3
     * BIT1: LED 2，蓝 1
     * BIT2: LED 3，红 4
     * BIT3: LED 4，黄 2
     */
    private Core core;

    public void init(Core core) {
        this.core = core;
    }

    //输金额 等卡 蓝
    public void waitting() {
        try {
            core.ledFlash(1, 0, 0, 0, 0xFFFF, 0, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //卡片处理黄
    public int cardData() {
        try {
            int ret = core.ledFlash(0, 1, 0, 0, 0xFFFF, 0, 0);
            return ret;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    boolean connectFlag;

    //通讯 闪烁绿
    public int connect() {
        try {
            return core.ledFlash(0, 0, 1, 0, 200, 200, 0xFFFF);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //成功 绿
    public int success() {
        connectFlag = false;
        try {
            return core.ledFlash(0, 0, 1, 0, 0xFFFF, 0, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //失败 红
    public int error() {
        connectFlag = false;
        try {
            return core.ledFlash(0, 0, 0, 1, 0xFFFF, 0, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //
    public int close() {
        connectFlag = false;
        try {
            int ret = core.ledFlash(0, 0, 0, 0, 0, 0, 0);
            return ret;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }
}