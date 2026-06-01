package com.foodgroup.common.storage;

import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final Optional<S3Port> s3Port;

    @PostMapping("/profile")
    public ApiResponse<String> uploadProfileImage(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(upload("profiles", file));
    }

    @PostMapping("/food")
    public ApiResponse<String> uploadFoodImage(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(upload("foods", file));
    }

    private String upload(String prefix, MultipartFile file) {
        if (s3Port.isEmpty()) {
            throw new BusinessException(ErrorCode.S3_NOT_ENABLED);
        }

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        try {
            byte[] data = file.getBytes();
            String originalFilename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "upload";
            return s3Port.get().upload(prefix, originalFilename, data, contentType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_READ_FAILED);
        }
    }
}
