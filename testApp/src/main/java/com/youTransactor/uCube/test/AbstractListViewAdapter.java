/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p/>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.youTransactor.uCube.test.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author gbillard on 3/23/16.
 */
abstract public class AbstractListViewAdapter<E> extends BaseAdapter {

	protected List<E> itemList = new ArrayList<>();

	protected AbstractListViewAdapter() {}

	protected AbstractListViewAdapter(Collection<E> items) {
		addAllItems(items);
	}

	protected AbstractListViewAdapter(E... items) {
		for (E item : items) {
			itemList.add(item);
		}
	}

	public void addItem(E item) {
		itemList.add(item);
		notifyDataSetChanged();
	}

	public void addAllItems(Collection<E> items) {
		itemList.addAll(items);
		notifyDataSetChanged();
	}

	public E removeItemAt(int position) {
		if (position >= 0 && position < itemList.size()) {
			E item = itemList.remove(position);

			notifyDataSetInvalidated();

			return item;
		}

		return null;
	}

	public boolean removeItem(E item) {
		if (itemList.remove(item)) {
			notifyDataSetInvalidated();
			return true;
		}

		return false;
	}

	public void clearItems() {
		itemList.clear();
		notifyDataSetInvalidated();
	}

	public int getItemPosition(E item) {
		return itemList.indexOf(item);
	}

	@Override
	public int getCount() {
		return itemList.size();
	}

	@Override
	public Object getItem(int position) {
		return itemList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater li = LayoutInflater.from(parent.getContext());
			convertView = li.inflate(R.layout.list_item, null);
		}

		renderITem(convertView, itemList.get(position));

		return convertView;
	}

	abstract protected void renderITem(View view, E item);

}
