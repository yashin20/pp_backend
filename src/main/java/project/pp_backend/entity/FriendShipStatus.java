package project.pp_backend.entity;

public enum FriendShipStatus {
    /**
     * PENDING : 친구 요청 발송 + 수락은 되지 않음.
     * ACCEPTED : 친구 요청 수락 (친구 관계 성립)
     * REJECTED : 친구 요청 거절 OR 발송자 요청 취소
     * BLOCKED : 친구 차단
     */

    PENDING,
    ACCEPTED,
    REJECTED,
    BLOCKED
}
