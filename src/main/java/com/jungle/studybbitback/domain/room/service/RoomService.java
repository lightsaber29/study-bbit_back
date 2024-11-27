package com.jungle.studybbitback.domain.room.service;

import com.jungle.studybbitback.common.file.service.FileService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileService fileService;

    @Transactional
    public CreateRoomResponseDto createRoom(CreateRoomRequestDto requestDto, MultipartFile roomImage) {

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = userDetails.getMemberId();

        // 방 생성자의 Member 정보 가져오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 비공개 방일 경우 비밀번호가 필수
        String password = null;
        if (requestDto.isPrivate()) { //true
            // 비공개 방에서 비밀번호를 입력하지 않으면 예외 처리
            if (!StringUtils.hasText(requestDto.getPassword())) {
                throw new IllegalArgumentException("비공개 방은 비밀번호를 설정해야 합니다.");
            }
            password = requestDto.getPassword();  // 비밀번호 설정
        }

        String roomImageUrl = null;
        if (roomImage != null && !roomImage.isEmpty()) {
            roomImageUrl = fileService.uploadFile(roomImage, "images", memberId);
        }

        //방 생성
        Room room = new Room(requestDto, memberId, roomImageUrl);
        roomRepository.save(room);

        //방 개설한 사람은 자동으로 참여명단에 추가.
        RoomMember roomMember = new RoomMember(room, member);
        roomMemberRepository.save(roomMember);

        return new CreateRoomResponseDto(room);
    }

    public Page<GetRoomResponseDto> getRoomAll(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return roomRepository.findAll(pageable)
                .map(room -> {
                    Member leader = memberRepository.findById(room.getLeaderId())
                            .orElseThrow(() -> new IllegalArgumentException("방장 정보를 찾을 수 없습니다."));
                    return new GetRoomResponseDto(
                            room,
                            leader.getNickname(),
                            leader.getProfileImageUrl()
                    );
                });
    }

    public GetRoomResponseDto getRoomById(Long id) {
        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );
        Member leader = memberRepository.findById(room.getLeaderId()).orElseThrow(
                () -> new IllegalArgumentException("방장 정보를 찾을 수 없습니다.")
        );
        return new GetRoomResponseDto(
                room,
                leader.getNickname(),
                leader.getProfileImageUrl()

        );
    }

    public Page<GetRoomResponseDto> searchRooms(String keyword, Pageable pageable) {
        Page<Room> rooms = roomRepository.searchByNameAndDetailIgnoreCase(keyword, pageable);
        return rooms.map(room -> {
            Member leader = memberRepository.findById(room.getLeaderId()).orElseThrow(
                    () -> new IllegalArgumentException("방장 정보를 찾을 수 없습니다.")
            );
            return new GetRoomResponseDto(
                    room,
                    leader.getNickname(),
                    leader.getProfileImageUrl()
            );
        });
    }


    public GetRoomDetailResponseDto getRoomDetail(Long id) {

        Room room = roomRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 방입니다.")
        );
        // leaderId를 통해 방장 정보 조회
        Member leader = memberRepository.findById(room.getLeaderId())
                .orElseThrow(() -> new IllegalArgumentException("방장이 존재하지 않습니다."));

        // 닉네임과 프로필 이미지를 조회
        String leaderNickname = leader.getNickname();
        String leaderImageUrl = leader.getProfileImageUrl();

        return new GetRoomDetailResponseDto(room, leaderNickname, leaderImageUrl);
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

        String roomImageUrl = room.getProfileImageUrl();
        if(requestDto.isRoomImageChanged() &&
                requestDto.getRoomImage() != null
                && !requestDto.getRoomImage().isEmpty()) {
            if(StringUtils.hasText(roomImageUrl)){
                //기존 이미지가 있으면 삭제
                fileService.deleteFile(roomImageUrl);
            }
            // 새 이미지 업로드
            roomImageUrl = fileService.uploadFile(requestDto.getRoomImage(), "images", room.getId());

        }

        // 공개 방이면 비밀번호는 null 처리
        String password = null;
        if (room.isPrivate() && StringUtils.hasText(requestDto.getPassword())) {
            password = requestDto.getPassword();
        }

        room.updateDetails(requestDto.getDetail(), requestDto.getPassword(), roomImageUrl, requestDto.isRoomImageChanged());
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
