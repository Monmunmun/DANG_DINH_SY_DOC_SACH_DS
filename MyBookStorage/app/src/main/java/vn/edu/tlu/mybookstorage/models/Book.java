package vn.edu.tlu.mybookstorage.models;

public class Book {
    private String bookId;
    private String uid;
    private String title;
    private String filePath;
    private String coverImage;
    private long uploadDate;

    public Book() {
    }

    public Book(String bookId, String uid, String title, String filePath, String coverImage, long uploadDate) {
        this.bookId = bookId;
        this.uid = uid;
        this.title = title;
        this.filePath = filePath;
        this.coverImage = coverImage;
        this.uploadDate = uploadDate;
    }

    public String getBookId() {
        return bookId;
    }

    public String getUid() {
        return uid;
    }

    public String getTitle() {
        return title;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public long getUploadDate() {
        return uploadDate;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public void setUploadDate(long uploadDate) {
        this.uploadDate = uploadDate;
    }
}

