package com.mygame.player;

/**
 * Gère la batterie du cyborg.
 * Vidage passif sur 10 minutes, x2 en sprint, x3 en vision nocturne.
 */
public class BatterieManager {

    // 10 minutes = 600 secondes → drain de base 100/600
    private static final float DRAIN_BASE = 100f / 600f;
    private static final float MULT_SPRINT = 2f;
    private static final float MULT_NIGHT = 3f;
    private static final float CHARGE_STATION = 30f; // % rechargé par station

    private float pourcentage = 100f;
    private boolean enSprint = false;
    private boolean visionNocturne = false;

    public void update(float tpf) {
        float drain = DRAIN_BASE;
        if (enSprint) drain *= MULT_SPRINT;
        if (visionNocturne) drain *= MULT_NIGHT;
        pourcentage = Math.max(0f, pourcentage - drain * tpf);
    }

    /** Recharge depuis une station. */
    public void recharger() {
        pourcentage = Math.min(100f, pourcentage + CHARGE_STATION);
    }

    public boolean estVide() {
        return pourcentage <= 0f;
    }

    public float getPourcentage() {
        return pourcentage;
    }

    public void setSprint(boolean sprint) {
        this.enSprint = sprint;
    }

    public void setVisionNocturne(boolean vn) {
        this.visionNocturne = vn;
    }
}
