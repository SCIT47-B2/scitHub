package net.dsa.scitHub.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dsa.scitHub.entity.user.Message;
import net.dsa.scitHub.entity.user.User;

@Getter
@Setter
@NoArgsConstructor
public class MessageCreateRequestDto {
    private String receiverUsername;
    private Integer receiverId;
    private String title;
    private String content;

    public Message toEntity(User sender, User receiver) {
        return Message.builder()
                .sender(sender)
                .receiver(receiver)
                .title(title)
                .content(content)
                .build();
    }
}

