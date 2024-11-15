package com.jungle.studybbitback.domain.room.controller;

import com.jungle.studybbitback.domain.room.dto.room.*;
import com.jungle.studybbitback.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 방 생성
    @PostMapping
    public ResponseEntity<CreateRoomResponseDto> createRoom(@RequestBody CreateRoomRequestDto requestDto) {
        CreateRoomResponseDto response = roomService.createRoom(requestDto);
        return ResponseEntity.ok(response);
    }

    // 모든 방 조회 (메인에서 보이는 전체 스터디룸 목록)
    @GetMapping
    public ResponseEntity<List<GetRoomResponseDto>> getRoomAll() {
        List<GetRoomResponseDto> rooms = roomService.getRoomAll();
        return ResponseEntity.ok(rooms);
    }

    // 특정 방 조회
    @GetMapping("/{roomId}")
    public ResponseEntity<GetRoomResponseDto> getRoomById(@PathVariable("roomId") Long id) {
        GetRoomResponseDto room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    // 상세 방 설명
    @GetMapping("/detail/{roomId}")
    public ResponseEntity<GetRoomDetailResponseDto> getRoomDetail(@PathVariable("roomId") Long id) {
        GetRoomDetailResponseDto roomDetail = roomService.getRoomDetail(id);
        return ResponseEntity.ok(roomDetail);
    }

    // 스터디룸 대시보드 홈 조회
    @GetMapping("/dashboard/{roomId}")
    public ResponseEntity<GetRoomDashboardResponseDto> getRoomDashboard(@PathVariable("roomId") Long id) {
        GetRoomDashboardResponseDto roomDashboard = roomService.getRoomDashboard(id);
        return ResponseEntity.ok(roomDashboard);
    }

    // 방 수정
    @PostMapping("/{roomId}")
    public ResponseEntity<UpdateRoomResponseDto> updateRoom(@PathVariable("roomId") Long id, @RequestBody UpdateRoomRequestDto requestDto) {
        UpdateRoomResponseDto response = roomService.updateRoom(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 방 삭제
    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteRoom(@PathVariable("roomId") Long id) {
        String response = roomService.deleteRoom(id);
        return ResponseEntity.ok(response);
    }

    // 화상회의 시작
    @PostMapping("/{roomId}/start-meeting")
    public ResponseEntity<String> startMeeting(@PathVariable("roomId") Long roomId) {
        UUID meetingId = roomService.startMeeting(roomId);
        return ResponseEntity.ok(meetingId.toString()); // 생성된 meetingId를 반환
    }

    // 화상회의 종료
    @PostMapping("/{roomId}/end-meeting")
    public ResponseEntity<String> endMeeting(@PathVariable("roomId") Long roomId) {
        roomService.endMeeting(roomId);
        return ResponseEntity.ok("화상회의가 종료되었습니다.");
    }

    // 방에 참여하기 (사용자가 스스로 방에 들어감)
    @PostMapping("/{roomId}/join")
    public ResponseEntity<JoinRoomResponseDto> joinRoom(@PathVariable("roomId") Long roomId, @RequestBody JoinRoomRequestDto requestDto) {
        JoinRoomResponseDto response = roomService.joinRoom(roomId, requestDto);
        return ResponseEntity.ok(response);
    }


}
