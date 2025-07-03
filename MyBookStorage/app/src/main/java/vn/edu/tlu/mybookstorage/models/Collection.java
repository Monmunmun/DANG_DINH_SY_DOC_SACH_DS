package vn.edu.tlu.mybookstorage.models;

import java.util.List;

public class Collection {
    private String id;
    private String name;
    private String userId;
    private List<String> bookIds;

    public Collection() {
    }

    public Collection(String id, String name, String userId, List<String> bookIds) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.bookIds = bookIds;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getBookIds() {
        return bookIds;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setBookIds(List<String> bookIds) {
        this.bookIds = bookIds;
    }
}
