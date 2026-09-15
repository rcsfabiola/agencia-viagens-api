package br.com.senai.agenciaviagens.service;

import br.com.senai.agenciaviagens.model.Perfil;
import br.com.senai.agenciaviagens.model.Usuario;
import br.com.senai.agenciaviagens.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        popularUsuariosIniciais();
    }

    private void popularUsuariosIniciais() {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = new Usuario("admin", passwordEncoder.encode("admin123"), Perfil.ADMIN);
        usuarioRepository.save(admin);

        Usuario user = new Usuario("user", passwordEncoder.encode("user123"), Perfil.USER);
        usuarioRepository.save(user);
    }
}