package com.mygame.enigmas;

/**
 * Énigme digicode — le joueur doit entrer le bon code pour ouvrir une porte.
 * Usage :
 *   Digicode d = new Digicode("1234");
 *   boolean ok = d.testerCode("1234"); // true
 */
public class Digicode {

    private final String codeSecret;
    private String saisieCourante = "";
    private boolean resolu = false;

    public Digicode(String codeSecret) {
        this.codeSecret = codeSecret;
    }

    /** Ajoute un chiffre à la saisie en cours. */
    public void ajouterChiffre(String chiffre) {
        if (resolu) return;
        saisieCourante += chiffre;
        // Teste automatiquement quand la longueur est atteinte
        if (saisieCourante.length() >= codeSecret.length()) {
            testerCode(saisieCourante);
        }
    }

    /**
     * Teste si le code fourni correspond au code secret.
     * @return true si correct
     */
    public boolean testerCode(String code) {
        if (codeSecret.equals(code)) {
            resolu = true;
            saisieCourante = "";
            System.out.println("[Digicode] Code correct ! Porte déverrouillée.");
            return true;
        } else {
            System.out.println("[Digicode] Code incorrect : " + code);
            saisieCourante = "";
            return false;
        }
    }

    /** Remet la saisie à zéro sans réinitialiser la résolution. */
    public void effacer() {
        saisieCourante = "";
    }

    public boolean isResolu()          { return resolu; }
    public String  getSaisieCourante() { return saisieCourante; }
    public int     getLongueurCode()   { return codeSecret.length(); }
}
