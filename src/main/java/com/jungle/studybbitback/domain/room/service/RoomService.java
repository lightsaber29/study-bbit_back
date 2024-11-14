package com.jungle.studybbitback.domain.room.service;

import com.jungle.studybbitback.domain.member.entity.Member;
import com.jungle.studybbitback.domain.member.repository.MemberRepository;
import com.jungle.studybbitback.domain.room.dto.room.*;
import com.jungle.studybbitback.domain.room.entity.Room;
import com.jungle.studybbitback.domain.room.entity.RoomMember;
import com.jungle.studybbitback.domain.room.respository.RoomMemberRepository;
import com.jungle.studybbitback.domain.room.respository.RoomRepository;
import com.jungle.studybbitback.jwt.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreateRoomResponseDto createRoom(CreateRoomRequestDto requestDto) {

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = userDetails.getMemberId();

        // 방 생성자의 Member 정보 가져오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Room room = new Room(requestDto, memberId);
        Room savedRoom = roomRepository.saveAndFlush(room);

        RoomMember roomMember = new RoomMember(savedRoom, member);
        roomMemberRepository.save(roomMember);

        return new CreateRoomResponseDto(savedRoom);
    }

    public List<GetRoomResponseDto> getRoomAll() {

        List<Room> roomList = roomRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return roomList.stream()
                .map(GetRoomResponseDto::new)
                .collect(Collectors.toList());
    }

    public GetRoomResponseDto getRoomById(Long id) {

        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );
        return new GetRoomResponseDto(room);
    }

    public GetRoomDetailResponseDto getRoomDetail(Long id) {

        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );
        return new GetRoomDetailResponseDto(room);
    }

    // 대시보드 접근 시 멤버 여부 확인
    @Transactional
    public GetRoomDashboardResponseDto getRoomDashboard(Long roomId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = userDetails.getMemberId();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 방에 가입된 회원인지 확인
        if (roomMemberRepository.findByRoomIdAndMemberId(roomId, memberId).isEmpty()) {
            throw new AccessDeniedException("해당 스터디룸에 가입된 사용자만 접근할 수 있습니다.");
        }

        return new GetRoomDashboardResponseDto(room);
    }

    @Transactional
    public UpdateRoomResponseDto updateRoom(Long id, UpdateRoomRequestDto requestDto) {
        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = userDetails.getMemberId();

        if (room.getLeaderId() != memberId){
            throw new IllegalArgumentException("스터디장만 수정할 수 있습니다.");
        }
        room.updateDetails(requestDto);
        roomRepository.save(room);

        return new UpdateRoomResponseDto(room);
    }

    @Transactional
    public String deleteRoom(Long id) {
        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = userDetails.getMemberId();

        if (room.getLeaderId() != memberId) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        // RoomMember 테이블도 먼저 삭제 후 함께 삭제되도록 설정
        roomMemberRepository.deleteByRoom(room);

        roomRepository.delete(room);
        return "스터디룸이 삭제되었습니다.";
    }

    @Transactional
    public UUID startMeeting(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        room.startMeeting(); // 매번 새로운 meetingId 생성
        return room.getMeetingId();
    }

    @Transactional
    public void endMeeting(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        room.endMeeting();
    }

    @Transactional
    public JoinRoomResponseDto joinRoom(Long roomId, JoinRoomRequestDto requestDto) {
//        Long roomId = requestDto.getRoomId();

        // 로그인한 사용자 정보 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = userDetails.getMemberId();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 이미 방에 참여한 경우 확인
        if (roomMemberRepository.existsByRoomAndMember(room, member)) {
            throw new IllegalStateException("이미 참여한 방입니다.");
        }

        // 방 참여 처리
        RoomMember roomMember = new RoomMember(room, member);
        roomMemberRepository.save(roomMember);

        int participantCount = roomMemberRepository.countByRoom(room);
        return new JoinRoomResponseDto(roomId, memberId, participantCount, memberId +"님이 방에 참여했습니다.");
    }
}
