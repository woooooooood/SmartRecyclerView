package com.wangbingcong.recyclerviewdemo.model;

/**
 * Created by wangbingcong on 16-3-10.
 */
public class ViewModel {
    private String title;
    private String description;

    public ViewModel(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
