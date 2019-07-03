package com.ringly.ringly.ui.screens.activity;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Function;
import com.jakewharton.rxbinding.view.RxView;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.config.model.Height;

import java.util.Calendar;
import java.util.List;

import retrofit2.http.HEAD;
import rx.Observable;
import rx.functions.Action0;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public class StepsDataAdapter extends RecyclerView.Adapter<StepsDataAdapter.ViewHolder> {
    public static final String TAG = StepsDataAdapter.class.getCanonicalName();

    public static final double MI_TO_KM = 1.60934;
    public static final float MAX_STEPS = 10000;

    private int mDateResolution;
    private List<StepsData> mData;
    private int mSelected;
    private Action0 mOnSetPrefs;

    private Height.HeightUnit mDistanceUnit;

    private PublishSubject<StepsData> mClickSubject;
    private CompositeSubscription mSubscriptions;

    public StepsDataAdapter(List<StepsData> data, int dateResolution, Action0 onSetPrefs) {
        mData = data;
        mDateResolution = dateResolution;
        mOnSetPrefs = onSetPrefs;
        mSelected = -1;
        mClickSubject = PublishSubject.create();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mSubscriptions = new CompositeSubscription();

        mDistanceUnit = Preferences.getHeight(recyclerView.getContext()).unit;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.view_steps_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final StepsData sd = mData.get(position);

        String date;
        switch (mDateResolution) {
            case Calendar.DAY_OF_MONTH:
                date = (String) DateFormat.format("EEE, MMM d", sd.startDate);
                break;
            case Calendar.HOUR_OF_DAY:
                String startDate = (String) DateFormat.format("EEE, MMM d hh:mm", sd.startDate);
                String endDate = (String) DateFormat.format("EEE, MMM d hh:mm", sd.endDate);
                date = startDate + " - " + endDate;
                break;
            default:
                date = DateFormat.getDateFormat(holder.mDate.getContext()).format(sd.startDate);
        }

        holder.mDate.setText(date);
        holder.mSteps.setText(String.valueOf(sd.steps));
        holder.mCals.setText(String.valueOf(sd.cals));
        holder.mStepsProgress.setProgress(sd.steps / MAX_STEPS);

        if(sd.cals == -1) {
            holder.mCals.setText(R.string.unset_prefs);
            holder.mCaloriesContainer.setOnClickListener(v ->
                showUnsetPrefsDialog(v.getContext(), R.string.unset_weight_dialog));
        } else {
            holder.mCals.setText(String.valueOf(sd.cals));
            holder.mCaloriesContainer.setOnClickListener(null);
            holder.mCaloriesContainer.setClickable(false);
        }

        boolean isMetric = mDistanceUnit == Height.HeightUnit.CM;
        holder.mDistanceUnit.setText(isMetric ? R.string.km : R.string.miles);

        if(sd.distance < 0) {
            holder.mDistance.setText(R.string.unset_prefs);
            holder.mDistanceContainer.setOnClickListener(v ->
                showUnsetPrefsDialog(v.getContext(), R.string.unset_height_dialog));
        } else {
            holder.mDistanceContainer.setOnClickListener(null);
            holder.mDistanceContainer.setClickable(false);
            double distanceConversion = isMetric ? MI_TO_KM : 1.0;
            holder.mDistance.setText(String.format("%.1f", sd.distance * distanceConversion));
        }

        mSubscriptions.add(
            RxView.clicks(holder.mView)
                .map(__ -> sd)
                .doOnNext(__ -> this.setSelected(mSelected == position ? -1 : position))
                .subscribe(
                    mClickSubject
                )
        );

        int selectedColor = position == mSelected ? R.color.medium_gray : R.color.gray;
        holder.mView.setBackgroundColor(
            ContextCompat.getColor(holder.mSteps.getContext(), selectedColor));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mSubscriptions.unsubscribe();
    }

    public void setData(List<StepsData> data) {
        mData = data;
        // TODO: Use specific notify methods
        notifyDataSetChanged();
    }

    public void setDateResolution(int mDateResolution) {
        this.mDateResolution = mDateResolution;
    }

    public void setSelected(int pos) {
        int prev = mSelected;
        mSelected = pos;

        if (pos >= 0) {
            notifyItemChanged(pos);
        }

        if (prev >= 0) {
            notifyItemChanged(prev);
        }
    }

    public Observable<StepsData> clicks() {
        return mClickSubject.asObservable();
    }

    private void showUnsetPrefsDialog(Context context, @StringRes int message) {
        new AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(R.string.set, (__, ___) -> mOnSetPrefs.call())
            .setNegativeButton(R.string.cancel, (__, ___) -> {})
            .create()
            .show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View mView;
        TextView mDate;
        TextView mSteps;
        View mCaloriesContainer;
        TextView mCals;
        View mDistanceContainer;
        TextView mDistance;
        TextView mDistanceUnit;
        StepsProgress mStepsProgress;


        ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mDate = (TextView) itemView.findViewById(R.id.date);
            mSteps = (TextView) itemView.findViewById(R.id.steps);
            mCaloriesContainer= itemView.findViewById(R.id.layout_calories);
            mCals = (TextView) itemView.findViewById(R.id.calories);
            mDistanceContainer= itemView.findViewById(R.id.layout_distance);
            mDistance = (TextView) itemView.findViewById(R.id.distance);
            mDistanceUnit = (TextView) itemView.findViewById(R.id.distance_unit);
            mStepsProgress = (StepsProgress) itemView.findViewById(R.id.progress);
        }
    }
}
