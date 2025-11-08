package project.pp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import project.pp_backend.entity.Room;

import java.time.LocalDateTime;

public class RoomDto {

    @Data
    public static class Response {
        private Long id;
        private String name;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        //Entity -> ResponseDto
        public Response(Room room) {
            this.id = room.getId();
            this.name = room.getName();
            this.createdAt = room.getCreatedAt();
            this.updatedAt = room.getUpdatedAt();
        }
    }

    @Data
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        private String name;

        public Room toEntity() {
            return new Room(name);
        }
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        private String name;
    }

}
