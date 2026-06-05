package com.foodgroup.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    NICKNAME_DUPLICATE("NICKNAME_DUPLICATE", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    EMAIL_INVALID_DOMAIN("EMAIL_INVALID_DOMAIN", HttpStatus.BAD_REQUEST, "학교 이메일 도메인이 아닙니다"),
    EMAIL_ALREADY_VERIFIED("EMAIL_ALREADY_VERIFIED", HttpStatus.CONFLICT, "이미 인증된 이메일입니다"),
    EMAIL_CODE_INVALID("EMAIL_CODE_INVALID", HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다"),
    EMAIL_CODE_EXPIRED("EMAIL_CODE_EXPIRED", HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다"),

    // Room
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다"),
    ROOM_FULL("ROOM_FULL", HttpStatus.CONFLICT, "방이 가득 찼습니다"),
    ROOM_STATUS_INVALID("ROOM_STATUS_INVALID", HttpStatus.CONFLICT, "유효하지 않은 방 상태 전이입니다"),
    ALREADY_JOINED("ALREADY_JOINED", HttpStatus.CONFLICT, "이미 참여한 방입니다"),
    ALREADY_IN_ACTIVE_ROOM("ALREADY_IN_ACTIVE_ROOM", HttpStatus.CONFLICT, "이미 진행 중인 방에 참여하고 있습니다"),
    NOT_ROOM_HOST("NOT_ROOM_HOST", HttpStatus.FORBIDDEN, "방장만 가능한 작업입니다"),
    HOST_CANNOT_LEAVE("HOST_CANNOT_LEAVE", HttpStatus.BAD_REQUEST, "다른 참여자가 있어 방장은 나갈 수 없습니다"),
    NOT_ROOM_PARTICIPANT("NOT_ROOM_PARTICIPANT", HttpStatus.FORBIDDEN, "방 참여자가 아닙니다"),
    ROOM_NOT_CLOSEABLE("ROOM_NOT_CLOSEABLE", HttpStatus.CONFLICT, "마감할 수 없는 상태입니다"),
    ROOM_COMPLETED("ROOM_COMPLETED", HttpStatus.CONFLICT, "완료된 방은 취소할 수 없습니다"),

    // Order
    ORDER_ITEM_NOT_FOUND("ORDER_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "주문 항목을 찾을 수 없습니다"),
    ORDER_NOT_CONFIRMABLE("ORDER_NOT_CONFIRMABLE", HttpStatus.CONFLICT, "주문 확정 조건을 충족하지 않습니다"),
    ORDER_LOCKED("ORDER_LOCKED", HttpStatus.CONFLICT, "주문이 확정된 후에는 수정할 수 없습니다"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // Storage
    S3_NOT_ENABLED("S3_NOT_ENABLED", HttpStatus.SERVICE_UNAVAILABLE, "파일 업로드 서비스가 비활성화되어 있습니다"),
    INVALID_FILE("INVALID_FILE", HttpStatus.BAD_REQUEST, "파일이 비어있습니다"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", HttpStatus.BAD_REQUEST, "파일 크기는 10MB를 초과할 수 없습니다"),
    UNSUPPORTED_FILE_TYPE("UNSUPPORTED_FILE_TYPE", HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다. (jpeg, png, webp만 허용)"),
    FILE_READ_FAILED("FILE_READ_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽는 중 오류가 발생했습니다"),

    // Common
    INVALID_INPUT("INVALID_INPUT", HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    OPTIMISTIC_LOCK("OPTIMISTIC_LOCK", HttpStatus.CONFLICT, "동시 접근 충돌이 발생했습니다. 다시 시도해주세요"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
