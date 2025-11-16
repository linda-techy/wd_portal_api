package com.wd.api.dto;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> data;
    private int currentPage;
    private int totalPages;
    private int totalItems;
    private int itemsPerPage;
    private boolean hasNextPage;
    private boolean hasPreviousPage;

    public PaginatedResponse() {}

    public PaginatedResponse(List<T> data, int currentPage, int totalPages, int totalItems, int itemsPerPage, boolean hasNextPage, boolean hasPreviousPage) {
        this.data = data;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.itemsPerPage = itemsPerPage;
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
    }

    // Getters and Setters
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }
}
