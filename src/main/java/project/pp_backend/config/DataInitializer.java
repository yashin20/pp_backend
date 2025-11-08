package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import project.pp_backend.dto.FriendShipDto;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.dto.RoomDto;
import project.pp_backend.entity.MemberRole;
import project.pp_backend.entity.MessageType;
import project.pp_backend.service.FriendShipService;
import project.pp_backend.service.MemberService;
import project.pp_backend.service.MessageService;
import project.pp_backend.service.RoomService;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final MemberService memberService;
    private final MessageService messageService;
    private final FriendShipService friendShipService;
    private final RoomService roomService;

    @Override
    public void run(String... args) throws Exception {
        initializeTestData();
    }

    private void initializeTestData() {
        System.out.println("--- [Dev Profile] Initializing Test Member Data ---");

        createMemberIfNotExist("member1", "1q2w3e4r", "maru", "maru@example.com");
        createMemberIfNotExist("member2", "1q2w3e4r", "nickname2", "nickname2@example.com");
        createMemberIfNotExist("member3", "1q2w3e4r", "nickname3", "nickname3@example.com");

        createFriendShipIfNotExist("member1", "member2");
        createFriendShipIfNotExist("member1", "member3");

        createRoomIfNotExist("room1 - 1", "member1");
        createRoomIfNotExist("room2 - 1", "member1");
        createRoomIfNotExist("room3 - 3", "member3");

        System.out.println("--- Test Member Data Initialization Complete ---");
    }

    private void createMemberIfNotExist(String username, String password, String nickname, String email) {
        try {
            MemberDto.CreateRequest request = MemberDto.CreateRequest.builder()
                    .username(username)
                    .password(password)
                    .nickname(nickname)
                    .email(email)
                    .role(MemberRole.USER)
                    .build();

            memberService.createMember(request);
            System.out.println("✅ Member created: " + username);

        } catch (RuntimeException e) {
            // 사용자 ID가 이미 존재할 경우 (MemberService에서 처리했다고 가정)
            System.out.println("ℹ️ Member already exists: " + username);
        } catch (Exception e) {
            System.err.println("❌ Error creating member " + username + ": " + e.getMessage());
        }
    }

    private void createFriendShipIfNotExist(String ownerUsername, String friendUsername) {
        try {
            FriendShipDto.CreateRequest request = FriendShipDto.CreateRequest.builder()
                    .ownerUsername(ownerUsername)
                    .friendUsername(friendUsername)
                    .build();

            friendShipService.createFriendShip(request);
            System.out.println("✅ FriendShip created: " + ownerUsername + " - " + friendUsername);

        } catch (RuntimeException e) {
            System.out.println("ℹ️ FriendShip already exists: " + ownerUsername + " - " + friendUsername);
        } catch (Exception e) {
            System.err.println("❌ Error creating FriendShip: " + ownerUsername + " - " + friendUsername);
        }
    }

    private void createRoomIfNotExist(String roomName, String username) {
        try {
            RoomDto.CreateRequest request = RoomDto.CreateRequest.builder()
                    .name(roomName)
                    .build();

            roomService.createRoom(username, request);
            System.out.println("✅ Room created: " + roomName);

        } catch (RuntimeException e) {
            System.out.println("ℹ️ Room already exists: " + roomName);
        } catch (Exception e) {
            System.err.println("❌ Error creating Room: " + roomName + ": " + e.getMessage());
        }
    }


    private void createMessageIfNotExist(String username, Long roomId, String content) {
        try {
            MessageDto.CreateRequest request = MessageDto.CreateRequest.builder()
                    .content(content)
                    .type(MessageType.CHAT)
                    .roomId(roomId)
                    .sender(username)
                    .build();

            messageService.createMessage(username, roomId, request);
            System.out.println("✅ Message created at roomId: " + roomId);

        } catch (RuntimeException e) {
            System.out.println("ℹ️ Message already exists roomId: " + roomId);
        } catch (Exception e) {
            System.err.println("❌ Error creating Message roomId: " + roomId + ": " + e.getMessage());
        }
    }
}
