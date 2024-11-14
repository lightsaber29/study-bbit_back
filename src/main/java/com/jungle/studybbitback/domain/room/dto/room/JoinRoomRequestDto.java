package com.jungle.studybbitback.domain.room.dto.room;

import lombok.Getter;

@Getter
public class JoinRoomRequestDto {
    private Long roomId;
    private Long memberId;

    public JoinRoomRequestDto(Long roomId, Long memberId) {
        this.roomId = roomId;
        this.memberId = memberId;
    }
}
