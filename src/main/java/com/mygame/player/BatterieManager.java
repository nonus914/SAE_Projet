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

    /** Recharge depuis une station (instantané +30%). */
    public void recharger() {
        pourcentage = Math.min(100f, pourcentage + CHARGE_STATION);
    }

    /** Recharge complète (ramasse une pile). */
    public void rechargerAFond() {
        pourcentage = 100f;
    }

    /** Recharge progressive depuis une station (appelé chaque frame). */
    public void rechargerProgressif(float tpf) {
        pourcentage = Math.min(100f, pourcentage + 15f * tpf); // +15%/s
    }

    public boolean estVide()    { return pourcentage <= 0f; }

    /** Alias pour compatibilité (isGameOver). */
    public boolean isGameOver() { return estVide(); }

    public float getPourcentage()   { return pourcentage; }

    /** Temps restant estimé en secondes (basé sur le drain de base). */
    public float getTempsRestant() {
        return pourcentage / DRAIN_BASE;
    }

    public void setSprint(boolean sprint) {
        this.enSprint = sprint;
    }

    public void setVisionNocturne(boolean vn) {
        this.visionNocturne = vn;
    }
}
