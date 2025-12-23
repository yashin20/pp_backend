package project.pp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.pp_backend.entity.Message;
import project.pp_backend.entity.MessageType;

import java.time.LocalDateTime;

public class MessageDto {

    @Data
    public static class Response {
        private Long id;
        private Long roomId;
        private String content;
        private MessageType type;
        private Long memberId;
        private String nickname;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // 귓속말 대상자 (null 이면 일반 메시지)
        private String recipientUsername;

        //Entity -> ResponseDto
        public Response(Message message) {
            this.id = message.getId();
            this.roomId = message.getRoom().getId();
            this.content = message.getContent();
            this.type = message.getType();
            this.memberId = message.getMember().getId();
            this.nickname = message.getMember().getNickname();
            this.createdAt = message.getCreatedAt();
            this.updatedAt = message.getUpdatedAt();

            this.recipientUsername = message.getRecipient() != null ? message.getRecipient().getUsername() : null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "메시지가 입력되지 않았습니다.")
        private String content;
        private MessageType type;
        //귓속말 수신자 (귓속말일때만 사용)
        private String recipientUsername;
    }
}
