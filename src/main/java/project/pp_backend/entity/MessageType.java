package project.pp_backend.entity;

public enum MessageType {
    CHAT, //일반 채팅
    WHISPER, //귓속말 채팅
    IMAGE, //이미지 타입 메시지
    ENTER, //시스템 입장 메시지
    LEAVE //시스템 퇴장 메시지
}
