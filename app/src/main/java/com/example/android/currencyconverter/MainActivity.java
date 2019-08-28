package com.example.android.currencyconverter;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    private Button checkButton;
    private TextView textView;

    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkButton = findViewById(R.id.checkButton);
        textView = findViewById(R.id.text);

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForFile();
            }
        });
    }

    private void checkForFile() {

        file = getBaseContext().getFileStreamPath(EXCHANGE_RATES_XML);

        if (!file.exists()) {
            Toast.makeText(getApplicationContext(), "Start download!!", Toast.LENGTH_SHORT).show();
            new DownloadExchangeRatesFromECB().execute();
        } else {
            Toast.makeText(getApplicationContext(), "File allready exist!", Toast.LENGTH_SHORT).show();
            parseXML();
        }
    }

    private void parseXML() {

        //https://stackoverflow.com/questions/50316974/how-to-read-an-online-xml-file-for-currency-rates-in-java
        List<EcbCurrency> ecbCurrencyList = new ArrayList<>();
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
                        ecbCurrencyList.add(new EcbCurrency(currencyTxt, rateValue));
                    }
                }
            }
        } catch (SAXException | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }

        printExchangeRates(ecbCurrencyList);
        }

    private void printExchangeRates(List<EcbCurrency> currencies) {
        StringBuilder builder = new StringBuilder();

        for (EcbCurrency ecbCurrency : currencies) {
            builder.append(ecbCurrency.name).append("\n").
                    append(ecbCurrency.value).append("\n");
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
