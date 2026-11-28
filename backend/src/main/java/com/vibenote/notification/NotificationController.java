package com.vibenote.notification;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.notification.dto.NotificationResponse;
import com.vibenote.notification.dto.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 헤더 알림 (NT-1). 모두 로그인 필요이고 내가 받은 알림만 다룬다. */
@Tag(name = "알림", description = "헤더 알림 목록·안 읽은 수·읽음 처리 (NT-1)")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "내 알림 목록",
            description = "내가 받은 알림을 최신순(같으면 id 내림차순)으로 페이지 조회한다. actor는 일으킨 회원의 지금 닉네임이다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "페이지 값 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal AuthUser user, @Valid @ParameterObject PageQuery page) {
        return notificationService.list(user.id(), page);
    }

    @Operation(summary = "안 읽은 알림 수", description = "헤더 종 배지에 쓰는 안 읽은 알림 수.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthUser user) {
        return notificationService.unreadCount(user.id());
    }

    @Operation(summary = "알림 읽음", description = "알림 하나를 읽음으로 바꾼다. 이미 읽었으면 그대로 204(멱등)다.")
    @ApiResponse(responseCode = "204", description = "읽음 처리", content = @Content)
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 남의 알림 (NOTIFICATION_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @Parameter(description = "알림 ID", example = "41") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        notificationService.markRead(id, user.id());
    }

    @Operation(summary = "모두 읽음", description = "내 안 읽은 알림을 모두 읽음으로 바꾼다. 없어도 204다.")
    @ApiResponse(responseCode = "204", description = "읽음 처리", content = @Content)
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthUser user) {
        notificationService.markAllRead(user.id());
    }
}
