package com.smos.smartlistview;

import android.util.ArrayMap;
import android.util.SparseBooleanArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by nrs on 3/8/16.
 */
public class SectionItemArray<DataStruct> implements DataSetOperator {
    public static final int NOT_FOUND = -1;
    private ArrayMap<String, SparseBooleanArray> mSelectMap;
    private ArrayMap<String, ArrayList<DataStruct>> mSectionMap;
    private HashSet<Integer> mSelectedPositions = new HashSet<Integer>();

    public SectionItemArray(ArrayMap<String, ArrayList<DataStruct>> sectionMap) {
        mSectionMap = sectionMap;
        mSelectMap = new ArrayMap<>();
    }

    public boolean contains(int position) {
        return mSelectedPositions.contains(position);
    }

    public DataStruct get(int index) {
        int size = mSectionMap.size();
        int count = 0;
        for (int i = 0; i < size; i++) {
            count++;
            ArrayList<DataStruct> structList = mSectionMap.valueAt(i);
            if (index < (structList.size() + count)) {
                int indexInSection = index - count;
                return structList.get(indexInSection);
            }
            count += structList.size();
        }
        return null;
    }

    public String getSection(int index) {
        int size = mSectionMap.size();
        int count = 0;
        for (int i = 0; i < size; i++) {
            count++;
            ArrayList<DataStruct> structList = mSectionMap.valueAt(i);
            if (index < (structList.size() + count)) {
                return mSectionMap.keyAt(i);
            }
            count += structList.size();
        }
        return null;
    }

    public boolean isSection(int index) {
        int size = mSectionMap.size();
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (index < count) {
                return false;
            } else if (index == count) {
                return true;
            }
            count++;
            ArrayList<DataStruct> structList = mSectionMap.valueAt(i);
            count += structList.size();
        }
        return false;
    }

    public int size() {
        int size = mSectionMap.size();
        int count = 0;
        for (int i = 0; i < size; i++) {
            count++;
            ArrayList<DataStruct> structList = mSectionMap.valueAt(i);
            count += structList.size();
        }
        return count;
    }

    public void delete(int index) {
        if (!isSection(index)) {
            int size = mSectionMap.size();
            int count = 0;
            for (int i = 0; i < size; i++) {
                count++;
                String key = mSectionMap.keyAt(i);
                ArrayList<DataStruct> structList = mSectionMap.valueAt(i);
                SparseBooleanArray booleanArray = mSelectMap.get(key);
                if (index < (structList.size() + count)) {
                    int indexInSection = index - count;
                    structList.remove(indexInSection);
                    booleanArray.delete(indexInSection);
                    if (structList.size() == 0) {
                        mSectionMap.remove(key);
                        mSelectMap.remove(key);
                    }
                    break;
                }
                count += structList.size();
            }
        }
    }

    private void doSelect(int index, boolean select) {
        if (select) {
            mSelectedPositions.add(index);
        } else {
            mSelectedPositions.remove(index);
        }
        int size = mSectionMap.size();
        int count = 0;
        for (int i = 0; i < size; i++) {
            count++;
            SparseBooleanArray selectList;
            ArrayList<DataStruct> structList = mSectionMap.valueAt(i);
            if (index < (structList.size() + count)) {
                int indexInSection = index - count;
                String key = mSectionMap.keyAt(i);
                if(mSelectMap.containsKey(key)){
                    selectList = mSelectMap.get(key);
                }else{
                    selectList = new SparseBooleanArray();
                    mSelectMap.put(key, selectList);
                }
                if (select) {
                    selectList.put(indexInSection, select);
                } else {
                    selectList.delete(indexInSection);
                }
                if (selectList.size() == structList.size()) {
                    mSelectedPositions.add(count - 1);
                } else {
                    mSelectedPositions.remove(count - 1);
                }
                break;
            }
            count += structList.size();
        }
    }

    public void select(int position) {
        doSelect(position, true);
    }

    public void deSelect(int position) {
        doSelect(position, false);
    }

    public List<Integer> getSelectedItem() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (Integer position : mSelectedPositions) {
            ids.add(position);
        }
        mSelectedPositions.clear();
        return ids;
    }
}
