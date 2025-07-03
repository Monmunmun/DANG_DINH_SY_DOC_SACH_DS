package vn.edu.tlu.mybookstorage.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.models.Collection;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private static final String TAG = "CollectionAdapter";
    private List<Collection> mCollectionList;
    private OnCollectionInteractionListener mListener;

    public interface OnCollectionInteractionListener {

        void onCollectionClick(Collection collection);
        boolean onCollectionLongClick(Collection collection);
    }

    public CollectionAdapter(List<Collection> collectionList, OnCollectionInteractionListener listener) {
        this.mCollectionList = collectionList;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent, false);
        return new CollectionViewHolder(view);
    }

    /**
     * Được gọi bởi RecyclerView để hiển thị dữ liệu tại vị trí đã chỉ định.
     */
    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        Collection collection = mCollectionList.get(position);

        holder.collectionNameTextView.setText(collection.getName());

        int bookCount = (collection.getBookIds() != null) ? collection.getBookIds().size() : 0;
        holder.collectionBookCountTextView.setText(" (" + bookCount + " sách)");

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onCollectionClick(collection);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                return mListener.onCollectionLongClick(collection);
            }
            return false;
        });
    }

    /**
     * Trả về tổng số mục trong tập dữ liệu được giữ bởi adapter.
     */
    @Override
    public int getItemCount() {
        return mCollectionList.size();
    }

    /**
     * Cập nhật tập dữ liệu của adapter bằng các bộ sưu tập mới và thông báo cho RecyclerView để làm mới.
     */
    public void updateCollections(List<Collection> newCollections) {
        this.mCollectionList = newCollections;
        notifyDataSetChanged();
    }

    public static class CollectionViewHolder extends RecyclerView.ViewHolder {
        public TextView collectionNameTextView;
        public TextView collectionBookCountTextView;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);

            collectionNameTextView = itemView.findViewById(R.id.collectionNameTextView);
            collectionBookCountTextView = itemView.findViewById(R.id.collectionBookCountTextView);
        }
    }
}