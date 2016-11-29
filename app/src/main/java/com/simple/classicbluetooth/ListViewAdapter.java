package com.simple.classicbluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * ListView适配器
 */
public class ListViewAdapter extends LBaseAdapter<BluetoothDevice,LBaseAdapter.BaseViewHolder> implements AdapterView.OnItemClickListener{
    private Context context;

    public ListViewAdapter(Context context){
        super(context);
        this.context = context;
    }

    @Override
    protected BaseViewHolder createViewHolder(int position, ViewGroup parent) {
        return new BaseViewHolder(View.inflate(getContext(),R.layout.device_item,null));
    }

    @Override
    protected void bindViewHolder(BaseViewHolder holder, int position, BluetoothDevice data) {
        TextView txtName = holder.getView(R.id.tVName);
        TextView txtAddress = holder.getView(R.id.tVAddress);

        String deviceName = data.getName();
        String deviceAddress = data.getAddress();

        if (deviceName != null && deviceName.length() > 0){
            txtName.setText(deviceName);
        } else{
            txtName.setText("unknown_device");
        }
        txtAddress.setText(deviceAddress);
    }

    @Override
    public void setDataSource(List<BluetoothDevice> dataSource) {
        super.setDataSource(dataSource);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(context,"点击了： "+position, Toast.LENGTH_SHORT).show();
    }
}