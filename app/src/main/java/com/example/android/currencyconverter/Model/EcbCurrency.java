package com.example.android.currencyconverter.Model;

/**
 * Created by Calin Tesu on 8/28/2019.
 */
public class EcbCurrency {

    public String name;
    public double value;

    public EcbCurrency(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public EcbCurrency() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
