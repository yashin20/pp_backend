package project.pp_backend.dto;

import lombok.Builder;
import lombok.Data;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.FriendShipStatus;
import project.pp_backend.entity.Member;

import java.time.LocalDateTime;

public class FriendShipDto {

    @Data
    public static class Response {
        private Long id;
        private String ownerUsername;
        private String friendUsername;
        private FriendShipStatus status;
        private String friendNickname;
        private LocalDateTime createdAt;

        //Entity -> Dto
        public Response(FriendShip friendShip) {
            this.id = friendShip.getId();
            this.ownerUsername = friendShip.getOwner().getUsername();
            this.friendUsername = friendShip.getFriend().getUsername();
            this.status = friendShip.getStatus();
            this.friendNickname = friendShip.getFriend().getNickname();
            this.createdAt = friendShip.getCreatedAt();
        }
    }

    @Data
    @Builder
    public static class CreateRequest {
        private String ownerUsername;
        private String friendUsername;
        private FriendShipStatus status;

        //Dto -> Entity
        public FriendShip toEntity(Member owner, Member friend) {
            return new FriendShip(owner, friend, status);
        }
    }

    @Data
    public static class DeleteRequest {
        private String ownerUsername;
        private String friendUsername;
    }
}
