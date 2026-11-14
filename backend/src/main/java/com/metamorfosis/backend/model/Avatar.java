package com.metamorfosis.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * HU20: el avatar con el que la persona se muestra sin poner su foto. Son las piezas que eligió en
 * el editor (tono de piel, peinado, color de pelo, ojos, boca y un accesorio opcional); el dibujo
 * lo arma el frontend con el estilo «Big Smile» de DiceBear. Los valores válidos están en
 * {@link CatalogoAvatar}. Todo usuario nuevo empieza con el avatar por defecto.
 */
@Embeddable
public class Avatar {

    @Column(name = "avatar_piel", nullable = false, length = 6)
    private String piel = CatalogoAvatar.PIEL_INICIAL;

    @Column(name = "avatar_peinado", nullable = false, length = 20)
    private String peinado = CatalogoAvatar.PEINADO_INICIAL;

    @Column(name = "avatar_color_pelo", nullable = false, length = 6)
    private String colorPelo = CatalogoAvatar.COLOR_PELO_INICIAL;

    @Column(name = "avatar_ojos", nullable = false, length = 12)
    private String ojos = CatalogoAvatar.OJOS_INICIALES;

    @Column(name = "avatar_boca", nullable = false, length = 14)
    private String boca = CatalogoAvatar.BOCA_INICIAL;

    /** Opcional: null es sin accesorio. */
    @Column(name = "avatar_accesorio", length = 16)
    private String accesorio;

    public Avatar() { }

    public Avatar(String piel, String peinado, String colorPelo, String ojos, String boca, String accesorio) {
        this.piel = piel;
        this.peinado = peinado;
        this.colorPelo = colorPelo;
        this.ojos = ojos;
        this.boca = boca;
        this.accesorio = accesorio;
    }

    public String getPiel() { return piel; }
    public String getPeinado() { return peinado; }
    public String getColorPelo() { return colorPelo; }
    public String getOjos() { return ojos; }
    public String getBoca() { return boca; }
    public String getAccesorio() { return accesorio; }
}
