package com.vibenote.post.domain;

import java.time.LocalDateTime;

/** comments 행 + 작성자 닉네임·상태(users JOIN). Controller 응답으로 직접 내보내지 않는다 (F-18). */
public class Comment {

    private Long id;
    private long postId;
    private long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorNickname;
    private String authorStatus;

    public Comment() {}

    /** 새 댓글 (insert용). created_at·updated_at은 같은 시각으로 넣는다. */
    public Comment(long postId, long userId, String content, LocalDateTime createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public boolean isAuthorWithdrawn() {
        return "WITHDRAWN".equals(authorStatus);
    }

    /** 수정한 적이 있는지. 같은 내용으로 보낸 수정은 updated_at을 바꾸지 않으므로 여기에 잡히지 않는다. */
    public boolean isEdited() {
        return updatedAt.isAfter(createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorStatus() {
        return authorStatus;
    }

    public void setAuthorStatus(String authorStatus) {
        this.authorStatus = authorStatus;
    }
}
