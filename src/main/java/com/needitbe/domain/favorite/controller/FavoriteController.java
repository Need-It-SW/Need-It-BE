package com.needitbe.domain.favorite.controller;

import com.needitbe.domain.favorite.dto.FavoriteResponse;
import com.needitbe.domain.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 관심 게시글 등록
    @PostMapping("/api/item-requests/{id}/favorites")
    public ResponseEntity<Void> addFavorite(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        favoriteService.addFavorite(memberId, id);

        return ResponseEntity.ok().build();
    }

    // 관심 게시글 해제
    @DeleteMapping("/api/item-requests/{id}/favorites")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        favoriteService.removeFavorite(memberId, id);

        return ResponseEntity.noContent().build();
    }

    // 내 관심 게시글 목록
    @GetMapping("/api/members/me/favorites")
    public ResponseEntity<List<FavoriteResponse>> getMyFavorites(
            Authentication authentication
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                favoriteService.getMyFavorites(memberId)
        );
    }
}