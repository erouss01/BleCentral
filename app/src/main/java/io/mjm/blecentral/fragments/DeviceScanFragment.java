package io.mjm.blecentral.fragments;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.mjm.blecentral.R;
import io.mjm.blecentral.adapters.DevicesArrayAdapter;
import io.mjm.blecentral.models.DeviceViewModel;

import static android.widget.Toast.LENGTH_LONG;

public class DeviceScanFragment extends Fragment {
    private DeviceViewModel mViewModel;
    private static final String TAG = "DeviceScanFragment";
    View view;
    Button btn_SCAN;
    BluetoothDevice deviceCourant;
    DevicesArrayAdapter adapterDevice = null;
    ListView listviewDevices = null;


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long SCAN_PERIOD = 10000;
    private  boolean mScanning = false ;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;


    List<BluetoothDevice> listeEntite = null;


    public static DeviceScanFragment newInstance() {
        return new DeviceScanFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new DeviceViewModel();
        listeEntite = new ArrayList<BluetoothDevice>();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.device_scan_fragment, container, false);

        // Bouton
        btn_SCAN = view.findViewById(R.id.btn_SCAN);

        btn_SCAN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanning();
            }
        });

// listview
        listviewDevices = view.findViewById(R.id.listviewDevices);
        adapterDevice = new DevicesArrayAdapter(getActivity());
        listviewDevices.setAdapter(adapterDevice);
        listviewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Object o = listviewDevices.getItemAtPosition(position);
                deviceCourant = (BluetoothDevice) o;
                //mViewModel.setSelected(deviceCourant);
                Bundle bundle = new Bundle();
                bundle.putParcelable("device", deviceCourant);
                //  Navigation.findNavController(view).navigate(R.id.GattClientFragment, bundle);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    stopScanning();
                }
                NavHostFragment.findNavController(DeviceScanFragment.this)
                        .navigate(R.id.action_DeviceScanFragment_to_GattClientFragment,bundle);
            }
        });

        getActivity().setTitle(R.string.device_scan_fragment_label);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        mViewModel.getBluetoothDevices().observe(getActivity(), new Observer<List<BluetoothDevice>>() {
            @Override
            public void onChanged(List<BluetoothDevice> devices) {
                adapterDevice.setmLeDevices(devices);
                adapterDevice.notifyDataSetChanged();
            }
        });

    }

    public void onStart() {
        super.onStart();
        startScanning();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stopScanning();
        }
        mScanning =false ;

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");
        if (mBluetoothLeScanner != null) {
            // Stop the scan, wipe the callback.
            mBluetoothLeScanner.stopScan(mScanCallback);
            //mScanCallback = null;
        }
        // Even if no new results, update 'last seen' times.
        adapterDevice.notifyDataSetChanged();
        mScanning = false ;
    }

    public void startScanning() {
        if (mScanCallback == null ||!mScanning ) {
            Log.d(TAG, "Starting Scanning");



            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    processScanLeDevice();
                }
            } else {
                // Permission to access the location is missing. Show rationale and request permission
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

            }



        } else {
            Toast.makeText(getActivity(), R.string.already_scanning, Toast.LENGTH_SHORT);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(getActivity(), "Required permission '" + permissions[index]
                                + "' not granted, exiting", LENGTH_LONG).show();
                        getActivity().finish();
                        return;
                    }
                }
                processScanLeDevice();
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void processScanLeDevice() {

        // Will stop the scanning after a set time.
        mHandler.postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                stopScanning();
            }
        }, SCAN_PERIOD);
        mScanCallback = new SampleScanCallback();



        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();

        mBluetoothLeScanner.startScan(null,settings,mScanCallback) ;
        mScanning = true ;

        String toastText = getString(R.string.scan_start_toast) + " "
                + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                + getString(R.string.seconds);
        Toast.makeText(getActivity(), toastText, LENGTH_LONG).show();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class SampleScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

        }
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result != null && result.getDevice() != null) {
                mViewModel.addDevice(result.getDevice());
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getActivity(), "Erreur Scan : " + errorCode, LENGTH_LONG)
                    .show();

        }

}}