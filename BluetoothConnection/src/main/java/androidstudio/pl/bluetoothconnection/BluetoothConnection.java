package androidstudio.pl.bluetoothconnection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothConnection extends Activity implements View.OnClickListener {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private static final String LOG_TAG = "BluetoothConnection";
    private static final int REQUEST_CONNECTED_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECTED_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private TextView mTitle;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService bluetoothService;
    private String mConnectedDeviceName;
    private GameEngine gameEngine;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(LOG_TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_bluetooth_connection);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        gameEngine = new GameEngine();
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        if (surfaceHolder != null) {
            surfaceHolder.addCallback(surfaceViewCallBack);
        }

        if (mBluetoothAdapter == null) {
            Log.w(LOG_TAG, "Bluetooth is not available");
            finish();
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w(LOG_TAG, "onStart");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.w(LOG_TAG, "Bluetooth is disabled");
            final Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        } else {
            Log.w(LOG_TAG, "Bluetooth is enabled");
            if (bluetoothService == null) {
                setupInterface();
            }
        }
    }

    private void setupInterface() {
        final Button mButtonLeft = (Button) findViewById(R.id.btnLeft);
        final Button mButtonTop = (Button) findViewById(R.id.btnTop);
        final Button mButtonRight = (Button) findViewById(R.id.btnRight);
        final Button mButtonBottom = (Button) findViewById(R.id.btnBottom);
        mButtonLeft.setOnClickListener(this);
        mButtonTop.setOnClickListener(this);
        mButtonRight.setOnClickListener(this);
        mButtonBottom.setOnClickListener(this);

        bluetoothService = new BluetoothService(this, handler);

    }

    @Override
    protected synchronized void onResume() {
        super.onStart();
        Log.w(LOG_TAG, "onResume");
        if (bluetoothService != null) {
            if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                bluetoothService.start();
            }
        }
    }

    @Override
    protected synchronized void onPause() {
        super.onPause();
        Log.w(LOG_TAG, "onPause");
    }

    @Override
    protected synchronized void onStop() {
        super.onStop();
        Log.w(LOG_TAG, "onStop");
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
        finish();
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(LOG_TAG, "onDestroy");
        this.unregisterReceiver(mReceiver);
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(LOG_TAG, "onActivityResult: " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECTED_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECTED_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupInterface();
                } else {
                    Log.w(LOG_TAG, "Bluetooth not enabled");
                    finish();
                }
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        final String address = data.getExtras().getString(DevicesListActivity.EXTRA_DEVICE_ADDRESS);
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        bluetoothService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent serverIntent;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                serverIntent = new Intent(this, DevicesListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECTED_DEVICE_SECURE);
                return true;
            case R.id.discoverable:
                ensureDiscoverable();
                return true;
            case R.id.disconnect:
                if (bluetoothService != null) {
                    bluetoothService.stop();
                }
                return true;
        }
        return false;
    }

    private void ensureDiscoverable() {
        Log.w(LOG_TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            final Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            gameEngine = new GameEngine();
                            gameEngine.boardWidth = surfaceView.getWidth() / 12;
                            gameEngine.boardHeight = surfaceView.getHeight() / 10;
                            if (surfaceHolder != null) prepareCanvas();
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    if (surfaceHolder != null) prepareCanvas();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage.equals("1")) {
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY][gameEngine.positionPlayerTwoX] = 0;
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY][gameEngine.positionPlayerTwoX - 1] = 1;
                        gameEngine.positionPlayerTwoX--;
                    } else if (readMessage.equals("2")) {
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY][gameEngine.positionPlayerTwoX] = 0;
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY - 1][gameEngine.positionPlayerTwoX] = 1;
                        gameEngine.positionPlayerTwoY--;
                    } else if (readMessage.equals("3")) {
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY][gameEngine.positionPlayerTwoX] = 0;
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY][gameEngine.positionPlayerTwoX + 1] = 1;
                        gameEngine.positionPlayerTwoX++;
                    } else if (readMessage.equals("4")) {
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY][gameEngine.positionPlayerTwoX] = 0;
                        gameEngine.playerTwo[gameEngine.positionPlayerTwoY + 1][gameEngine.positionPlayerTwoX] = 1;
                        gameEngine.positionPlayerTwoY++;
                    }
                    if (surfaceHolder != null) prepareCanvas();
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(BluetoothConnection.this, "Połączono z "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(BluetoothConnection.this, msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

    @Override
    public void onClick(View view) {
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Log.w(LOG_TAG, "No device connected");
            return;
        }
        String move = null;
        switch (view.getId()) {
            case R.id.btnLeft:
                if (gameEngine.positionPlayerOneX > 0) {
                    gameEngine.playerOne[gameEngine.positionPlayerOneY][gameEngine.positionPlayerOneX] = 0;
                    gameEngine.playerOne[gameEngine.positionPlayerOneY][gameEngine.positionPlayerOneX - 1] = 1;
                    gameEngine.positionPlayerOneX--;
                    move = "1";
                }
                break;
            case R.id.btnTop:
                if (gameEngine.positionPlayerOneY > 0) {
                    gameEngine.playerOne[gameEngine.positionPlayerOneY][gameEngine.positionPlayerOneX] = 0;
                    gameEngine.playerOne[gameEngine.positionPlayerOneY - 1][gameEngine.positionPlayerOneX] = 1;
                    gameEngine.positionPlayerOneY--;
                    move = "2";
                }
                break;
            case R.id.btnRight:
                if (gameEngine.positionPlayerOneX < 11) {
                    gameEngine.playerOne[gameEngine.positionPlayerOneY][gameEngine.positionPlayerOneX] = 0;
                    gameEngine.playerOne[gameEngine.positionPlayerOneY][gameEngine.positionPlayerOneX + 1] = 1;
                    gameEngine.positionPlayerOneX++;
                    move = "3";
                }
                break;
            case R.id.btnBottom:
                if (gameEngine.positionPlayerOneY < 9) {
                    gameEngine.playerOne[gameEngine.positionPlayerOneY][gameEngine.positionPlayerOneX] = 0;
                    gameEngine.playerOne[gameEngine.positionPlayerOneY + 1][gameEngine.positionPlayerOneX] = 1;
                    gameEngine.positionPlayerOneY++;
                    move = "4";
                }
                break;
        }
        byte[] bytes;
        if (move != null) {
            bytes = move.getBytes();
            bluetoothService.write(bytes);
        }
    }

    private final SurfaceHolder.Callback surfaceViewCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.i(LOG_TAG, "SurfaceView created");
            paint = new Paint();
            prepareCanvas();

        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    private void prepareCanvas() {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas(null);
            onDraw(canvas);
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 12; j++) {
                int x = j * gameEngine.boardWidth;
                int y = i * gameEngine.boardHeight;
                paint.setColor(Color.GREEN);
                canvas.drawRect(x, y, x + gameEngine.boardWidth, y + gameEngine.boardHeight, paint);
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 12; j++) {
                int x = j * gameEngine.boardWidth;
                int y = i * gameEngine.boardHeight;
                switch (gameEngine.playerOne[i][j]) {
                    case 1:
                        paint.setColor(Color.RED);
                        canvas.drawRect(x, y, x + gameEngine.boardWidth, y + gameEngine.boardHeight, paint);
                        break;
                }

            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 12; j++) {
                int x = j * gameEngine.boardWidth;
                int y = i * gameEngine.boardHeight;
                switch (gameEngine.playerTwo[i][j]) {
                    case 1:
                        paint.setColor(Color.BLUE);
                        canvas.drawRect(x, y, x + gameEngine.boardWidth, y + gameEngine.boardHeight, paint);
                        break;
                }

            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(LOG_TAG, "Bluetooth off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(LOG_TAG, "Turning Bluetooth off...");
                        if (bluetoothService != null) {
                            bluetoothService.stop();
                        }
                        finish();
                        System.exit(0);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(LOG_TAG, "Bluetooth on");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i(LOG_TAG, "Turning Bluetooth on...");
                        break;
                }
            }
        }
    };
}
