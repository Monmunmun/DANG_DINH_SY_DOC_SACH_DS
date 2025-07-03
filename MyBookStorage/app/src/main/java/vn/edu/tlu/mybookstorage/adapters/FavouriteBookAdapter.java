package vn.edu.tlu.mybookstorage.adapters;

import android.content.Intent;
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

public class FavouriteBookAdapter extends RecyclerView.Adapter<FavouriteBookAdapter.FavouriteBookViewHolder> {

    private List<Book> mBookList;
    private OnFavouriteBookActionListener mListener;

    public interface OnFavouriteBookActionListener {
        void onRemoveFromFavoritesClick(Book book);
        void onBookItemClick(Book book);
    }

    public FavouriteBookAdapter(List<Book> bookList, OnFavouriteBookActionListener listener) {
        this.mBookList = bookList;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public FavouriteBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favourite_book, parent, false);
        return new FavouriteBookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouriteBookViewHolder holder, int position) {
        Book book = mBookList.get(position);
        holder.bookTitleInFavouriteTextView.setText(book.getTitle());

        holder.removeBookFromFavouriteIcon.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRemoveFromFavoritesClick(book);
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

    public static class FavouriteBookViewHolder extends RecyclerView.ViewHolder {
        public TextView bookTitleInFavouriteTextView;
        public ImageView removeBookFromFavouriteIcon;

        public FavouriteBookViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitleInFavouriteTextView = itemView.findViewById(R.id.bookTitleInFavouriteTextView);
            removeBookFromFavouriteIcon = itemView.findViewById(R.id.removeBookFromFavouriteIcon);
        }
    }
}
