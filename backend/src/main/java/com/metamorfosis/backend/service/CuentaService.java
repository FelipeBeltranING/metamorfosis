package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.dto.CuentaResponse;
import com.metamorfosis.backend.dto.EditarCuentaRequest;
import com.metamorfosis.backend.dto.FotoPerfilRequest;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-23: ver y editar los datos de la cuenta (nombre, apellido y foto de perfil).
 * Siempre sobre el usuario autenticado: el código sale del token, nunca de la petición.
 */
@Service
public class CuentaService {

    /** Largo máximo de nombre y apellido al editar. */
    static final int TEXTO_MAXIMO = 50;
    /** Peso máximo de la imagen ya decodificada. El navegador la manda reducida (unos 30 KB). */
    static final int FOTO_MAXIMA_BYTES = 512 * 1024;

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/(jpeg|png|webp);base64,([A-Za-z0-9+/]+={0,2})$");

    private final UsuarioRepository usuarioRepository;

    public CuentaService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public CuentaResponse verCuenta(Integer codigoUsuario) {
        return CuentaResponse.desde(buscar(codigoUsuario));
    }

    /** Cambia nombre y apellido (sin espacios sobrantes). Los dos son obligatorios. */
    @Auditar(accion = AccionLog.EDITAR_PERFIL, entidad = EntidadLog.USUARIO)
    @Transactional
    public CuentaResponse editarDatos(Integer codigoUsuario, EditarCuentaRequest pedido) {
        Usuario usuario = buscar(codigoUsuario);
        List<String> errores = new ArrayList<>();
        String nombre = limpiar(pedido == null ? null : pedido.nombre());
        String apellido = limpiar(pedido == null ? null : pedido.apellido());
        validarTexto(nombre, "nombre", errores);
        validarTexto(apellido, "apellido", errores);
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        return CuentaResponse.desde(usuarioRepository.save(usuario));
    }

    /** Pone o reemplaza la foto. Solo JPG, PNG o WebP de verdad (se revisan los bytes, no solo el tipo). */
    @Auditar(accion = AccionLog.CAMBIAR_FOTO_PERFIL, entidad = EntidadLog.USUARIO)
    @Transactional
    public CuentaResponse cambiarFoto(Integer codigoUsuario, FotoPerfilRequest pedido) {
        Usuario usuario = buscar(codigoUsuario);
        usuario.setFotoPerfil(validarFoto(pedido == null ? null : pedido.foto()));
        return CuentaResponse.desde(usuarioRepository.save(usuario));
    }

    /** Quita la foto; se vuelven a mostrar las iniciales. Sin foto no hace nada. */
    @Auditar(accion = AccionLog.QUITAR_FOTO_PERFIL, entidad = EntidadLog.USUARIO)
    @Transactional
    public CuentaResponse quitarFoto(Integer codigoUsuario) {
        Usuario usuario = buscar(codigoUsuario);
        if (usuario.getFotoPerfil() != null) {
            usuario.setFotoPerfil(null);
            usuario = usuarioRepository.save(usuario);
        }
        return CuentaResponse.desde(usuario);
    }

    private Usuario buscar(Integer codigoUsuario) {
        return usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));
    }

    /** Quita espacios al inicio y al final y deja uno solo entre palabras. */
    static String limpiar(String texto) {
        return texto == null ? null : texto.strip().replaceAll("\\s+", " ");
    }

    private static void validarTexto(String valor, String campo, List<String> errores) {
        if (valor == null || valor.isEmpty()) {
            errores.add("Escribe tu " + campo);
        } else if (valor.length() > TEXTO_MAXIMO) {
            errores.add("El " + campo + " puede tener máximo " + TEXTO_MAXIMO + " caracteres");
        }
    }

    static String validarFoto(String foto) {
        String mensaje = "La foto debe ser una imagen JPG, PNG o WebP";
        if (foto == null || foto.isBlank()) throw new ValidacionException(List.of("Elige una foto"));
        Matcher m = DATA_URL.matcher(foto.strip());
        if (!m.matches()) throw new ValidacionException(List.of(mensaje));

        // La longitud en base64 da el tamaño sin decodificar: se corta antes de reservar memoria.
        if ((long) m.group(2).length() * 3 / 4 > FOTO_MAXIMA_BYTES + 3) {
            throw new ValidacionException(List.of("La foto pesa demasiado; elige una más liviana"));
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(List.of(mensaje));
        }
        if (bytes.length > FOTO_MAXIMA_BYTES) {
            throw new ValidacionException(List.of("La foto pesa demasiado; elige una más liviana"));
        }
        if (!coincideFormato(m.group(1), bytes)) throw new ValidacionException(List.of(mensaje));
        return "data:image/" + m.group(1) + ";base64," + m.group(2);
    }

    /** Firma de los primeros bytes de cada formato, para no guardar algo que no es una imagen. */
    static boolean coincideFormato(String tipo, byte[] b) {
        return switch (tipo) {
            case "jpeg" -> b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "png" -> b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "webp" -> b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
