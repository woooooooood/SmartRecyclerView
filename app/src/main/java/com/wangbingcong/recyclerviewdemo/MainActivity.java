package com.wangbingcong.recyclerviewdemo;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.smos.smartlistview.SectionItemArray;
import com.smos.smartlistview.SelectItemListener;
import com.smos.smartlistview.SelectModeEnabler;
import com.smos.smartlistview.SlideInRightAnimator;
import com.smos.smartlistview.SmartListViewManager;
import com.smos.smartlistview.StickyHeaderDecoration;
import com.smos.smartlistview.SwipeItemListener;

import java.util.ArrayList;


public class MainActivity extends Activity {

    public RecyclerView mRecyclerView;
    private RecyclerView mGridView;
    private EditText mEtPos;
    private Button mBtnOpen;
    private Button mBtnEdit;
    private Button mBtnDelete;
    private RelativeLayout topBar;
    SmartListViewManager mManager;
    public SelectModeEnabler mSelectModeEnabler;
    private ListGridExchanger mListGridExchanger;

    private SelectModeEnabler.ModeChangingAnimatorCreator<LinearLayout> mAnimatorCreator =
            new SelectModeEnabler.ModeChangingAnimatorCreator<LinearLayout>() {
                @Override
                public Animator createModeChangingAnimator(final LinearLayout editableView, boolean
                        editable) {

                    final RelativeLayout.LayoutParams layoutParams =
                            (RelativeLayout.LayoutParams) editableView.getLayoutParams();

                    int editableViewMarginLeft = (int) getResources().getDimension(R.dimen
                            .editableview_margin_left);
                    int start = editable ? editableViewMarginLeft : 0;
                    int end = editable ? 0 : editableViewMarginLeft;

                    ValueAnimator animator = ValueAnimator.ofInt(start, end);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            layoutParams.leftMargin = (int) animation.getAnimatedValue();
                            editableView.requestLayout();
                        }
                    });

                    return animator;
                }
            };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.rv);

        initGridView();
        initRadioGroup();

        topBar = (RelativeLayout) findViewById(R.id.topBar);

        mEtPos = (EditText) findViewById(R.id.etPos);
        mBtnOpen = (Button) findViewById(R.id.btnOpen);
        mBtnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mBtnEdit = (Button) findViewById(R.id.btnEdit);
        mBtnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = mSelectModeEnabler.isSelectMode() ?
                        SmartListViewManager.SWIPE_MODE : SmartListViewManager.Select_MODE;
                mManager.setEditMode(mode);
                mSelectModeEnabler.changeEditMode(!mSelectModeEnabler.isSelectMode());
            }
        });

        SectionItemArray sectionItemArray = initDataList();
        int size = sectionItemArray.size();
        Log.d("ddd", "initView " + size);
        final SwipeMenuAdapter mAdapter = new SwipeMenuAdapter(this, mManager, mRecyclerView, sectionItemArray);


        //TODO SelectModeEnabler
        mSelectModeEnabler = new SelectModeEnabler(mRecyclerView, R.id.editableView,
                mAnimatorCreator);

        //TODO StickyHeaderDecoration
        StickyHeaderDecoration mStickyHeaderDecoration = new StickyHeaderDecoration(mAdapter);

        //TODO SelectItemListener
        SelectItemListener mSelectItemListener = new SelectItemListener(mRecyclerView, mAdapter);

        //TODO SwipeItemListener
        SwipeItemListener mSwipeItemListener = new SwipeItemListener(mRecyclerView,
                mAdapter);
        SlideInRightAnimator slideInRightAnimator = new SlideInRightAnimator();
        mManager = new SmartListViewManager.ManagerBuilder().setRecyclerView(mRecyclerView)
                .setAdapter(mAdapter)
                .setSwipeItemListener(mSwipeItemListener)
                .setInitialEditMode(SmartListViewManager.SWIPE_MODE)
//                .setStickyHeaderDecoration(mStickyHeaderDecoration)
                .setDataSetOperator(sectionItemArray)
                .setItemAnimator(slideInRightAnimator)
                .setSelectItemListener(mSelectItemListener).build();
        mAdapter.setManager(mManager);

        mBtnDelete = (Button) findViewById(R.id.btnDelete);
        mBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.deleteSelectedData();
            }
        });

        //TODO
        mListGridExchanger = new ListGridExchanger(mRecyclerView, mGridView, R.id.ivThumb, R.id.iv);
    }

    private SectionItemArray initDataList() {
        ArrayMap<String, ArrayList<String>> map = new ArrayMap<>();
        for (int i = 0; i < 26; i++) {
            ArrayList<String> strings = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                strings.add(String.valueOf((char) ('a' + i)));
            }
            map.put(String.valueOf((char) ('a' + i)), strings);
        }
        return new SectionItemArray<>(map);
    }

    private void initGridView() {
        mGridView = (RecyclerView) findViewById(R.id.rvGrid);
        mGridView.setLayoutManager(new GridLayoutManager(this, 3));
        mGridView.setAdapter(new GridViewAdapter(this));
    }

    private void initRadioGroup() {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rbLinear:
                        swapToList();
                        break;
                    case R.id.rbGrid:
                        swapToGrid();
                        break;
                }
            }
        });
    }

    private void swapToGrid() {
        mListGridExchanger.swapToGrid(null);
    }

    private void swapToList() {
        mListGridExchanger.swapToList(null);
    }
}
