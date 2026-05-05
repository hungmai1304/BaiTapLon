package com.auction.common.model.base;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BaseEntity implements Serializable {
    private String id;
    private LocalDateTime timeCreated;

    public BaseEntity() {}

    public BaseEntity(String id, LocalDateTime timeCreated) {
        this.id = id;
        this.timeCreated = timeCreated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(LocalDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }
}