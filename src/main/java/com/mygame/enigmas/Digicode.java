package com.mygame.enigmas;

/**
 * Énigme digicode — le joueur entre un code pour déverrouiller une porte.
 *
 * Fusion du code de Lotfi (3 tentatives, game-over) et de Malek (saisie chiffre par chiffre).
 *
 * Usage :
 *   Digicode d = new Digicode("1234");
 *   d.ajouterChiffre("1");  // saisie progressive
 *   boolean ok = d.testerCode("1234"); // test direct
 */
public class Digicode {

    private final String codeSecret;
    private int  tentativesRestantes;
    private boolean estDeverrouille = false;
    private String saisieCourante  = "";

    public Digicode(String codeSecret) {
        this(codeSecret, 3);
    }

    public Digicode(String codeSecret, int maxTentatives) {
        this.codeSecret          = codeSecret;
        this.tentativesRestantes = maxTentatives;
    }

    // ── Saisie chiffre par chiffre (clavier en jeu) ───────────────────────────

    /**
     * Ajoute un chiffre à la saisie en cours.
     * Teste automatiquement quand la longueur du code est atteinte.
     */
    public boolean ajouterChiffre(String chiffre) {
        if (estDeverrouille || tentativesRestantes <= 0) return false;
        saisieCourante += chiffre;
        if (saisieCourante.length() >= codeSecret.length()) {
            return testerCode(saisieCourante);
        }
        return false;
    }

    /** Efface la saisie en cours. */
    public void effacer() { saisieCourante = ""; }

    // ── Test direct ───────────────────────────────────────────────────────────

    /**
     * Teste si le code fourni est correct.
     * @return true si le code est bon
     */
    public boolean testerCode(String saisie) {
        saisieCourante = "";

        if (estDeverrouille || tentativesRestantes <= 0) return false;

        if (codeSecret.equals(saisie)) {
            estDeverrouille = true;
            System.out.println("[Digicode] SUCCÈS : Code correct, porte déverrouillée !");
            return true;
        } else {
            tentativesRestantes--;
            System.out.println("[Digicode] ERREUR : Code incorrect. Tentatives restantes : "
                    + tentativesRestantes);
            if (tentativesRestantes <= 0) {
                System.out.println("[Digicode] ALARME : Digicode bloqué !");
            }
            return false;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isDeverrouille()    { return estDeverrouille; }
    public boolean isResolu()          { return estDeverrouille; }
    public boolean isBloque()          { return tentativesRestantes <= 0 && !estDeverrouille; }
    public int     getTentativesRestantes() { return tentativesRestantes; }
    public String  getSaisieCourante() { return saisieCourante; }
    public int     getLongueurCode()   { return codeSecret.length(); }
}
