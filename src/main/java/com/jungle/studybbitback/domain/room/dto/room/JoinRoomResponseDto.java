package com.jungle.studybbitback.domain.room.dto.room;

import lombok.Getter;

@Getter
public class JoinRoomResponseDto {
    private Long roomId;
    private Long memberId;
    private int participantCount;
    private String message;

    public JoinRoomResponseDto(Long roomId, Long memberId, int participantCount, String message) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.participantCount = participantCount;
        this.message = message;
    }
}
