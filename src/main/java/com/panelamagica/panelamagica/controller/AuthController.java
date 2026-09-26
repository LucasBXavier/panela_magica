package com.panelamagica.panelamagica.controller;

import com.panelamagica.panelamagica.dto.SuccessResponseDTO;
import com.panelamagica.panelamagica.dto.login.LoginRequestDTO;
import com.panelamagica.panelamagica.dto.login.LoginResponseDTO;
import com.panelamagica.panelamagica.dto.login.RefreshTokenRequestDTO;
import com.panelamagica.panelamagica.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Login de usuário",
            description = "Autentica um usuário existente e devolve o token JWT (access) e um refresh token. "
                    + "Após 5 tentativas malsucedidas para o mesmo e-mail (ou 20 para o mesmo IP) em 15 minutos, responde 429 com `Retry-After`.")
    @PostMapping("/login")
    public ResponseEntity<SuccessResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO dto,
                                                                      HttpServletRequest request) {
        LoginResponseDTO response = usuarioService.login(dto, request.getRemoteAddr());
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Login realizado com sucesso!", response));
    }

    @Operation(summary = "Renovar token",
            description = "Troca um refresh token válido por um novo par de tokens. O refresh token enviado é invalidado (rotação); "
                    + "reutilizar um refresh token já usado revoga todas as sessões do usuário.")
    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponseDTO<LoginResponseDTO>> refresh(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ResponseEntity.ok(new SuccessResponseDTO<>(200, "Token renovado com sucesso!", usuarioService.refresh(dto)));
    }

    @Operation(summary = "Logout",
            description = "Revoga o refresh token informado. Sempre responde 204, mesmo se o token for desconhecido. "
                    + "O access token já emitido continua válido até expirar.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        usuarioService.logout(dto);
        return ResponseEntity.noContent().build();
    }
}
