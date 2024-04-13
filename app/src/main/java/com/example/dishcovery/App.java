package com.example.dishcovery;

import android.app.Application;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;
import com.paypal.checkout.createorder.CurrencyCode;
import com.paypal.checkout.createorder.UserAction;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PayPalCheckout.setConfig(new CheckoutConfig(
                this,
                "AdmiBfK5VjxEmoqpDviFGPWMnbRl2EBBvDCCoP0yrn_65PPWDcq8FLWE9AY_1E_3zH-W7nOGJHh0-wt3",
                Environment.SANDBOX,
                CurrencyCode.USD,
                UserAction.PAY_NOW,
                "com.example.dishcovery://paypalpay"
        ));
    }
}
