package com.mygame.player;

/**
 * Gère la batterie du joueur (temps en secondes).
 *
 * Fusion du code de Lotfi (base temps + game over) et de Malek (sprint/vision nuit).
 *
 * Drain de base : tempsMax secondes pour vider (défaut 600s = 10 min).
 * Sprint  → drain x2.
 * Vision nocturne → drain x3 supplémentaire.
 */
public class BatterieManager {

    private final float tempsMaxInitial;
    private float batterieSec;
    private boolean gameOver = false;

    private boolean enSprint       = false;
    private boolean visionNocturne = false;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Durée personnalisée en secondes. */
    public BatterieManager(float tempsMaxEnSecondes) {
        this.tempsMaxInitial = tempsMaxEnSecondes;
        this.batterieSec     = tempsMaxEnSecondes;
    }

    /** Durée par défaut : 10 minutes. */
    public BatterieManager() {
        this(600f);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(float tpf) {
        if (gameOver) return;

        float drain = 1f; // 1 seconde par seconde de base
        if (enSprint)       drain *= 2f;
        if (visionNocturne) drain *= 3f;

        batterieSec -= drain * tpf;

        if (batterieSec <= 0) {
            batterieSec = 0;
            gameOver = true;
            System.out.println("GAME OVER : La batterie est à plat !");
        }
    }

    // ── Recharge ──────────────────────────────────────────────────────────────

    /** Recharge complète (ramassage d'une pile). */
    public void rechargerAFond() {
        batterieSec = tempsMaxInitial;
        gameOver    = false;
        System.out.println("BATTERIE RECHARGÉE !");
    }

    /** Recharge progressive depuis une station (plein en ~10 secondes). */
    public void rechargerProgressif(float tpf) {
        batterieSec += (tempsMaxInitial / 10f) * tpf;
        if (batterieSec > tempsMaxInitial) batterieSec = tempsMaxInitial;
        if (batterieSec > 0) gameOver = false;
    }

    /** Recharge instantanée partielle (+30% du max). */
    public void recharger() {
        batterieSec = Math.min(tempsMaxInitial, batterieSec + tempsMaxInitial * 0.30f);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Temps restant en secondes (entier). */
    public int getTempsRestant() { return (int) batterieSec; }

    /** Pourcentage restant (0–100) pour le HUD. */
    public float getPourcentage() { return (batterieSec / tempsMaxInitial) * 100f; }

    public boolean isGameOver() { return gameOver; }
    public boolean estVide()    { return gameOver; }

    // ── Setters états ─────────────────────────────────────────────────────────

    public void setSprint(boolean sprint)             { this.enSprint = sprint; }
    public void setVisionNocturne(boolean vn)         { this.visionNocturne = vn; }
}
