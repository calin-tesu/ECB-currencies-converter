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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class MainActivity extends AppCompatActivity {

    private static final String CURRENCY = "currency";
    private static final String CUBE_NODE = "//Cube/Cube/Cube";
    private static final String RATE = "rate";

    private static final String EXCHANGE_RATES_XML = "exchange_rates.xml";

    private EditText amountToConvert;
    private TextView textView;

    private List<EcbCurrency> ecbCurrencyList;

    File file;

    //TODO add a recyclerView to display the converted values

    //TODO add a Spinner and the logic to switch between currencies to convert (convert from USD
    //or from RON or GBP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        amountToConvert = findViewById(R.id.amount_to_convert);
        textView = findViewById(R.id.text);

        file = getBaseContext().getFileStreamPath(EXCHANGE_RATES_XML);
        if (!file.exists()) {
            Toast.makeText(getApplicationContext(), "Start download!!", Toast.LENGTH_SHORT).show();
            new DownloadExchangeRatesFromECB().execute();
        } else {
            Toast.makeText(getApplicationContext(), "File allready exist!", Toast.LENGTH_SHORT).show();
            ecbCurrencyList = parseXML();
        }

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

    private List<EcbCurrency> parseXML() {

        //https://stackoverflow.com/questions/50316974/how-to-read-an-online-xml-file-for-currency-rates-in-java
        List<EcbCurrency> ecbCurrencies = new ArrayList<>();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document document;

        try {
            InputStream inputStream = getApplicationContext().openFileInput(EXCHANGE_RATES_XML);
            document = builder.parse(inputStream);

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(CUBE_NODE);
            NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                NamedNodeMap nodeAttributes = node.getAttributes();
                if (nodeAttributes.getLength() > 0) {
                    Node currencyAttribute = nodeAttributes.getNamedItem(CURRENCY);
                    if (currencyAttribute != null) {
                        String currencyTxt = currencyAttribute.getNodeValue();
                        String rateTxt = nodeAttributes.getNamedItem(RATE).getNodeValue();
                        double rateValue = Double.parseDouble(rateTxt);
                        ecbCurrencies.add(new EcbCurrency(currencyTxt, rateValue));
                    }
                }
            }
        } catch (SAXException | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }

        return ecbCurrencies;
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

        textView.setText(builder.toString());
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
