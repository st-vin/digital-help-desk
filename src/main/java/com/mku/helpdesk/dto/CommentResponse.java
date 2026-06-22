package com.mku.helpdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String authorName;   // flattened from comment.getAuthor().getFullName()
    private String authorRole;   // "STUDENT" or "ICT_STAFF" — frontend uses this to align chat bubbles left/right
    private LocalDateTime createdAt;
}
