package com.mygame.npc;

import com.mygame.ui.HudManager;

/**
 * Gère l'affichage séquentiel des dialogues via le HUD.
 * Chaque appel à avancer() passe à la réplique suivante.
 */
public class DialogueManager {

    private final HudManager hud;
    private String[] lignes;
    private int index = 0;
    private boolean actif = false;

    public DialogueManager(HudManager hud) {
        this.hud = hud;
    }

    /** Démarre un dialogue avec les lignes données. */
    public void demarrer(String[] lignes) {
        this.lignes = lignes;
        this.index = 0;
        this.actif = true;
        afficherLigne();
    }

    /** Passe à la ligne suivante. Ferme le dialogue si terminé. */
    public void avancer() {
        if (!actif) return;
        index++;
        if (index >= lignes.length) {
            fermer();
        } else {
            afficherLigne();
        }
    }

    private void afficherLigne() {
        hud.setOverlay(lignes[index]);
    }

    public void fermer() {
        actif = false;
        hud.setOverlay("");
    }

    public boolean estActif() {
        return actif;
    }
}
