package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class Pagination {
    @SerializedName("page")
    private int page;
    
    @SerializedName("limit")
    private int limit;
    
    @SerializedName("total")
    private int total;
    
    @SerializedName("totalPages")
    private int totalPages;

    public Pagination() {
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

