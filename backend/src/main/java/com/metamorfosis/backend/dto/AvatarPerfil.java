package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Avatar;

/**
 * HU20: las piezas del avatar. Se usa para pedir el cambio y para devolverlo en la cuenta y en el
 * perfil. {@code accesorio} es opcional (null es sin accesorio).
 */
public record AvatarPerfil(String piel, String peinado, String colorPelo, String ojos, String boca, String accesorio) {

    public static AvatarPerfil desde(Avatar avatar) {
        Avatar a = avatar == null ? new Avatar() : avatar;
        return new AvatarPerfil(a.getPiel(), a.getPeinado(), a.getColorPelo(), a.getOjos(), a.getBoca(), a.getAccesorio());
    }
}
