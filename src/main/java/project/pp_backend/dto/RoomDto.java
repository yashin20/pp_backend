package project.pp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import project.pp_backend.entity.Room;

import java.time.LocalDateTime;
import java.util.List;

public class RoomDto {

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        //Entity -> ResponseDto
        public Response(Room room) {
            this.id = room.getId();
            this.name = room.getName();
            this.createdAt = room.getCreatedAt();
            this.updatedAt = room.getUpdatedAt();
        }

        public Response(Long id, String name, String lastMessage, LocalDateTime lastMessageTime, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.createdAt = createdAt;
        }
    }

    @Data
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        private String name;
        @NotEmpty(message = "참가할 회원 목록 리스트는 필수입니다.")
        List<Long> memberIds;

        public Room toEntity() {
            return new Room(name);
        }
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        private String name;
    }

    @Data
    public static class InviteRequest {
        private Long roomId;
        private List<String> usernames;
    }

}
