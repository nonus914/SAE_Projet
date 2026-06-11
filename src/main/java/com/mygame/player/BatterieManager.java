package com.mygame.player;

/**
 * Gère la batterie du joueur (temps en secondes).
 * Code original de Lotfi — conservé tel quel.
 */
public class BatterieManager {

    private float batterieSec;
    private float tempsMaxInitial;
    private boolean gameOver;
    private boolean enSprint = false; // x2 drain en sprint

    public BatterieManager(float tempsMaxEnSecondes) {
        this.tempsMaxInitial = tempsMaxEnSecondes;
        this.batterieSec     = tempsMaxEnSecondes;
        this.gameOver        = false;
    }

    public void setSprint(boolean sprint) { this.enSprint = sprint; }

    public void update(float tpf) {
        if (gameOver) return;
        float drain = enSprint ? tpf * 2f : tpf; // sprint = x2
        batterieSec -= drain;
        if (batterieSec <= 0) {
            batterieSec = 0;
            gameOver = true;
            System.out.println("GAME OVER : La batterie est à plat !");
        }
    }

    public int getTempsRestant() { return (int) batterieSec; }

    public boolean isGameOver()  { return gameOver; }

    public void rechargerAFond() {
        this.batterieSec = tempsMaxInitial;
        this.gameOver    = false;
        System.out.println("BATTERIE RECHARGÉE !");
    }

    /** Vide la batterie plus vite (ex : course de fuite finale). */
    public void drainer(float secondes) {
        if (gameOver) return;
        batterieSec -= secondes;
        if (batterieSec <= 0) { batterieSec = 0; gameOver = true; }
    }

    public void rechargerProgressif(float tpf) {
        float gainParSeconde = tempsMaxInitial / 10f;
        batterieSec += gainParSeconde * tpf;
        if (batterieSec > tempsMaxInitial) batterieSec = tempsMaxInitial;
        if (batterieSec > 0) gameOver = false;
    }

    /** Pourcentage 0–100 pour la barre visuelle HUD. */
    public float getPourcentage() {
        return (batterieSec / tempsMaxInitial) * 100f;
    }
}
