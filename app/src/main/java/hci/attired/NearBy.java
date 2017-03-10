package hci.attired;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

    private static final String[] SHOPPINGLIST = new String[] {
            "Jeans", "Jacket", "Shirt", "T-Shirt", "Shoes"
    };

    //list of store id's
    private HashMap<String,String> store_ids = new HashMap<String,String>();
    int count=0;

    //list of beacon_ids - delete
    private ArrayList<String> beacon_ids = new ArrayList<String>();

    //new Design to remove data
    private HashMap<String,Timers> beaconIds = new HashMap<String,Timers>();

    //ble
    private BluetoothAdapter bleDev = null;
    private BluetoothLeScanner scanner = null;
    // request ID for enabling Bluetooth
    private static final int REQUEST_ENABLE_BT = 1000;

    private boolean isScanning = false;
    private int scanMode = ScanSettings.SCAN_MODE_BALANCED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        setContentView(R.layout.activity_near_by);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        list         = new ArrayList<>();

        gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new CustomAdapter(this, list);
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(ATTIRE_STORAGE, MODE_PRIVATE);
        item = prefs.getString("item1", "");

        //ACTION BAR STUFF

        android.support.v7.app.ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

        ArrayAdapter<String> adapterAC = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line , SHOPPINGLIST);

        final AutoCompleteTextView search = (AutoCompleteTextView) findViewById(R.id.shoppingListNearBy);
        search.setAdapter(adapterAC);

        ImageButton addItems = (ImageButton) findViewById(R.id.addItems);

        TextView tv  = (TextView) findViewById(R.id.nearByText);

        addItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setVisibility(View.INVISIBLE);
                search.setVisibility(View.VISIBLE);
                search.callOnClick();

            }
        });


        search.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(v.getWindowToken(), 0);
                search.showDropDown();
            }
        });

        search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                tv.setVisibility(View.VISIBLE);
                search.setVisibility(View.INVISIBLE);
                item = search.getText().toString();
                Log.w(TAG, item);
                list.clear();
                Iterator<String> keys = beaconIds.keySet().iterator();
                while (keys.hasNext()){
                    String BeaconID = keys.next();
                    try{
                        parseXMLFile(store_ids.get(BeaconID));
                        updateRecyclerView();
                    }catch (XmlPullParserException e){
                        e.printStackTrace();
                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.FAB);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
                finish();
            }
        });


        //BLUETOOTH STUFF

        //Set store_ids
        store_ids.put("E1:20:1D:FC:88:D0","Zara");
        store_ids.put("00:07:80:C7:AF:7C","Topshop");


        //=========================================================BLE======================================
        // retrieve the BluetoothManager instance and check if Bluetooth is enabled. If not the
        // user will be prompted to enable it and the response will be checked in onActivityResult
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleDev = bluetoothManager.getAdapter();
        if (bleDev == null || !bleDev.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if(scanner == null) {
            scanner = bleDev.getBluetoothLeScanner();
            if(scanner == null) {
                // probably tried to start a scan without granting Bluetooth permission
                Toast.makeText(this, "Failed to start scan (BT permission granted?)", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Failed to get BLE scanner instance");
                return;
            }
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(scanMode).build();
        scanner.startScan(filters, settings, bleScanCallback);
        isScanning = true;

        //startService();

    }

    public void startService() {
        startService(new Intent(getBaseContext(), BLEService.class));
    }

    public void stopService() {
        stopService(new Intent(getBaseContext(), BLEService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop any in-progress scan and stop updating the graph if activity is paused
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        String scanning = "not scanning";
        if(!isScanning){
            scanning = "scanning";
            isScanning = true;
            beacon_ids.clear(); //clear all data retreived from beacons as user may be in a different location when restarting app
            Log.i(TAG, "Scan started");
            scanner.startScan(bleScanCallback);
        }
        Toast.makeText(this, "Resumed & " + scanning, Toast.LENGTH_SHORT).show();
    }

    private void stopScan() {
        if(scanner != null && isScanning) {
            //Toast.makeText(this, "Stopping BLE scan...", Toast.LENGTH_SHORT).show();
            isScanning = false;
            beacon_ids.clear(); //clear all data retreived from beacons as user may be in a different location when restarting app
            Log.i(TAG, "Scan stopped");
            scanner.stopScan(bleScanCallback);
        }
    }

    private void updateRecyclerView(){
        adapter.notifyItemInserted(list.size() - 1);
        adapter.notifyDataSetChanged();
    }


    // class implementing BleScanner callbacks
    private ScanCallback bleScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            final BluetoothDevice dev = result.getDevice();
            final int rssi = result.getRssi();

            Log.d(TAG,dev.getAddress());
            if(dev != null && isScanning) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // retrieve device info and add to or update existing set of beacon data
                        String name = dev.getName();
                        String address = dev.getAddress();
                        Log.d(TAG,"Name of the BLE!!!!!! --> "+ name);
                        Log.d(TAG,"Address of the BLE!!!!!! --> "+ address);

                        if (store_ids.containsKey(address)){
                            if (!beaconIds.containsKey(address)){
                                beaconIds.put(address,new Timers(new Date()));
                                try{
                                    parseXMLFile(store_ids.get(address));
                                    updateRecyclerView();
                                }catch (XmlPullParserException e){
                                    e.printStackTrace();
                                }catch (FileNotFoundException e){
                                    e.printStackTrace();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }
                            else{
                                beaconIds.get(address).setLastUpdate(new Date());
                            }
                        }


                        //check for beacons who are now out of range to remove their content
                        checkBeacons();


                        count++;
                        for (int i=0; i<beacon_ids.size(); i++){
                            Log.d(TAG,beacon_ids.get(i)+" -- "+count);
                        }
//                        scanAdapter.update(dev, address, name == null ? address.toString(): name, rssi);
                    }

                });
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "BatchScanResult(" + results.size() + " results)");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.w(TAG, "ScanFailed(" + errorCode + ")");
        }
    };

    private synchronized void checkBeacons() {
        HashMap<String, Timers> tempBeaconIds = new HashMap<String, Timers>();
        tempBeaconIds.putAll(beaconIds);
        Iterator<String> keys = beaconIds.keySet().iterator();
        while (keys.hasNext()){
            String beaconID = keys.next();
            long before = beaconIds.get(beaconID).getLastUpdate().getTime()/1000;
            long after = new Date().getTime()/1000;
            if (after - before > 10){
                tempBeaconIds.remove(beaconID);
                remove_shops_content(store_ids.get(beaconID));
            }
        }
        //update list of beacons in radius
        beaconIds.clear();
        beaconIds.putAll(tempBeaconIds);
    }

    private void remove_shops_content(final String shop_name) {
        for (int i=0; i<list.size(); i++){
            if (list.get(i).getShop_name().compareTo(shop_name)==0){
                list.remove(i);
                i--;
            }
        }
        updateRecyclerView();
    }


    private class BeaconInfo {
        public BluetoothDevice device;
        public String address;
        public String name;
        public int rssi;

        private static final int WINDOW_SIZE = 9;
        private int[] window = new int[WINDOW_SIZE];
        private int windowptr = 0;

        public BeaconInfo(BluetoothDevice device, String address, String name, int rssi) {
            this.device = device;
            this.address = address;
            this.name = name;
            this.rssi = rssi;
            for(int i=0;i<WINDOW_SIZE;i++)
                this.window[i] = rssi;
        }

        // called when a new scan result for this beacon is parsed
        public void updateRssi(int newRssi) {
            this.rssi = newRssi;
            window[windowptr] = newRssi;
            windowptr = (windowptr + 1) % WINDOW_SIZE;
        }

        // returns the latest raw RSSI reading for this beacon
        public double getRssi() {
            return this.rssi;
        }

        // returns a very simple moving average of the last WINDOW_SIZE
        // RSSI values received for this beacon
        public double getFilteredRssi() {
            double mean = 0.0;
            for(int i=0;i<WINDOW_SIZE;i++) {
                mean += window[i];
            }
            mean /= WINDOW_SIZE;
            return mean;
        }

        @Override
        public boolean equals(Object o) {
            // test if beacon objects are equal using their addresses
            if(o != null && o instanceof BeaconInfo) {
                BeaconInfo other = (BeaconInfo) o;
                if(other.address.equals(address))
                    return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            // as with equals() use addresses to test equality
            return address.hashCode();
        }
    }

    private void parseXMLFile(String shop_name) throws XmlPullParserException, IOException {

        boolean found = false;

        Log.d(TAG,"Shope names ->>>>>>>>>>>>> "+shop_name);
        try {
            InputStream is = getAssets().open(shop_name+".xml");

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
                    found = true;
                    Item data = new Item(i, name, amount, url, "Size: " + sSize + "\nPrice: " + amount + "\nShop: "+shop_name, shop_name);
                    list.add(data);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
