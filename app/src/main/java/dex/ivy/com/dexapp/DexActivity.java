package dex.ivy.com.dexapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DexActivity extends AppCompatActivity {

    private static final int DISCOVER_DURATION = 300;
    private static final int REQUEST_BLU = 1;
    String path;
    private EditText macAddressEdit;
    byte[] bytes;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//    private static final UUID SPP_UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        macAddressEdit =  findViewById(R.id.mac_address_edit);

        macAddressEdit.setText("4C:55:CC:1C:8E:59");

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.file_send);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            bytes = buffer.toByteArray();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void sendViaBluetooth(View v) {

        new ConnectDevice().execute();

    }

    private boolean connect(String macAddress) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice mBluetoothDevice = null;
        BluetoothSocket mBluetoothSocket = null;

        OutputStream mOutputStream = null;

        try {
            if (macAddress.equals(""))
                System.out.println("Mac address is empty...");
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(macAddress);
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            mBluetoothSocket.connect();

            mOutputStream = mBluetoothSocket.getOutputStream();
            mOutputStream.write(bytes);
            mOutputStream.flush();

            mOutputStream.close();

            mBluetoothSocket.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }

    class ConnectDevice extends AsyncTask<String, Void, Boolean> {

        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {

            String macAddress = macAddressEdit.getText().toString();

            return macAddress.trim().length() > 0 && connect(macAddress);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result)
                Toast.makeText(DexActivity.this, "File Processed", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(DexActivity.this, "Failed", Toast.LENGTH_SHORT).show();
        }

    }

    //logon printer self test gives the reverse mac address
    private String getReverseofMacAddress(String macAddress) {
        String mMAcAddress = "";

        String[] split = macAddress.split(":");
        List<String> list = Arrays.asList(split);
        Collections.reverse(list);
        for (int i = 0; i < list.size(); i++) {
            if (i != list.size() - 1)
                mMAcAddress = mMAcAddress + list.get(i) + ":";
            else
                mMAcAddress = mMAcAddress + list.get(i);
        }

        return mMAcAddress;
    }

    public void enableBluetooth() {
        Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVER_DURATION);
        startActivityForResult(discoveryIntent, REQUEST_BLU);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == DISCOVER_DURATION && requestCode == REQUEST_BLU) {

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("*/*");

            File f = new File(path);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));

            PackageManager pm = getPackageManager();
            List<ResolveInfo> appsList = pm.queryIntentActivities(intent, 0);

            if (appsList.size() > 0) {
                String packageName = null;
                String className = null;
                boolean found = false;

                for (ResolveInfo info : appsList) {
                    packageName = info.activityInfo.packageName;
                    if (packageName.equals("com.android.bluetooth")) {
                        className = info.activityInfo.name;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    Toast.makeText(this, "Bluetooth havn't been found",
                            Toast.LENGTH_LONG).show();
                } else {
                    intent.setClassName(packageName, className);
                    startActivity(intent);
                }
            }
        }else {
            Toast.makeText(this, "Bluetooth is cancelled", Toast.LENGTH_LONG)
                    .show();
        }
    }
}
