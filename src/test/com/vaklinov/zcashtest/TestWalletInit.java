package com.vaklinov.zcashtest;

import cash.koto.daemon.windows.CheckAndInit;
import com.vaklinov.zcashui.Log;

/**
 * 2018-01-31
 *
 * @author KostaPC
 * c0f3.net
 */
public class TestWalletInit {
    public static void main(String[] args) {
        CheckAndInit checkAndInit = new CheckAndInit();
        checkAndInit.process(Log.getUsersConsole());
    }
}
