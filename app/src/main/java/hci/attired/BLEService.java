package hci.attired;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BLEService extends Service {

    private static final String TAG = "Attires Debug";

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

    private boolean isScanning = false;
    private int scanMode = ScanSettings.SCAN_MODE_BALANCED;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();

        store_ids.put("E1:20:1D:FC:88:D0","Zara");
        store_ids.put("00:07:80:C7:AF:7C","Topshop");

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleDev = bluetoothManager.getAdapter();

        if(scanner == null) {
            scanner = bleDev.getBluetoothLeScanner();
            if(scanner == null) {
                // probably tried to start a scan without granting Bluetooth permission
                Toast.makeText(this, "Failed to start scan (BT permission granted?)", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Failed to get BLE scanner instance");
            }
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(scanMode).build();
        scanner.startScan(filters, settings, bleScanCallback);
        isScanning = true;

        return START_STICKY;
    }

    public void setBeacon_ids(ArrayList<String> beacon_ids) {
        this.beacon_ids = beacon_ids;
    }

    public void setStore_ids(HashMap<String, String> store_ids) {
        this.store_ids = store_ids;
    }

    // class implementing BleScanner callbacks
    private ScanCallback bleScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            final BluetoothDevice dev = result.getDevice();
            final int rssi = result.getRssi();

            Log.d(TAG, dev.getAddress());
            if (dev != null && isScanning) {
                new Runnable() {
                    @Override
                    public void run() {
                        // retrieve device info and add to or update existing set of beacon data
                        String name = dev.getName();
                        String address = dev.getAddress();
                        Log.d(TAG, "Name of the BLE!!!!!! --> " + name);
                        Log.d(TAG, "Address of the BLE!!!!!! --> " + address);

                        if (store_ids.containsKey(address)) {
                            if (!beaconIds.containsKey(address)) {
                                beaconIds.put(address, new Timers(new Date()));
                                    Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    v.vibrate(100);

                                android.support.v4.app.NotificationCompat.Builder mBuilder =
                                        new NotificationCompat.Builder(getApplicationContext())
                                                .setSmallIcon(R.drawable.round_button)
                                                .setContentTitle("Attire for you")
                                                .setContentText("We found something maching you personality!");

                                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                Intent notificationIntent = new Intent(getApplicationContext(), NearBy.class);
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

                                mBuilder.setContentIntent(pendingIntent);
                                mNotificationManager.notify(001, mBuilder.build());


                            } else {
                                beaconIds.get(address).setLastUpdate(new Date());
                            }
                        }

                        //check for beacons who are now out of range to remove their content
                        checkBeacons();

                        count++;
                        for (int i = 0; i < beacon_ids.size(); i++) {
                            Log.d(TAG, beacon_ids.get(i) + " -- " + count);
                        }
//                        scanAdapter.update(dev, address, name == null ? address.toString(): name, rssi);
                    }
                }.run();
            }
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
            }
        }
        //update list of beacons in radius
        beaconIds.clear();
        beaconIds.putAll(tempBeaconIds);
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
            if(o != null && o instanceof BLEService.BeaconInfo) {
                BLEService.BeaconInfo other = (BLEService.BeaconInfo) o;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }
}