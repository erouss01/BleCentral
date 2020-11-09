package io.mjm.blecentral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import io.mjm.blecentral.helpers.Constantes;
import io.mjm.blecentral.models.BLEHelper;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
           finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

         if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

         if (!mBluetoothAdapter.isEnabled()) {
            // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constantes.REQUEST_ENABLE_BT);
        }
        BLEHelper.getInstance().setBluetoothAdapter(mBluetoothAdapter);
        /*FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constantes.REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                // this device?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        Toast.makeText(this, R.string.bt_ads_not_supported,
                                Toast.LENGTH_SHORT).show();

                    }
                }
                // Everything is supported and enabled, load the fragments.
                BLEHelper.getInstance().setBluetoothAdapter(mBluetoothAdapter);

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}