package io.mjm.blecentral.adapters;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import io.mjm.blecentral.R;

public class DevicesArrayAdapter extends BaseAdapter {
    private Context context;
    private List<BluetoothDevice> mLeDevices = new ArrayList<BluetoothDevice>();;
    private LayoutInflater mInflator;

    public DevicesArrayAdapter(Context context) {
        super();
        this.context = context;


        mInflator = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getItemCount() {
        if(getmLeDevices() != null){
            return getmLeDevices().size();
        }else{
            return 0;
        }
    }

    public BluetoothDevice getDevice(int position) {
        return getmLeDevices().get(position);
    }


    @Override
    public int getCount() {
        return getmLeDevices().size();
    }

    @Override
    public Object getItem(int i) {
        return getmLeDevices().get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;

        if (view == null) {
            view = mInflator.inflate(R.layout.item_template_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress =  view.findViewById(R.id.textVieDeviceAddress);
            viewHolder.deviceName =  view.findViewById(R.id.textViewDeviceName);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = getmLeDevices().get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);
        viewHolder.deviceAddress.setText(device.getAddress());

        return view;
    }


    public List<BluetoothDevice> getmLeDevices() {
        return mLeDevices;
    }

    public void setmLeDevices(List<BluetoothDevice> mLeDevices) {
        this.mLeDevices = mLeDevices;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }



}
