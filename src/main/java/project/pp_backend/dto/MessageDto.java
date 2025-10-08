package project.pp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import project.pp_backend.entity.Member;
import project.pp_backend.entity.Message;
import project.pp_backend.entity.MessageType;
import project.pp_backend.entity.Room;

import java.time.LocalDateTime;

public class MessageDto {

    @Data
    public static class Response {
        private Long id;
        private String content;
        private MessageType type;
        private Long memberId;
        private Long roomId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        //Entity -> ResponseDto
        public Response(Message message) {
            this.id = message.getId();
            this.content = message.getContent();
            this.type = message.getType();
            this.memberId = message.getMember().getId();
            this.roomId = message.getRoom().getId();
            this.createdAt = message.getCreatedAt();
            this.updatedAt = message.getUpdatedAt();
        }
    }

    @Data
    public static class CreateRequest {
        @NotBlank(message = "메시지가 입력되지 않았습니다.")
        private String content;
        private MessageType type;
        private Long memberId;
        private Long roomId;

        //RequestDto -> Entity
        public Message toEntity(Member member, Room room) {
            return new Message(
                    this.content,
                    this.type,
                    member,
                    room
            );
        }
    }
}
