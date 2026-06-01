package com.mygame.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventaire du joueur — jusqu'à 5 objets.
 */
public class Inventory {

    public static final int CAPACITE_MAX = 5;
    private final List<String> objets = new ArrayList<>();

    /** Ajoute un objet si la capacité le permet. */
    public boolean ajouter(String objet) {
        if (objets.size() >= CAPACITE_MAX) return false;
        if (objets.contains(objet)) return false;
        objets.add(objet);
        return true;
    }

    public boolean contient(String objet) {
        return objets.contains(objet);
    }

    public void retirer(String objet) {
        objets.remove(objet);
    }

    public List<String> getObjets() {
        return objets;
    }

    /** Chaîne lisible pour l'affichage HUD. */
    public String toAffichage() {
        if (objets.isEmpty()) return "Inventaire : vide";
        return "Inventaire : " + String.join("  |  ", objets);
    }
}
