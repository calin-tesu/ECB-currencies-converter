package com.example.android.currencyconverter;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.currencyconverter.Model.EcbCurrency;
import com.example.android.currencyconverter.utils.ParseXML;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Currency;
import java.util.List;

import static com.example.android.currencyconverter.Constants.EXCHANGE_RATES_XML;

public class MainActivity extends AppCompatActivity {

    private TextView timeOfExchangeRates;
    private EditText amountToConvert;
    private TextView convertedValuesTxt;

    private List<EcbCurrency> ecbCurrencyList;

    private ParseXML parseXML;

    File file;

    //TODO add a recyclerView to display the converted values

    //TODO add a Spinner and the logic to switch between currencies to convert (convert from USD
    //or from RON or GBP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeOfExchangeRates = findViewById(R.id.time_of_rates);
        amountToConvert = findViewById(R.id.amount_to_convert);
        convertedValuesTxt = findViewById(R.id.text);

        parseXML = new ParseXML(getApplicationContext());

        file = getBaseContext().getFileStreamPath(EXCHANGE_RATES_XML);
        if (!file.exists()) {
            Toast.makeText(getApplicationContext(), "Start download!!", Toast.LENGTH_SHORT).show();
            new DownloadExchangeRatesFromECB().execute();
        } else {
            Toast.makeText(getApplicationContext(), "File allready exist!", Toast.LENGTH_SHORT).show();
            //parseXML = new ParseXML(getApplicationContext());
            ecbCurrencyList = parseXML.getCurrenciesValues();
        }

        timeOfExchangeRates.setText(parseXML.getCurrenciesTime());

        amountToConvert.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (amountToConvert.getText().toString().matches("")) {
                        Toast.makeText(getApplicationContext(), "Please enter a value!", Toast.LENGTH_SHORT).show();
                    } else {
                        int inputValue = Integer.valueOf(amountToConvert.getText().toString());

                        printExchangeRates(ecbCurrencyList, inputValue);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void printExchangeRates(List<EcbCurrency> currencies, int inputValue) {
        StringBuilder builder = new StringBuilder();
        String currencyName = "";

        for (EcbCurrency ecbCurrency : currencies) {
            //will use the integer values of exchange rates conversions for simplicity
            int convertedValue = (int) (ecbCurrency.value * inputValue);

            //Gets the name that is suitable for displaying this currency from the currency code
            Currency currency = Currency.getInstance(ecbCurrency.name);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                currencyName = currency.getDisplayName();
            }

            builder.append(" = ").append(convertedValue).append("  ").
                    append(ecbCurrency.name).append(" - ").
                    append(currencyName).
                    append("\n");
        }

        convertedValuesTxt.setText(builder.toString());
    }

    class DownloadExchangeRatesFromECB extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {
            int count;
            try {
                URL url = new URL("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml?ecf1fe18196a2eeb131d673c7c204355");

                URLConnection connection = url.openConnection();
                connection.connect();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream
                FileOutputStream output = openFileOutput(EXCHANGE_RATES_XML, Context.MODE_PRIVATE);

                byte[] data = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(getApplicationContext(),"Download successful!!!", Toast.LENGTH_SHORT).show();
        }
    }
}
