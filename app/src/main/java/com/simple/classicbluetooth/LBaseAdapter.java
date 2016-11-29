package com.simple.classicbluetooth;

import android.content.Context;
import android.support.annotation.IdRes;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 杨卢阳 on 2016/11/9.
 * ListView适配器抽象父类
 */
public abstract class LBaseAdapter<E, V extends LBaseAdapter.BaseViewHolder> extends BaseAdapter {

    private Context context;
    private List<E> dataSource = new ArrayList<>(); //初始化一个防止getCount()空指针

    public LBaseAdapter(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    //替换原有数据源
    public void setDataSource(List<E> dataSource) {
        setDataSource(dataSource,true);
    }

    //如果isClear==true,则替换原有数据源，否则加到数据源后面
    public void setDataSource(List<E> dataSource, boolean isClear) {
        if (isClear) this.dataSource.clear();
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    //只加一个数据
    public void addData(E data) {
        this.dataSource.add(data);
        notifyDataSetChanged();
    }

    //通过下标移除一条数据
    public void removeData(int position) {
        this.dataSource.remove(position);
        notifyDataSetChanged();
    }

    //通过对象移除一条数据
    public void removeData(E data) {
        this.dataSource.remove(data);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return this.dataSource.size();
    }


    @Override
    public E getItem(int position) {
        return this.dataSource.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        V viewHolder = null;
        if (convertView == null) {
            viewHolder = createViewHolder(position, parent);
            if (viewHolder == null || viewHolder.getRootView() == null) {
                throw new NullPointerException("createViewHolder不能返回null或view为null的实例");
            }
            convertView = viewHolder.getRootView();
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (V) convertView.getTag();
        }
        //给当前复用的holder一个正确的position
        viewHolder.setPosition(position);
        bindViewHolder(viewHolder,position,getItem(position));
        return viewHolder.getRootView();
    }

    protected abstract V createViewHolder(int position, ViewGroup parent);

    protected abstract void bindViewHolder(V holder,int position, E data);

    public static class BaseViewHolder {
        private View rootView;
        private SparseArray<View> viewCache = new SparseArray<>();
        private int position = -1;

        public View getRootView() {
            return rootView;
        }

        void setPosition(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        public BaseViewHolder(View rootView) {
            this.rootView = rootView;
        }

        public <R> R getView(@IdRes int viewID) {
            View cachedView = viewCache.get(viewID);
            if(null == cachedView) {
                cachedView = rootView.findViewById(viewID);
                viewCache.put(viewID, cachedView);
            }
            return (R) cachedView;
        }
    }


}