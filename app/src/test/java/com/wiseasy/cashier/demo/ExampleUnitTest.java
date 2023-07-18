package com.wiseasy.cashier.demo;

import com.wiseasy.emvprocess.utils.ByteUtil;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        byte bytes = ByteUtil.ascii2Bcd("00")[0];
        System.out.println(bytes);
        System.out.println(ByteUtil.bytes2Int(new byte[]{bytes}));
        int i = Integer.parseInt("99", 16);
        System.out.println(i);
        System.out.println(Integer.parseInt("00",16));

    }
}