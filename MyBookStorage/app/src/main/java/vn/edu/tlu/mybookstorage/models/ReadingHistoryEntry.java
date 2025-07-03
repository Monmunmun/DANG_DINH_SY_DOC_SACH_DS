package vn.edu.tlu.mybookstorage.models;


public class ReadingHistoryEntry {
    private String bookId;
    private String bookTitle;
    private String filePath;
    private String coverImage;
    private int lastReadPage;
    private long timestamp;

    public ReadingHistoryEntry() {
    }

    public ReadingHistoryEntry(String bookId, String bookTitle, String filePath, String coverImage, int lastReadPage, long timestamp) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.filePath = filePath;
        this.coverImage = coverImage;
        this.lastReadPage = lastReadPage;
        this.timestamp = timestamp;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public int getLastReadPage() {
        return lastReadPage;
    }

    public void setLastReadPage(int lastReadPage) {
        this.lastReadPage = lastReadPage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}