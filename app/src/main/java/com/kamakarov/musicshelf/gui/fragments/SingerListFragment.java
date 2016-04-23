package com.kamakarov.musicshelf.gui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kamakarov.musicshelf.R;
import com.kamakarov.musicshelf.gui.adapters.SingerAdapter;
import com.kamakarov.musicshelf.model.Singer;
import com.kamakarov.musicshelf.utils.RxUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public final class SingerListFragment extends FragmentBase implements SwipeRefreshLayout.OnRefreshListener {
    public static SingerListFragment newInstance() {
        Bundle args = new Bundle();

        SingerListFragment fragment = new SingerListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Bind(R.id.swipe_refresh_layout_singer_lst)
    SwipeRefreshLayout swipeRefreshLayout;

    @Bind(R.id.recycler_view_singer_list)
    RecyclerView recyclerView;

    @Bind(R.id.report_empty)
    View emptyPlaceholder;

    @Bind(R.id.root_view_singer_list)
    View rootView;

    private final List<Singer> singerList = new ArrayList<>();
    private SingerAdapter mAdapter;
    private boolean isFirstTimeCreated;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        isFirstTimeCreated = true;
        fetchData();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_singer_list, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new SingerAdapter(getContext(), singerList);
        recyclerView.setAdapter(mAdapter);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        showEmptyPlaceholder(singerList.isEmpty());
    }

    @Override
    public void onPause() {
        super.onPause();
        isFirstTimeCreated = false;
    }

    @Override
    public void onRefresh() {
        fetchData();
    }

    Subscription databaseSubsctiption;
    Subscription apiSubscription;

    private void fetchData() {
        RxUtils.unsubscribeNullSafe(apiSubscription);
        if (databaseSubsctiption == null) {
            //first time: try to get from database, after that, show only when table was changed
            databaseSubsctiption = dbManager
                    .getSingers()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::showData);
        }


        apiSubscription = api.getSingers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onErrorInternetConnection)
                .observeOn(Schedulers.io())
                .subscribe(this::saveData, throwable -> {/*do nothing, because we handle in doOnError*/});
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxUtils.unsubscribeNullSafe(databaseSubsctiption);
        RxUtils.unsubscribeNullSafe(apiSubscription);
    }

    private void saveData(List<Singer> singers) {
        dbManager.addSingers(singers);
    }

    private void onErrorInternetConnection(Throwable throwable) {
        Log.d("eee", throwable.getMessage());

        if (singerList == null || singerList.isEmpty()) {
            showEmptyPlaceholder(true);
        } else {
            showEmptyPlaceholder(false);
        }

        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
            showSnackBarRetry();
        }

    }

    private void showSnackBarRetry() {
        Snackbar.make(rootView, "Clicked!", Snackbar.LENGTH_SHORT).show();
        //// TODO: 23.04.16  Add snack with retry
    }

    private void showEmptyPlaceholder(boolean needShow) {
        if (emptyPlaceholder != null) {
            if (needShow && !isFirstTimeCreated) {
                emptyPlaceholder.setVisibility(View.VISIBLE);
            } else {
                emptyPlaceholder.setVisibility(View.GONE);
            }
        }
    }

    private void showData(List<Singer> singers) {
        // FIXME: 27.03.16 it can be doing before onCreateView, just after onCreate.
        emptyPlaceholder.setVisibility(View.GONE);
        Log.d("eee", "data is fetched");
//        singerList.clear();
        singerList.addAll(singers);
        isFirstTimeCreated = false;
        showEmptyPlaceholder(singerList.isEmpty());
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        Log.d("eee", "count in list: " + singerList.size());
    }
}
