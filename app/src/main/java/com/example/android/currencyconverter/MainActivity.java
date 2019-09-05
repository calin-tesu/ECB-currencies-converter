package com.example.android.currencyconverter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;
import java.util.TimeZone;

import static com.example.android.currencyconverter.Constants.EXCHANGE_RATES_XML;

public class MainActivity extends AppCompatActivity {

    private File file;
    private TextView timeOfExchangeRates;
    private EditText amountToConvert;
    private TextView convertedValuesTxt;
    private List<EcbCurrency> ecbCurrencyList;
    private ParseXML parseXML;

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

        //Download XML from API
        //The downloaded XML file will also solve the problem of local data persistence
        if (!file.exists() && !isNetworkAvailable()) {
            convertedValuesTxt.setText("Internet connectivity needed to download initial foreign" +
                    " currencies exchange values from European Central Bank.");
        } else if (!file.exists()) {
            Toast.makeText(getApplicationContext(), "Downloading initial data", Toast.LENGTH_SHORT).show();
            new DownloadExchangeRatesFromECB().execute();
        } else if (isNewRatesAvailable() && isNetworkAvailable()) {
            Toast.makeText(getApplicationContext(), "Downloading NEW DATA", Toast.LENGTH_SHORT).show();
            new DownloadExchangeRatesFromECB().execute();
        } else {
            Toast.makeText(getApplicationContext(), "File already exist!", Toast.LENGTH_SHORT).show();
            calculateExchangeRates();
        }
    }

    private void calculateExchangeRates() {
        String timeRates = parseXML.getCurrenciesTime();

        timeOfExchangeRates.setText(timeRates);

        ecbCurrencyList = parseXML.getCurrenciesValues();

        amountToConvert.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (amountToConvert.getText().toString().matches("")) {
                        Toast.makeText(getApplicationContext(), "Please enter a value!", Toast.LENGTH_SHORT).show();
                    } else {
                        double inputValue = Double.valueOf(amountToConvert.getText().toString());

                        printExchangeRates(ecbCurrencyList, inputValue);
                    }
                }
                return false;
            }
        });
    }

    private void printExchangeRates(List<EcbCurrency> currencies, double inputValue) {
        StringBuilder builder = new StringBuilder();
        String currencyName = "";

        for (EcbCurrency ecbCurrency : currencies) {
            double convertedValue = (ecbCurrency.value * inputValue);

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
            Toast.makeText(getApplicationContext(), "Download successful!!!", Toast.LENGTH_SHORT).show();
            calculateExchangeRates();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /*
    This method checks if there is new exchange rates published by ECB.
    Rates are published by European Central Bank every working day around 16:00 CET
    */
    //TODO find a solution to download the XML only in the working days of ECB
    /*Ex: On Saturday and Sunday the app will download the same XML downloaded on Friday
    * (which is valid until Monday at 16:00*/
    private boolean isNewRatesAvailable() {
        //Get the time of currencies rates specified in the downloaded XML
        String timeRates = parseXML.getCurrenciesTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar timeOfDownloadedXML = Calendar.getInstance();

        /*
        Set time of timeOfDownloadedXML to be the date of the last downloaded XML at time 00:00
        */
        try {
            timeOfDownloadedXML.setTime(simpleDateFormat.parse(timeRates));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Log.i("Date of downloaded XML", String.valueOf(timeOfDownloadedXML));

        /*
        Find the date and time until the current downloaded XML with the exchange rates are valid.
        For this we will add 40 hours to variable timeOfDownloadedXML (the date specified in the current downloaded XML at hour 00:00)
        to reach the next day at 16:00 (24h + 16h = 40h).
        */
        timeOfDownloadedXML.add(Calendar.HOUR, 40);
        Log.i("Date for new download", String.valueOf(timeOfDownloadedXML));

        /*
        If current date and time in Central Europe TimeZone is after timeOfDownloadedXML then
        there is a new XML to download
        */
        return Calendar.getInstance(TimeZone.getTimeZone("CET")).after(timeOfDownloadedXML);
    }
}
