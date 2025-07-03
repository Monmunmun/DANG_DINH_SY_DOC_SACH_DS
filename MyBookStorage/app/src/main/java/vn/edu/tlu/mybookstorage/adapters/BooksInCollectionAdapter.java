package vn.edu.tlu.mybookstorage.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.models.Book;

public class BooksInCollectionAdapter extends RecyclerView.Adapter<BooksInCollectionAdapter.BookInCollectionViewHolder> {

    private List<Book> mBookList;
    private OnBookRemoveListener mListener;

    public interface OnBookRemoveListener {
        void onRemoveBookClick(Book book);
        void onBookItemClick(Book book);
    }

    public BooksInCollectionAdapter(List<Book> bookList, OnBookRemoveListener listener) {
        this.mBookList = bookList;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public BookInCollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection_book, parent, false);
        return new BookInCollectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookInCollectionViewHolder holder, int position) {
        Book book = mBookList.get(position);
        holder.bookTitleInCollectionTextView.setText(book.getTitle());

        holder.removeBookFromCollectionIcon.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRemoveBookClick(book);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onBookItemClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mBookList.size();
    }

    public void updateBookList(List<Book> newBookList) {
        this.mBookList = newBookList;
        notifyDataSetChanged();
    }

    public static class BookInCollectionViewHolder extends RecyclerView.ViewHolder {
        public TextView bookTitleInCollectionTextView;
        public ImageView removeBookFromCollectionIcon;

        public BookInCollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitleInCollectionTextView = itemView.findViewById(R.id.bookTitleInCollectionTextView);
            removeBookFromCollectionIcon = itemView.findViewById(R.id.removeBookFromCollectionIcon);
        }
    }
}