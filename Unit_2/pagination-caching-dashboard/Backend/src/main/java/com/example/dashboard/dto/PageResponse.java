package com.example.dashboard.dto;

import org.springframework.data.domain.Page;
import java.io.Serializable;
import java.util.List;

/**
 * Optimized response payload: instead of returning the full Spring Data Page<T>
 * object (which duplicates a lot of metadata), we return only what a client
 * dashboard actually needs. This is the "optimize response payload" part of
 * Experiment 2.2.1.
 */
public class PageResponse<T> implements Serializable {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private String sortBy;
    private String direction;

    public PageResponse() {}

    public static <T> PageResponse<T> of(Page<T> page, String sortBy, String direction) {
        PageResponse<T> resp = new PageResponse<>();
        resp.content = page.getContent();
        resp.pageNumber = page.getNumber();
        resp.pageSize = page.getSize();
        resp.totalElements = page.getTotalElements();
        resp.totalPages = page.getTotalPages();
        resp.first = page.isFirst();
        resp.last = page.isLast();
        resp.sortBy = sortBy;
        resp.direction = direction;
        return resp;
    }

    public List<T> getContent() { return content; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isFirst() { return first; }
    public boolean isLast() { return last; }
    public String getSortBy() { return sortBy; }
    public String getDirection() { return direction; }
}
