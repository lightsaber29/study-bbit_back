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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        // 비공개 방일 경우 비밀번호가 필수
        if (requestDto.isPrivate() && (requestDto.getPassword() == null || requestDto.getPassword().isEmpty())) {
            throw new IllegalArgumentException("비공개 방은 비밀번호를 설정해야 합니다.");
        }

        //방 생성
        Room room = new Room(requestDto, memberId);
        Room savedRoom = roomRepository.saveAndFlush(room);

        //방 개설한 사람은 자동으로 참여명단에 추가.
        RoomMember roomMember = new RoomMember(savedRoom, member);
        roomMemberRepository.save(roomMember);

        return new CreateRoomResponseDto(savedRoom);
    }

    public Page<GetRoomResponseDto> getRoomAll(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Room> roomPage = roomRepository.findAll(pageable);
        return roomPage.map(GetRoomResponseDto::new);
    }

    public GetRoomResponseDto getRoomById(Long id) {

        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );
        return new GetRoomResponseDto(room);
    }

    public Page<GetRoomResponseDto> searchRooms(String keyword, Pageable pageable) {
        Page<Room> rooms = roomRepository.findByNameContainingOrDetailContaining(keyword, keyword, pageable); //nameKeyword, detailKeyword 순
        return rooms.map(GetRoomResponseDto::new);
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

    // 공개 방이면 비밀번호 무시
        if (!room.isPrivate()) {
            requestDto = UpdateRoomRequestDto.builder()
                    .detail(requestDto.getDetail())
                    .password(null) // 비밀번호 제거
                    .profileImageUrl(requestDto.getProfileImageUrl())
                    .build();
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

}
