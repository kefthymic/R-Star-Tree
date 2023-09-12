import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConvertOsm {
    private Document osmFile;
    private File csvFile;
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;
    private BufferedWriter csvWriter;
    private String[] header={"Node id", "Name", "Lat", "Lon"};



    private ArrayList<String> getAllTheIds(Document doc) {
        XPathFactory xpathFactory = XPathFactory.newInstance(); // Create XPathFactory object
        XPath xpath = xpathFactory.newXPath(); // Create XPath object
        ArrayList<String> id= new ArrayList<>();

        try {
            XPathExpression expr = xpath.compile("/osm/node/@id");
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                id.add(nodes.item(i).getNodeValue());
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }


        return id;
    }

    private ArrayList<String> getAllTheLat(Document doc, ArrayList<String> id){
        String tempId;
        XPathFactory xpathFactory = XPathFactory.newInstance(); // Create XPathFactory object
        XPath xpath = xpathFactory.newXPath(); // Create XPath object
        ArrayList<String> lat= new ArrayList<>();

        try {
            for(int i=0;i<id.size();i++){
                tempId= id.get(i);
                XPathExpression expr = xpath.compile("/osm/node[@id='" + tempId + "']/@lat");
                String temp = (String) expr.evaluate(doc, XPathConstants.STRING);
                lat.add(temp);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return lat;
    }

    private ArrayList<String> getAllTheLon(Document doc, ArrayList<String> id){
        String tempId;
        XPathFactory xpathFactory = XPathFactory.newInstance(); // Create XPathFactory object
        XPath xpath = xpathFactory.newXPath(); // Create XPath object
        ArrayList<String> lon= new ArrayList<>();

        try {
            for(int i=0;i<id.size();i++){
                tempId= id.get(i);
                XPathExpression expr = xpath.compile("/osm/node[@id='" + tempId + "']/@lon");
                String temp = (String) expr.evaluate(doc, XPathConstants.STRING);
                lon.add(temp);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return lon;
    }

    private ArrayList<String> getAllTheNames(Document doc, ArrayList<String> id){
        ArrayList<String> names= new ArrayList<>();

        String tempId;
        XPathFactory xpathFactory = XPathFactory.newInstance(); // Create XPathFactory object
        XPath xpath = xpathFactory.newXPath(); // Create XPath object

        try {
            NodeList help;
            for(int i=0;i<id.size();i++){
                tempId= id.get(i);

                XPathExpression expr= xpath.compile("/osm/node[@id='" + tempId + "']/tag[starts-with(@k, 'name')]/@v");
                help= (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                if( help.getLength()>0){
                    String temp = (String) expr.evaluate(doc, XPathConstants.STRING);
                    names.add(temp);
                }
                else {
                    names.add(null);
                }
            }

        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return names;
    }

    public ConvertOsm(String osmFileName){
        this.factory = DocumentBuilderFactory.newInstance();
        this.factory.setNamespaceAware(true);

        try{
            this.builder = factory.newDocumentBuilder();
            this.osmFile= builder.parse(osmFileName);

            this.csvFile= new File("dataFile.csv"); //create the csv File
            csvWriter = new BufferedWriter(new FileWriter(csvFile));

            ArrayList<String> id = getAllTheIds(osmFile);
            ArrayList<String> names = getAllTheNames(osmFile, id);
            ArrayList<String> lat = getAllTheLat(osmFile, id);
            ArrayList<String> lon = getAllTheLon(osmFile, id);

            for(int i=0;i<id.size();i++){
                csvWriter.write(id.get(i));
                csvWriter.write(",");
                csvWriter.write(lat.get(i));
                csvWriter.write(",");
                csvWriter.write(lon.get(i));
                csvWriter.write(",");
                if(names.get(i)==null){
                    csvWriter.write("");
                }else{
                    csvWriter.write(names.get(i));
                }

                csvWriter.newLine();
            }

            csvWriter.close();

        }catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }
}
