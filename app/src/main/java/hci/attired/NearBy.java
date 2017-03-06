package hci.attired;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.statsbar));
        setContentView(R.layout.activity_near_by);

        SharedPreferences prefs = getSharedPreferences(ATTIRE_STORAGE, MODE_PRIVATE);
        item = prefs.getString("item1", "");

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


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.FAB);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

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
