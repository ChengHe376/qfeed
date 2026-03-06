package com.qfeed.post.dto;

import java.util.List;

public class FeedResponse {
    public List<PostItem> items;
    public String nextCursor;

    public FeedResponse(List<PostItem> items, String nextCursor) {
        this.items = items;
        this.nextCursor = nextCursor;
    }
}