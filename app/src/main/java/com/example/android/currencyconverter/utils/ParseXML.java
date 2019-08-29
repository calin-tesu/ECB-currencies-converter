package com.example.android.currencyconverter.utils;

import android.content.Context;

import com.example.android.currencyconverter.Constants;
import com.example.android.currencyconverter.Model.EcbCurrency;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
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

/**
 * Created by Calin Tesu on 8/29/2019.
 */
public class ParseXML {

    private Context mContext;

    public ParseXML(Context context) {
        mContext = context;
    }

    public List<EcbCurrency> getCurrenciesValues() {

        List<EcbCurrency> ecbCurrencies = new ArrayList<>();
        Document document = createDOM();

        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(Constants.CUBE_NODE_CURRENCIES);
            NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                NamedNodeMap nodeAttributes = node.getAttributes();
                if (nodeAttributes.getLength() > 0) {
                    Node currencyAttribute = nodeAttributes.getNamedItem(Constants.CURRENCY);
                    if (currencyAttribute != null) {
                        String currencyTxt = currencyAttribute.getNodeValue();
                        String rateTxt = nodeAttributes.getNamedItem(Constants.RATE).getNodeValue();
                        double rateValue = Double.parseDouble(rateTxt);
                        ecbCurrencies.add(new EcbCurrency(currencyTxt, rateValue));
                    }
                }
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return ecbCurrencies;
    }

    public String getCurrenciesTime() {

        String timeTxt = null;
        Document document = createDOM();

        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(Constants.CUBE_NODE_TIME);
            NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            Node node = nl.item(0);
            NamedNodeMap nodeAttributes = node.getAttributes();
            if (nodeAttributes.getLength() > 0) {
                Node timeAttribute = nodeAttributes.getNamedItem(Constants.CURRENCY);
                if (timeAttribute != null) {
                    timeTxt = timeAttribute.getNodeValue();
                }
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return timeTxt;
    }

    private Document createDOM() {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        Document document = null;

        InputStream inputStream = null;
        try {
            inputStream = mContext.getApplicationContext().openFileInput(Constants.EXCHANGE_RATES_XML);
            document = builder.parse(inputStream);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }

        return document;
    }
}
