package project.pp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.FriendShipStatus;
import project.pp_backend.entity.Member;

import java.time.LocalDateTime;

public class FriendShipDto {

    @Data
    public static class Response {
        private Long id;
        private Long ownerId;
        private Long friendId;
        private FriendShipStatus status;
        private String ownerNickname;
        private String friendNickname;
        private LocalDateTime createdAt;

        //Entity -> Dto
        public Response(FriendShip friendShip) {
            this.id = friendShip.getId();
            this.ownerId = friendShip.getOwner().getId();
            this.friendId = friendShip.getFriend().getId();
            this.status = friendShip.getStatus();
            this.ownerNickname = friendShip.getOwner().getNickname();
            this.friendNickname = friendShip.getFriend().getNickname();
            this.createdAt = friendShip.getCreatedAt();
        }
    }

    @Data
    @Builder
    public static class Request {
        private Long ownerId;
        private Long friendId;
    }

    @Data
    @Builder
    public static class SendNicknameRequest {
        private Long ownerId;
        private String targetNickname;
    }

}
