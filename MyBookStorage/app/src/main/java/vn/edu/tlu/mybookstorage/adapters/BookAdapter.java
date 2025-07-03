package vn.edu.tlu.mybookstorage.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import vn.edu.tlu.mybookstorage.activities.BookReaderActivity;
import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.models.Book;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> mBookList;
    private OnBookActionListener mListener;

    public interface OnBookActionListener {
        void onBookClick(Book book);
        void onAddToCollectionClick(Book book);
        void onEditBookClick(Book book);
        void onDeleteBookClick(Book book);
        void onAddToFavoritesClick(Book book);
    }
    /**
     * Constructor cho BookAdapter.
     */
    public BookAdapter(List<Book> bookList, OnBookActionListener listener) {
        this.mBookList = bookList;
        this.mListener = listener;
    }

    /**
     * Tạo ViewHolder mới khi RecyclerView cần một ViewHolder mới.
     */
    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_placeholder, parent, false);
        return new BookViewHolder(view);
    }

    /**
     * Ràng buộc dữ liệu vào ViewHolder tại vị trí đã cho.
     */
    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = mBookList.get(position);

        holder.bookTitleTextView.setText(book.getTitle());

        String coverImageUrl = book.getCoverImage();
        Picasso.get()
                .load(coverImageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.bookCoverImageView);

        final String filePath = book.getFilePath();
        final String bookTitle = book.getTitle();
        final String bookId = book.getBookId();

        Log.d("BookAdapter", "Đang ràng buộc sách: " + bookTitle + ", URL: " + filePath);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), BookReaderActivity.class);
                intent.putExtra(BookReaderActivity.EXTRA_BOOK_URL, filePath);
                intent.putExtra(BookReaderActivity.EXTRA_BOOK_TITLE, bookTitle);
                intent.putExtra(BookReaderActivity.EXTRA_BOOK_ID, bookId);
                intent.putExtra(BookReaderActivity.EXTRA_BOOK_COVER_IMAGE, coverImageUrl);
                v.getContext().startActivity(intent);
            }
        });

        holder.collectionBookIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onAddToCollectionClick(book);
                }
            }
        });

        holder.settingsBookIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onEditBookClick(book);
                }
            }
        });

        holder.deleteBookIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onDeleteBookClick(book);
                }
            }
        });

        holder.favoriteBookIcon.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onAddToFavoritesClick(book);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mBookList.size();
    }

    /**
     * ViewHolder để giữ các View cho mỗi mục sách trong RecyclerView.
     */
    public static class BookViewHolder extends RecyclerView.ViewHolder {
        public TextView bookTitleTextView;

        public ImageView bookCoverImageView;
        public ImageView openBookIcon;
        public ImageView favoriteBookIcon;
        public ImageView collectionBookIcon;
        public ImageView deleteBookIcon;
        public ImageView settingsBookIcon;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các View con
            bookTitleTextView = itemView.findViewById(R.id.bookTitleTextView);
            bookCoverImageView = itemView.findViewById(R.id.bookCoverImageView);
            favoriteBookIcon = itemView.findViewById(R.id.favoriteBookIcon);
            collectionBookIcon = itemView.findViewById(R.id.collectionBookIcon);
            deleteBookIcon = itemView.findViewById(R.id.deleteBookIcon);
            settingsBookIcon = itemView.findViewById(R.id.settingsBookIcon);
        }
    }
}