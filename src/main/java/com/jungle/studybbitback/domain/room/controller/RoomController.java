package com.jungle.studybbitback.domain.room.controller;

import com.jungle.studybbitback.domain.room.dto.room.*;
import com.jungle.studybbitback.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 모든 방 조회
    @GetMapping
    public ResponseEntity<List<GetRoomResponseDto>> getAllRooms() {
        List<GetRoomResponseDto> rooms = roomService.getRoomAll();
        return ResponseEntity.ok(rooms);
    }

    // 특정 방 조회
    @GetMapping("/{roomId}")
    public ResponseEntity<GetRoomResponseDto> getRoomById(@PathVariable("roomId") Long id) {
        GetRoomResponseDto room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
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

    // 방에 참여하기 (사용자가 스스로 방에 들어감)
    @PostMapping("/{roomId}/join")
    public ResponseEntity<JoinRoomResponseDto> joinRoom(@PathVariable("roomId") Long roomId, @RequestBody JoinRoomRequestDto requestDto) {
        JoinRoomResponseDto response = roomService.joinRoom(roomId, requestDto);
        return ResponseEntity.ok(response);
    }
}