package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.donkey.plain.IdUtils;

public class Candidate {
    private final long id, pageId;
    private final double dist;

    public Candidate(long id, double dist) {
        this.id = id;
        this.dist = dist;
        pageId = IdUtils.toPageId(id);
    }

    public double getDist() {
        return dist;
    }

    public long getId() {
        return id;
    }

    public long getPageId() {
        return pageId;
    }
}