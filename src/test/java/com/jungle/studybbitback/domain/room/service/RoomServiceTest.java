package com.jungle.studybbitback.domain.room.service;

import com.jungle.studybbitback.domain.member.entity.Member;
import com.jungle.studybbitback.domain.member.entity.MemberRoleEnum;
import com.jungle.studybbitback.domain.room.dto.room.CreateRoomRequestDto;
import com.jungle.studybbitback.domain.room.dto.room.CreateRoomResponseDto;
import com.jungle.studybbitback.domain.room.entity.Room;
import com.jungle.studybbitback.domain.room.respository.RoomRepository;
import com.jungle.studybbitback.domain.room.respository.RoomMemberRepository;
import com.jungle.studybbitback.domain.member.repository.MemberRepository;
import com.jungle.studybbitback.jwt.dto.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class RoomServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(RoomServiceTest.class);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock된 Member 객체 생성
        Member mockMember = new Member(1L, "testUser@example.com", "password", "testUser", MemberRoleEnum.ROLE_USER);
        logger.info("Member ID: {}", mockMember.getId());

        // Mock된 사용자 인증 설정
        CustomUserDetails userDetails = new CustomUserDetails(mockMember);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // SecurityContextHolder의 인증 정보 확인
        logger.info("Authenticated User ID from SecurityContextHolder: {}", userDetails.getMemberId());

        // ** memberRepository의 findById에 대한 Mock 설정 **
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(mockMember));

        // ** roomMemberRepository의 save에 대한 Mock 설정 **
        when(roomMemberRepository.save(any())).thenReturn(null);
    }

    @Test
    void createRoom_shouldReturnRoomResponseDto() {
        // Given
        CreateRoomRequestDto requestDto = CreateRoomRequestDto.builder()
                .name("Room1")
                .roomUrl("url1")
                .password("password")
                .detail("Test Room")
                .maxParticipants(10)
                .profileImageUrl("image.jpg")
                .build();

        logger.info("Testing createRoom with request DTO: {}", requestDto);

        // Mock Room 객체 설정
        Room room = new Room(1L, requestDto, 1L); // 새로운 생성자를 사용해 ID 설정
        when(roomRepository.saveAndFlush(any(Room.class))).thenReturn(room);
        logger.info("Mocked roomRepository to return Room: {}", room);

        // When
        CreateRoomResponseDto response = roomService.createRoom(requestDto);

        // Then
        logger.info("Received response from createRoom: {}", response);
        assertNotNull(response);
        assertEquals("Room1", response.getName());
        assertEquals("url1", response.getRoomUrl());
        assertNull(room.getMeetingId()); // 초기 생성 시 meetingId는 null
    }

    @Test
    void startMeeting_shouldGenerateNewMeetingId() {
        // Given
        CreateRoomRequestDto requestDto = CreateRoomRequestDto.builder().build();
        Room room = new Room(1L, requestDto, 1L); // 새로운 생성자를 사용해 ID 설정

        // ** roomRepository의 findById에 대한 Mock 설정 **
        when(roomRepository.findById(anyLong())).thenReturn(Optional.of(room));

        // When
        UUID meetingId = roomService.startMeeting(room.getId());

        // Then
        assertNotNull(meetingId); // 새로운 meetingId가 생성됨
        assertEquals(meetingId, room.getMeetingId()); // room의 meetingId가 생성된 ID와 같아야 함
        logger.info("Generated meetingId: {}", meetingId);
    }
    @Test
    void endMeeting_shouldSetMeetingIdToNull() {
        // Given
        CreateRoomRequestDto requestDto = CreateRoomRequestDto.builder().build();
        Room room = new Room(1L, requestDto, 1L); // 새로운 생성자를 사용해 ID 설정
        room.startMeeting(); // meetingId가 이미 생성된 상태

        // ** roomRepository의 findById에 대한 Mock 설정 **
        when(roomRepository.findById(anyLong())).thenReturn(Optional.of(room));

        // When
        roomService.endMeeting(room.getId());

        // Then
        assertNull(room.getMeetingId()); // meetingId가 null로 설정되어야 함
        logger.info("Meeting ended, meetingId is now: {}", room.getMeetingId());
    }
}