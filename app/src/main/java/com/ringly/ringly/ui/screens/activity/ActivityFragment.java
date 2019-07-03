package com.ringly.ringly.ui.screens.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.bluetooth.RinglyService;
import com.ringly.ringly.config.Screen;
import com.ringly.ringly.db.Db;
import com.ringly.ringly.ui.MainActivity;
import com.ringly.ringly.ui.Utilities;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class ActivityFragment extends Fragment {

    private CompositeSubscription mSubscriptions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSubscriptions = new CompositeSubscription();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        Observable<List<StepsData>> sdObservable =
            Db.getInstance(getContext())
                .eventsByField(Calendar.DATE)
                .debounce(500, TimeUnit.MILLISECONDS);

        RecyclerView rv = (RecyclerView) view.findViewById(R.id.steps_list);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(llm);

        Action0 onSetPrefs = () ->
            ((MainActivity) getActivity()).changeScreen(Screen.PREFERENCES, R.string.activity, false);
        StepsDataAdapter adapter =
            new StepsDataAdapter(Lists.newArrayList(), Calendar.DAY_OF_MONTH, onSetPrefs);

        Observable<List<StepsData>> rvData = sdObservable.map(Lists::reverse);

        mSubscriptions.add(
            rvData
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    adapter::setData
                )
        );

        rv.setAdapter(adapter);

        DividerItemDecoration divider = new DividerItemDecoration(rv.getContext(),
            llm.getOrientation());
        rv.addItemDecoration(divider);

        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);


        BarChart chart = (BarChart) view.findViewById(R.id.chart);
        chart.getDescription().setEnabled(false);
        chart.setDrawValueAboveBar(false);
        chart.getLegend().setEnabled(false);
        chart.setNoDataText("Loading...");
        chart.setNoDataTextColor(ContextCompat.getColor(getContext(), R.color.light_gray));

        XAxis xAxis = chart.getXAxis();
        xAxis.setLabelCount(7);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setYOffset(32f);
        xAxis.setGranularity(1f);
        xAxis.setDrawLabels(true);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return DateUtils.formatDateTime(getContext(),
                    (long) (value * TimeUnit.DAYS.toMillis(1)),
                    DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_NO_YEAR
                );
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });

        final YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(false);
        yAxis.setDrawGridLines(false);
        yAxis.setDrawLabels(false);

        YAxis right = chart.getAxisRight();
        right.setDrawAxisLine(false);
        right.setDrawGridLines(false);
        right.setDrawLabels(false);

        mSubscriptions.add(
            sdObservable
                .map(sds -> {
                    List<BarEntry> entries = Lists.newArrayList();
                    for (StepsData sd : sds) {
                        entries.add(new BarEntry(xVal(sd), sd.steps));
                    }
                    return entries;
                })
                .filter(es -> es.size() > 0)
                .map(es -> {
                    BarDataSet ds = new BarDataSet(es, "Steps");
                    ds.setDrawValues(false);
                    return ds;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bds -> {
                    yAxis.resetAxisMaximum();

                    if (chart.getData() == null) {
                        bds.setColor(ContextCompat.getColor(getContext(), R.color.graph_bar_normal));
                        bds.setHighLightColor(ContextCompat.getColor(getContext(), R.color.graph_bar_highlight));
                        BarData bd = new BarData(bds);
                        bd.setBarWidth(1.0f);
                        chart.setData(bd);
                        List<BarEntry> vals = bds.getValues();
                        chart.moveViewToX(vals.get(vals.size() - 1).getX());

                    } else {
                        float scrollPosition = chart.getHighestVisibleX();
                        List<BarEntry> oldVals = null;
                        if(chart.getBarData().getDataSetCount()>0) {
                            oldVals=((BarDataSet)chart.getBarData().getDataSetByIndex(0)).getValues();
                            chart.getBarData().removeDataSet(0);
                        }
                        chart.getBarData().addDataSet(bds);
                        chart.notifyDataSetChanged();
                        if (oldVals!= null && oldVals.size() > 0 && oldVals.get(oldVals.size()-1).getX() <= scrollPosition) {
                            List<BarEntry> vals = bds.getValues();
                            chart.moveViewToX(vals.get(vals.size() - 1).getX());
                        }
                    }

                    chart.setVisibleXRangeMaximum(7);
                    chart.setVisibleXRangeMinimum(7);
                    chart.setScaleEnabled(false);

                    setMaxAxis(chart, yAxis);

                    chart.invalidate();
                })
        );

        mSubscriptions.add(
            ChartValueSelectedOnSubscribe.valueSelected(chart)
                .withLatestFrom(rvData, (be, sds) ->
                    com.ringly.ringly.Utilities.bind(be, bep -> {
                            for (int i = 0; i < sds.size(); i++) {
                                if (bep.getX() == xVal(sds.get(i))) {
                                    return Optional.of(i);
                                }
                            }
                            return Optional.absent();
                        }
                    )
                )
                .map(pos -> pos.or(-1))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    pos -> {
                        if (pos >= 0) {
                            rv.smoothScrollToPosition(pos);
                        }
                        adapter.setSelected(pos);
                    }
                )
        );

        mSubscriptions.add(
            adapter.clicks()
                .subscribe(sd -> {
                    Highlight[] highlights = chart.getHighlighted();

                    if (highlights != null && highlights.length > 0 &&
                        highlights[0].getX() == xVal(sd)) {
                        chart.highlightValue(0, -1);
                    } else {
                        chart.highlightValue(xVal(sd), 0, true);
                        chart.centerViewToAnimated(xVal(sd), 0f, YAxis.AxisDependency.LEFT, 100);
                    }
                })
        );

        RinglyService.doUpdateActivity(getContext());

        mSubscriptions.add(
            Observable.interval(1, TimeUnit.MINUTES, Schedulers.computation())
                .subscribe(__ -> RinglyService.doUpdateActivity(getContext()))
        );

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.clear();
    }

    private float xVal(StepsData sd) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(sd.startDate);
        long millis = Utilities.truncateCal(cal, Calendar.DATE).getTimeInMillis();
        return millis / TimeUnit.DAYS.toMillis(1) + 1;
    }

    private void setMaxAxis(BarChart chart, YAxis yAxis) {
        float currentMax = chart.getYMax();
        int goal = Preferences.getGoal(getContext());
        yAxis.removeAllLimitLines();

        String num;
        double goalK = goal * 0.001;
        if(goalK == (int) goalK) {
            num = String.format("%dK", (int) goalK);
        } else {
            num = String.format("%sK", goalK);
        }

        LimitLine line = new LimitLine(goal, num);
        line.setLineColor(ContextCompat.getColor(getContext(), R.color.graph_goal_line));
        line.setTextSize(12);
        line.setTextColor(ContextCompat.getColor(getContext(), R.color.graph_goal_line));
        yAxis.addLimitLine(line);

        line = new LimitLine(goal, getString(R.string.goal));
        line.setLineColor(ContextCompat.getColor(getContext(), R.color.graph_goal_line));
        line.setTextSize(12);
        line.setTextColor(ContextCompat.getColor(getContext(), R.color.graph_goal_line));
        line.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        yAxis.addLimitLine(line);

        yAxis.setAxisMinimum(0f);
        float max = Math.max(goal + 500, currentMax);
        yAxis.setAxisMaximum(max);
        chart.setVisibleYRangeMaximum(max, YAxis.AxisDependency.LEFT);
        chart.setVisibleYRangeMinimum(max, YAxis.AxisDependency.LEFT);
    }
}

