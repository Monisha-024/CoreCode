package com.example.devassistant.controller;

import com.example.devassistant.dto.AskRequest;
import com.example.devassistant.dto.AskResponse;
import com.example.devassistant.repository.UserRepository;
import com.example.devassistant.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final UserRepository userRepository;

    public AssistantController(AssistantService assistantService, UserRepository userRepository) {
        this.assistantService = assistantService;
        this.userRepository = userRepository;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request, Authentication authentication) {
        Long userId = userRepository.findByEmail(authentication.getName())
                .map(u -> u.getId()).orElse(null);
        return ResponseEntity.ok(assistantService.ask(request, userId));
    }
}
