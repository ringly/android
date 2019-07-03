package com.ringly.ringly.ui.screens.activity;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.common.base.Optional;

import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;

import static rx.android.MainThreadSubscription.verifyMainThread;

public class ChartValueSelectedOnSubscribe<T extends Entry> implements Observable.OnSubscribe<Optional<T>> {
    final Chart<? extends ChartData<? extends IDataSet<T>>> view;

    public ChartValueSelectedOnSubscribe(Chart<? extends ChartData<? extends IDataSet<T>>> view) {
        this.view = view;
    }

    @Override
    public void call(Subscriber<? super Optional<T>> subscriber) {
        verifyMainThread();

        OnChartValueSelectedListener listener = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(Optional.of((T) e));
                }
            }

            @Override
            public void onNothingSelected() {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(Optional.<T>absent());
                }
            }
        };

        subscriber.add(new MainThreadSubscription() {
            @Override
            protected void onUnsubscribe() {
                view.setOnChartValueSelectedListener(null);
            }
        });

        view.setOnChartValueSelectedListener(listener);
    }

    public static <T extends Entry> Observable<Optional<T>> valueSelected(Chart<? extends ChartData<? extends IDataSet<T>>> chart) {
        return Observable.create(new ChartValueSelectedOnSubscribe<>(chart));
    }
}


