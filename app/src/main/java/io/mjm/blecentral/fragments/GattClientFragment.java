package io.mjm.blecentral.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.UUID;



import io.mjm.blecentral.R;
import io.mjm.blecentral.models.BLEHelper;
import io.mjm.blecentral.models.DeviceViewModel;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;

public class GattClientFragment extends Fragment {

    private static final String TAG = "GattClientFragment";
    private DeviceViewModel mViewModel;
    BluetoothDevice deviceCourant;

    View view;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mdeviceAddress;
    private TextView mdeviceName;
    private ToggleButton btn_Toggle_led;

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private int connectionState = STATE_DISCONNECTED;

    private BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBluetoothGatt;

    private static final byte LED_STATE_OFF = 0x00;
    private static final byte LED_STATE_ON = 0x01;
    BluetoothGattCharacteristic characteristicRX ;
    BluetoothGattCharacteristic characteristicTX ;

    public static String LBS_SERVICE_CONFIG = "00001523-1212-efde-1523-785feabcd123";
    public static String LBS_BTN_RX_CHARACTERISTIC_CONFIG = "00001524-1212-efde-1523-785feabcd123";
    public static String LBS_LED_TX_CHARACTERISTIC_CONFIG = "00001525-1212-efde-1523-785feabcd123";
    public static String LBE_CHARACTERISTIC_DESCRPT = "00002902-0000-1000-8000-00805f9b34fb" ;//0x2902;


    public final static UUID LBS_UUID_SERVICE =
            UUID.fromString(LBS_SERVICE_CONFIG);
    public final static UUID LBS_UUID_BUTTON_CHAR =
            UUID.fromString(LBS_BTN_RX_CHARACTERISTIC_CONFIG);
    public final static UUID LBS_UUID_LED_CHAR =
            UUID.fromString(LBS_LED_TX_CHARACTERISTIC_CONFIG);
    public final static UUID UUID_LEDBTN_CHARACTERISTIC_DESCRPT =
            UUID.fromString(LBE_CHARACTERISTIC_DESCRPT);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceCourant = getArguments().getParcelable("device");
        mBluetoothAdapter = BLEHelper.getInstance().getBluetoothAdapter();

        if(deviceCourant != null){
            mDeviceAddress = deviceCourant.getAddress();
            mDeviceName = deviceCourant.getName();
            connect(mDeviceAddress);
        }
    }


    public static GattClientFragment newInstance() {
        return new GattClientFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.gatt_client_fragment, container, false);

        mConnectionState = view.findViewById(R.id.connection_state);
        mDataField = view.findViewById(R.id.data_value);
        btn_Toggle_led = view.findViewById(R.id.btn_Toggle_led);
        mdeviceAddress = view.findViewById(R.id.device_address);
        mdeviceName = view.findViewById(R.id.device_name);
        mdeviceName.setText(mDeviceName);
        mdeviceAddress.setText(mDeviceAddress);

        btn_Toggle_led.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (characteristicRX != null){
                    if (isChecked) {
                        writeCharacteristic(characteristicTX,new byte[]{LED_STATE_ON});
                        printInMessage("Message 'On' successfully to Device");
                    } else {
                        writeCharacteristic(characteristicTX,new byte[]{LED_STATE_OFF});
                        printInMessage("Message 'Off' successfully to Device");
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    // CONNEXION

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (address.equals(mDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(getActivity(), false, mGattCallback, TRANSPORT_LE);
            Log.d(TAG, "Trying to create a new connection.");
            mDeviceAddress = address;
            connectionState = STATE_CONNECTING;
        }

        return true;
    }



    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                updateConnectionState(R.string.connected);
                connectionState = STATE_CONNECTED;

                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updateConnectionState(R.string.disconnected);
                connectionState = STATE_DISCONNECTED;
                clearUI();
                Log.i(TAG, "Disconnected from GATT server.");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService serviceUart = gatt.getService(LBS_UUID_SERVICE);
                if (serviceUart != null) {

                    characteristicRX = serviceUart.getCharacteristic(LBS_UUID_BUTTON_CHAR);
                    characteristicTX = serviceUart.getCharacteristic(LBS_UUID_LED_CHAR);

                    gatt.setCharacteristicNotification(characteristicRX, true);
                    BluetoothGattDescriptor descriptor = characteristicRX.getDescriptor(UUID_LEDBTN_CHARACTERISTIC_DESCRPT);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);


                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG,"Failed to read characteristic: " + characteristic.getUuid());
                return;
            }

            //Read value from device
            byte[] readValue = characteristic.getValue();

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] messageBytes = characteristic.getValue();

            try {
                int val = messageBytes[0] ;

                printOutMessage(val);

            } catch (Exception e) {
                Log.e(TAG, "Unable to convert message bytes to string");
            }

        }
    };

    private  void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        Log.i(TAG, "characteristic " + characteristic.toString());

        try {

            characteristic.setValue(data);
            boolean success = mBluetoothGatt.writeCharacteristic(characteristic);

        } catch (Exception e) {
            Log.e(TAG, "Failed to convert message string to byte array");
        }

    }

    private void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }


    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    private void clearUI() {

        mDataField.setText(R.string.no_data);
    }

    private void updateConnectionState(final int resourceId) {
        getActivity(). runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void printInMessage(final String msm) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDataField.setText(msm);
            }
        });
    }

    private void printOutMessage(final int val) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mDataField.setText(val == 1 ? "Button pressed":"Button released");
                mDataField.setTextColor(val == 1 ? Color.WHITE:Color.BLACK);
                mDataField.setBackgroundColor(val == 1 ? Color.BLUE:Color.parseColor("#C0C0C0"));
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }
}
