package com.qfeed.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePostRequest {

    @NotBlank
    @Size(max = 2000)
    public String content;

    @NotBlank
    @Size(max = 64)
    public String clientRequestId;

}