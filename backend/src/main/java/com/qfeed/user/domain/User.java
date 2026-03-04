package com.qfeed.user.domain;

import java.time.LocalDateTime;

public class User {
    public Long id;
    public String username;
    public String passwordHash;
    public LocalDateTime createdAt;
}
