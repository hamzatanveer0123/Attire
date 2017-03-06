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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private BluetoothAdapter bleDev = null;
    private BluetoothLeScanner scanner = null;
    //    private ScanResultArrayAdapter scanAdapter = null;
    private Handler graphHandler = null;
    private Button toggleScan = null;


    // request ID for enabling Bluetooth
    private static final int REQUEST_ENABLE_BT = 1000;

    private boolean isScanning = false;
    private int scanMode = ScanSettings.SCAN_MODE_BALANCED;

    private int lastx = 0;

    // currently selected beacon, if any
    private BeaconInfo selectedBeacon = null;

    private static final String[] SHOPPINGLIST = new String[] {
            "Jeans", "Jacket", "Shirt", "T-Shirt", "Shoes"
    };

    public static final String ATTIRE_STORAGE = "attire_storage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.statsbar));

        setContentView(R.layout.activity_fullscreen);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line , SHOPPINGLIST);

        final AutoCompleteTextView search = (AutoCompleteTextView) findViewById(R.id.shoppingList);
        search.setAdapter(adapter);

        Button searchBtn = (Button) findViewById(R.id.searchBtn);

        searchBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String selected = search.getText().toString();
                Toast.makeText(getApplicationContext(), selected, Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = getSharedPreferences(ATTIRE_STORAGE, MODE_PRIVATE).edit();
                editor.putString("item1", selected);
                editor.commit();

                Intent intent = new Intent(getApplicationContext(), NearBy.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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

        // retrieve the BluetoothManager instance and check if Bluetooth is enabled. If not the
        // user will be prompted to enable it and the response will be checked in onActivityResult
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleDev = bluetoothManager.getAdapter();
        if (bleDev == null || !bleDev.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if(scanner == null) {
            Log.d(TAG,"running1");
            scanner = bleDev.getBluetoothLeScanner();
            Log.d(TAG,"running2");
            if(scanner == null) {
                Log.d(TAG,"running3");
                // probably tried to start a scan without granting Bluetooth permission
                Toast.makeText(this, "Failed to start scan (BT permission granted?)", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Failed to get BLE scanner instance");
                return;
            }
        }

        Toast.makeText(this, "Starting BLE scan...", Toast.LENGTH_SHORT).show();

        // clear old scan results
//        scanAdapter.clear();

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(scanMode).build();
        scanner.startScan(filters, settings, bleScanCallback);
        isScanning = true;


//        // create the adapter used to populate the list of beacons and attach it to the widget
//        scanAdapter = new ScanResultArrayAdapter(this);
//        final ListView scanResults = (ListView) findViewById(R.id.listResults);
//        scanResults.setAdapter(scanAdapter);
//
//        // set up a click handler which sets/updates the selected beacon
//        scanResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//                // retrieve the adapter and get the selected item from it
//                ScanResultArrayAdapter adapter = (ScanResultArrayAdapter) adapterView.getAdapter();
//                BeaconInfo newSelectedBeacon = adapter.getItem(position);
//                Log.i(TAG, "Tapped beacon " + newSelectedBeacon.name);
//                if(!isScanning)
//                    return;
//
//                // if the selected beacon is being changed
//                if(selectedBeacon == null || !newSelectedBeacon.equals(selectedBeacon)) {
//                    // reset both data series (doesn't seem to be a clear() method...)
//                    DataPoint[] dp = new DataPoint[1];
//                    dp[0] = new DataPoint(lastx, newSelectedBeacon.rssi);
//                    rawRssi.resetData(dp);
//                    basicFilteredRssi.resetData(dp);
//
//                    // ask for a redraw of the list widget so that the selected item highlight
//                    // is updated to match the new item
//                    scanResults.invalidateViews();
//                }
//
//                selectedBeacon = newSelectedBeacon;
//            }
//
//        });

        // set up a handler for taps on the start/stop scanning button
//        toggleScan = (Button) findViewById(R.id.btnToggleScan);
//        toggleScan.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                toggleScan();
//            }
//
//        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop any in-progress scan and stop updating the graph if activity is paused
        stopScan();
    }

    private void stopScan() {
        if(scanner != null && isScanning) {
            Toast.makeText(this, "Stopping BLE scan...", Toast.LENGTH_SHORT).show();
            isScanning = false;
            Log.i(TAG, "Scan stopped");
            scanner.stopScan(bleScanCallback);
        }

        selectedBeacon = null;
//        toggleScan.setText("Start scanning");
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


//    // basic adapter class to act as a data source for the list widget
//    private class ScanResultArrayAdapter extends BaseAdapter {
//
//        private final Context context;
//        private final HashMap<String, BeaconInfo> data;
//        private final List<String> keys;
//
//        public ScanResultArrayAdapter(Context context) {
//            super();
//            this.context = context;
//            this.keys = new ArrayList<>();
//            this.data = new HashMap<>();
//        }
//
//        public void clear() {
//            data.clear();
//            keys.clear();
//            notifyDataSetChanged();
//        }
//
//        @Override
//        public BeaconInfo getItem(int position) {
//            return data.get(keys.get(position));
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return position;
//        }
//
//        @Override
//        public int getCount() {
//            return data.size();
//        }
//
//        // updates the dataset with a new scan result. may create a new BeaconInfo object or update
//        // an existing one.
//        public void update(BluetoothDevice beaconDevice, String beaconAddress, String beaconName, int beaconRssi) {
//            if(data.containsKey(beaconAddress)) {
//                data.get(beaconAddress).updateRssi(beaconRssi);
//            } else {
//                BeaconInfo info = new BeaconInfo(beaconDevice, beaconAddress, beaconName, beaconRssi);
//                data.put(beaconAddress, info);
//                keys.add(beaconAddress);
//            }
//            notifyDataSetChanged();
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            View row;
//            if(convertView == null) {
//                row = inflater.inflate(R.layout.layout_scanresult, parent, false);
//            } else {
//                row = convertView;
//            }
//
//            // manually set the contents of each of the labels
//            BeaconInfo info = data.get(keys.get(position));
//
//            // if this happens to be the selected beacon, change the background colour to highlight it
//            if(selectedBeacon != null && info.equals(selectedBeacon))
//                row.setBackgroundColor(Color.argb(64, 0, 255, 0));
//            else
//                row.setBackgroundColor(Color.argb(255, 255, 255, 255));
//
//            return row;
//        }
//    }

}