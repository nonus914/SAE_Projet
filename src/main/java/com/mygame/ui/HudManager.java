package com.mygame.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

/**
 * Gère tous les éléments HUD :
 * - barre de batterie (bas gauche)
 * - inventaire (bas centre)
 * - message interaction "Appuyez sur E" (centre écran)
 * - message overlay (dialogues, codes)
 * - icône vision nocturne
 */
public class HudManager {

    private final Node guiNode;
    private final int largeurEcran;
    private final int hauteurEcran;

    private BitmapText texteBatterie;
    private BitmapText texteInventaire;
    private BitmapText texteInteraction;
    private BitmapText texteOverlay;
    private BitmapText texteVisionNuit;
    private BitmapText texteCroix; // viseur

    public HudManager(AssetManager am, Node guiNode, int largeur, int hauteur) {
        this.guiNode = guiNode;
        this.largeurEcran = largeur;
        this.hauteurEcran = hauteur;

        BitmapFont font = am.loadFont("Interface/Fonts/Default.fnt");
        float taille = font.getCharSet().getRenderedSize();

        // Viseur central
        texteCroix = new BitmapText(font, false);
        texteCroix.setSize(taille * 1.5f);
        texteCroix.setColor(ColorRGBA.White);
        texteCroix.setText("+");
        texteCroix.setLocalTranslation(largeur / 2f - 5, hauteur / 2f + 8, 0);
        guiNode.attachChild(texteCroix);

        // Batterie (bas gauche)
        texteBatterie = new BitmapText(font, false);
        texteBatterie.setSize(taille);
        texteBatterie.setColor(ColorRGBA.Green);
        texteBatterie.setLocalTranslation(10, 55, 0);
        guiNode.attachChild(texteBatterie);

        // Inventaire (juste au-dessus de la batterie)
        texteInventaire = new BitmapText(font, false);
        texteInventaire.setSize(taille);
        texteInventaire.setColor(ColorRGBA.Cyan);
        texteInventaire.setText("Inventaire : vide");
        texteInventaire.setLocalTranslation(10, 80, 0);
        guiNode.attachChild(texteInventaire);

        // Message interaction (Appuyez sur E)
        texteInteraction = new BitmapText(font, false);
        texteInteraction.setSize(taille * 1.2f);
        texteInteraction.setColor(ColorRGBA.Yellow);
        texteInteraction.setLocalTranslation(largeur / 2f - 80, hauteur / 2f - 40, 0);
        texteInteraction.setText("");
        guiNode.attachChild(texteInteraction);

        // Overlay (dialogues, codes, indices)
        texteOverlay = new BitmapText(font, false);
        texteOverlay.setSize(taille);
        texteOverlay.setColor(ColorRGBA.White);
        texteOverlay.setLocalTranslation(largeur / 2f - 200, hauteur * 0.75f, 0);
        texteOverlay.setText("");
        guiNode.attachChild(texteOverlay);

        // Vision nocturne
        texteVisionNuit = new BitmapText(font, false);
        texteVisionNuit.setSize(taille);
        texteVisionNuit.setColor(ColorRGBA.Green);
        texteVisionNuit.setLocalTranslation(largeur - 160, 60, 0);
        texteVisionNuit.setText("");
        guiNode.attachChild(texteVisionNuit);
    }

    public void updateBatterie(float pct) {
        String barres = "Batterie: [" + barre(pct) + "] " + (int) pct + "%";
        texteBatterie.setText(barres);
        if (pct > 50) texteBatterie.setColor(ColorRGBA.Green);
        else if (pct > 20) texteBatterie.setColor(ColorRGBA.Orange);
        else texteBatterie.setColor(ColorRGBA.Red);
    }

    private String barre(float pct) {
        int rempli = (int) (pct / 10);
        return "##########".substring(0, rempli) + "..........".substring(rempli);
    }

    public void updateInventaire(String texte) {
        texteInventaire.setText(texte);
    }

    public void setMessageInteraction(String msg) {
        texteInteraction.setText(msg);
    }

    public void setOverlay(String msg) {
        texteOverlay.setText(msg);
    }

    public void setVisionNuit(boolean actif) {
        texteVisionNuit.setText(actif ? "[VISION NUIT ON]" : "");
    }

    public void detacher() {
        guiNode.detachChild(texteCroix);
        guiNode.detachChild(texteBatterie);
        guiNode.detachChild(texteInventaire);
        guiNode.detachChild(texteInteraction);
        guiNode.detachChild(texteOverlay);
        guiNode.detachChild(texteVisionNuit);
    }
}
