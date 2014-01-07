package androidstudio.pl.bluetoothconnection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DevicesListActivity extends Activity {

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String TAG_LOG = "DevicesListActivity";
    private ArrayAdapter<String> mPairedDevicesAdapter;
    private ArrayAdapter<String> mNewdevicesArrayAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);

        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscovery();
                scanButton.setEnabled(false);
            }
        });

        mPairedDevicesAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewdevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        final ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesAdapter);
        pairedListView.setOnItemClickListener(mDeviceCliskListener);

        final ListView newDeviceListView = (ListView) findViewById(R.id.new_devices);
        newDeviceListView.setAdapter(mNewdevicesArrayAdapter);
        newDeviceListView.setOnItemClickListener(mDeviceCliskListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReciver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReciver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final Set<BluetoothDevice> pairedDeviecs = mBluetoothAdapter.getBondedDevices();
        if (pairedDeviecs.size() > 0) {
            for (BluetoothDevice device : pairedDeviecs) {
                mPairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    private final BroadcastReceiver mReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewdevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                scanButton.setEnabled(true);
            }
        }
    };
    private final ListView.OnItemClickListener mDeviceCliskListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mBluetoothAdapter.cancelDiscovery();
            final String info = ((TextView) view).getText().toString();
            final Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, info.substring(info.length() - 17));
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private void doDiscovery() {

        mNewdevicesArrayAdapter.clear();
        Log.w(TAG_LOG, "doDiscovery");
        setProgressBarIndeterminateVisibility(true);
        setTitle("Wyszukiwanie");
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG_LOG, "onDestroy");
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReciver);
    }
}
