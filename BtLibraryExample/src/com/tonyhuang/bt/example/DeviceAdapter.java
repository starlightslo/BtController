package com.tonyhuang.bt.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * @author Tony Huang (starlightslo@gmail.com)
 * @version Creation time: 2014/6/3 下午8:11:15
 */
public class DeviceAdapter extends BaseAdapter {
	private Context mContext;
	private String[] mName;
	private String[] mStatus;
	
	public DeviceAdapter(Context context, String[] name, String[] status) {
		mContext = context;
		update(name, status);
	}
	
	@Override
	public int getCount() {
		return mName.length;
	}

	@Override
	public Object getItem(int position) {
		return mName[position];
	}

	@Override
	public long getItemId(int id) {
		return id;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.layout_devices, parent, false);
		
		TextView name = (TextView) rowView.findViewById(R.id.name);
		TextView status = (TextView) rowView.findViewById(R.id.status);
		name.setText(mName[position]);
		status.setText(mStatus[position]);
		
		return rowView;
	}

	public void update(String[] name, String[] status) {
		this.mName = name;
		this.mStatus = status;
	}
	
}
