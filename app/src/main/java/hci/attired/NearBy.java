package hci.attired;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class NearBy extends AppCompatActivity {

    public static final String ATTIRE_STORAGE = "attire_storage";
    private static final String TAG = "Attires Debug";

    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private CustomAdapter adapter;
    private List<Item> list;

    private String item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_by);

        SharedPreferences prefs = getSharedPreferences(ATTIRE_STORAGE, MODE_PRIVATE);
        item = prefs.getString("item1", "");

        TextView textView = (TextView) findViewById(R.id.nearByTitle);
        textView.setText("Near by " + item);


        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        list         = new ArrayList<>();

        try {
            parseXMLFile();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new CustomAdapter(this, list);
        recyclerView.setAdapter(adapter);
    }

    private void parseXMLFile() throws XmlPullParserException, IOException {

        try {
            InputStream is = getAssets().open("zara.xml");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            Element element = doc.getDocumentElement();
            element.normalize();

            NodeList items  = doc.getElementsByTagName("Item");
            NodeList prices = doc.getElementsByTagName("Price");
            NodeList images = doc.getElementsByTagName("Image");
            NodeList sizes  = doc.getElementsByTagName("Size");
            NodeList descriptions  = doc.getElementsByTagName("Sex");

            for (int i=0; i<items.getLength(); i++) {

                String name   = "";
                String amount = "";
                String url    = "";
                String sSize   = "";
                String description = "";

                Node item  = items.item(i);
                Node price = prices.item(i);
                Node image = images.item(i);
                Node size  = sizes.item(i);
                Node desc  = descriptions.item(i);


                if (item.getNodeType() == Node.ELEMENT_NODE) {
                    Element e   = (Element) item;
                     name = e.getTextContent();
                }

                if (price.getNodeType() == Node.ELEMENT_NODE) {
                    Element e    = (Element) price;
                    amount = e.getTextContent();
                }

                if (image.getNodeType() == Node.ELEMENT_NODE) {
                    Element e   = (Element) image;
                    url  = e.getTextContent();
                }

                if (size.getNodeType() == Node.ELEMENT_NODE) {
                    Element e          = (Element) size;
                    sSize = e.getTextContent();
                }

                if (size.getNodeType() == Node.ELEMENT_NODE) {
                    Element e          = (Element) desc;
                    description = e.getTextContent();
                }

                if(name.compareTo(this.item) == 0) {
                    Item data = new Item(i, name, amount, url, "Size: " + sSize + "\nPrice: " + amount);
                    list.add(data);
                }
            }

        } catch (Exception e) {e.printStackTrace();}

    }
}
