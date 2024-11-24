package com.jungle.studybbitback.domain.room.service.roomboard;

import com.jungle.studybbitback.domain.member.entity.Member;
import com.jungle.studybbitback.domain.member.repository.MemberRepository;
import com.jungle.studybbitback.domain.room.dto.roomboard.*;
import com.jungle.studybbitback.domain.room.entity.Room;
import com.jungle.studybbitback.domain.room.entity.roomboard.RoomBoard;
import com.jungle.studybbitback.domain.room.entity.roomboard.RoomBoardComment;
import com.jungle.studybbitback.domain.room.respository.RoomMemberRepository;
import com.jungle.studybbitback.domain.room.respository.RoomRepository;
import com.jungle.studybbitback.domain.room.respository.roomboard.RoomBoardCommentRepository;
import com.jungle.studybbitback.domain.room.respository.roomboard.RoomBoardRepository;
import com.jungle.studybbitback.jwt.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomBoardService {

    private final RoomBoardRepository roomBoardRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberRepository memberRepository;
    private final RoomBoardCommentRepository commentRepository;

    @Transactional
    public CreateRoomBoardResponseDto createRoomBoard(CreateRoomBoardRequestDto requestDto) {

        // 로그인된 사용자의 Member 객체를 가져오기
        Member member = getAuthenticatedMember();

        // 스터디룸 정보 조회
        Room room = roomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디룸이 존재하지 않습니다."));

        // 해당 사용자가 스터디룸 멤버인지 확인
        if (roomMemberRepository.findByRoomIdAndMemberId(requestDto.getRoomId(), member.getId()).isEmpty()) {
            throw new IllegalArgumentException("해당 스터디룸에 가입된 사용자만 게시글을 작성할 수 있습니다.");
        }

        // 게시글 생성
        RoomBoard roomBoard = new RoomBoard(room, requestDto.getTitle(), requestDto.getContent(), member.getId());
        roomBoardRepository.save(roomBoard);

        // 작성자의 닉네임 조회
        String createdByNickname = memberRepository.findById(member.getId())
                .map(Member::getNickname)
                .orElse("알 수 없음");

        // DTO 변환 후 반환
        return CreateRoomBoardResponseDto.from(roomBoard, createdByNickname);
    }

    // 해당 스터디룸 게시글 전체 조회
    @Transactional(readOnly = true)
    public Page<GetRoomBoardResponseDto> getRoomBoards(Long roomId, Pageable pageable) {
        return roomBoardRepository.findByRoomId(roomId, pageable)
                .map(roomBoard -> {
                    String createdByNickname = roomBoard.getCreatedByNickname(memberRepository);
                    return GetRoomBoardResponseDto.from(roomBoard, createdByNickname);
                });
    }


    // 게시글 상세 조회
    @Transactional(readOnly = true)
    public GetRoomBoardDetailResponseDto getRoomBoardDetail(Long roomBoardId, int page, int size) {
        return roomBoardRepository.findById(roomBoardId)
                .map(roomBoard -> {
                    String createdByNickname = memberRepository.findById(roomBoard.getCreatedBy())
                            .map(Member::getNickname)
                            .orElse("알 수 없음");

                    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                    //댓글 목록 조회
                    Page<RoomBoardComment> comments = commentRepository.findByRoomBoardId(roomBoardId, pageable);

                    return GetRoomBoardDetailResponseDto.from(roomBoard, createdByNickname, comments, memberRepository);

                })
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
    }

    private Member getAuthenticatedMember() {
        return memberRepository.findById(((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
    }

    //스터디룸 게시글 수정
    @Transactional
    public UpdateRoomBoardResponseDto updateRoomBoard(Long roomBoardId, UpdateRoomBoardRequestDto requestDto) {
        RoomBoard roomBoard = roomBoardRepository.findById(roomBoardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        // 게시글 정보 업데이트
        roomBoard.setTitle(requestDto.getTitle());
        roomBoard.setContent(requestDto.getContent());

        // 업데이트된 게시글 저장
        roomBoardRepository.save(roomBoard);

        // 응답 DTO 생성
        return new UpdateRoomBoardResponseDto(roomBoard.getId(), roomBoard.getTitle(), roomBoard.getContent());
    }

    //스터디룸 게시글 삭제
    @Transactional
    public void deleteRoomBoard(Long roomBoardId) {
        // 게시글을 데이터베이스에서 찾기
        RoomBoard roomBoard = roomBoardRepository.findById(roomBoardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        // 게시글 삭제 - 연관된 댓글들도 자동으로 삭제됨 (Cascade 설정 필요)
        roomBoardRepository.delete(roomBoard);
    }




}
