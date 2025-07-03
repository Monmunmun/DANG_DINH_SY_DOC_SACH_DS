package vn.edu.tlu.mybookstorage.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.models.ReadingHistoryEntry;

public class HistoryBookAdapter extends RecyclerView.Adapter<HistoryBookAdapter.HistoryViewHolder> {

    private List<ReadingHistoryEntry> mHistoryList;
    private OnHistoryItemClickListener mListener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(ReadingHistoryEntry entry);
    }

    public HistoryBookAdapter(List<ReadingHistoryEntry> historyList, OnHistoryItemClickListener listener) {
        this.mHistoryList = historyList;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_book, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ReadingHistoryEntry entry = mHistoryList.get(position);

        holder.tvBookTitle.setText(entry.getBookTitle());
        holder.tvLastReadPage.setText("Trang: " + entry.getLastReadPage());

        holder.tvLastReadTime.setText(formatTimestamp(entry.getTimestamp()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onHistoryItemClick(entry);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mHistoryList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookTitle;
        TextView tvLastReadPage;
        TextView tvLastReadTime;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookTitle = itemView.findViewById(R.id.tv_book_title_history);
            tvLastReadPage = itemView.findViewById(R.id.tv_last_read_page_history);
            tvLastReadTime = itemView.findViewById(R.id.tv_last_read_time_history);
        }
    }

    /**
     * Định dạng timestamp.
     */
    private String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Vừa xong";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toMinutes(diff) + " phút trước";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toHours(diff) + " giờ trước";
        } else if (diff < TimeUnit.DAYS.toMillis(2)) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (!dayFormat.format(new Date(now)).equals(dayFormat.format(new Date(timestamp)))) {
                return "Hôm qua lúc " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            } else {
                return "Hôm nay lúc " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            }
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            return TimeUnit.MILLISECONDS.toDays(diff) + " ngày trước";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}