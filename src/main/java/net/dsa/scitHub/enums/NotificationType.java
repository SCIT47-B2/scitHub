package net.dsa.scitHub.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    NEW_LIKE_ON_POST("あなたの投稿に新しい「いいね！」が付きました。"),
    NEW_COMMENT_ON_POST("あなたの投稿に新しいコメントがありました。"),
    NEW_MESSAGE("新しいメッセージが届きました。"),
    NEW_EVENT("全体スケジュールが登録されました。");

    private final String title;
}
