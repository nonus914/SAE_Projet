package com.mygame.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Gère tous les éléments HUD :
 * - barre de batterie visuelle (bas gauche) — style Lotfi
 * - inventaire texte (bas gauche, au-dessus)
 * - message interaction "[E] Ouvrir…" (centre écran)
 * - message overlay (dialogues, codes)
 * - indicateur vision nocturne
 * - viseur central
 */
public class HudManager {

    private static final float BARRE_LARGEUR = 200f; // pixels largeur max

    private final Node    guiNode;
    private final int     largeurEcran;
    private final int     hauteurEcran;
    private final AssetManager am;

    // Textes
    private BitmapText texteInventaire;
    private BitmapText texteInteraction;
    private BitmapText texteOverlay;
    private BitmapText texteVisionNuit;
    private BitmapText texteCroix;
    private BitmapText texteObjectif;

    // Barre de batterie (géométries)
    private Geometry barreFond;
    private Geometry barreVie;
    private Material matBarre;

    public HudManager(AssetManager am, Node guiNode, int largeur, int hauteur) {
        this.am           = am;
        this.guiNode      = guiNode;
        this.largeurEcran = largeur;
        this.hauteurEcran = hauteur;

        BitmapFont font  = am.loadFont("Interface/Fonts/Default.fnt");
        float taille     = font.getCharSet().getRenderedSize();

        // ── Viseur ────────────────────────────────────────────────────────────
        texteCroix = new BitmapText(font, false);
        texteCroix.setSize(taille * 1.5f);
        texteCroix.setColor(ColorRGBA.White);
        texteCroix.setText("+");
        texteCroix.setLocalTranslation(largeur / 2f - 5, hauteur / 2f + 8, 0);
        guiNode.attachChild(texteCroix);

        // ── Objectif (haut gauche) ────────────────────────────────────────────
        texteObjectif = new BitmapText(font, false);
        texteObjectif.setSize(taille * 1.2f);
        texteObjectif.setColor(ColorRGBA.White);
        texteObjectif.setText("Objectif : Enfuyez-vous du laboratoire");
        texteObjectif.setLocalTranslation(20, hauteur - 20, 0);
        guiNode.attachChild(texteObjectif);

        // ── Inventaire (bas gauche) ───────────────────────────────────────────
        texteInventaire = new BitmapText(font, false);
        texteInventaire.setSize(taille);
        texteInventaire.setColor(ColorRGBA.Cyan);
        texteInventaire.setText("Inventaire : vide");
        texteInventaire.setLocalTranslation(10, 80, 0);
        guiNode.attachChild(texteInventaire);

        // ── Barre de batterie (bas gauche, sous l'inventaire) ─────────────────
        // Fond gris
        Box fondShape = new Box(BARRE_LARGEUR / 2f, 8f, 0f);
        barreFond = new Geometry("BatterieFond", fondShape);
        Material matFond = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matFond.setColor("Color", ColorRGBA.DarkGray);
        barreFond.setMaterial(matFond);
        barreFond.setLocalTranslation(10 + BARRE_LARGEUR / 2f, 50, 0);
        guiNode.attachChild(barreFond);

        // Barre verte (se rétrécit avec la batterie)
        Box barreShape = new Box(BARRE_LARGEUR / 2f, 7f, 0f);
        barreVie = new Geometry("BarreBatterie", barreShape);
        matBarre = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matBarre.setColor("Color", ColorRGBA.Green);
        barreVie.setMaterial(matBarre);
        barreVie.setLocalTranslation(10 + BARRE_LARGEUR / 2f, 50, 1);
        guiNode.attachChild(barreVie);

        // ── Message interaction (centre écran) ────────────────────────────────
        texteInteraction = new BitmapText(font, false);
        texteInteraction.setSize(taille * 1.2f);
        texteInteraction.setColor(ColorRGBA.Yellow);
        texteInteraction.setText("");
        texteInteraction.setLocalTranslation(largeur / 2f - 100, hauteur / 2f - 40, 0);
        guiNode.attachChild(texteInteraction);

        // ── Overlay (dialogues, codes) ────────────────────────────────────────
        texteOverlay = new BitmapText(font, false);
        texteOverlay.setSize(taille);
        texteOverlay.setColor(ColorRGBA.White);
        texteOverlay.setText("");
        texteOverlay.setLocalTranslation(largeur / 2f - 200, hauteur * 0.75f, 0);
        guiNode.attachChild(texteOverlay);

        // ── Vision nocturne ───────────────────────────────────────────────────
        texteVisionNuit = new BitmapText(font, false);
        texteVisionNuit.setSize(taille);
        texteVisionNuit.setColor(ColorRGBA.Green);
        texteVisionNuit.setText("");
        texteVisionNuit.setLocalTranslation(largeur - 160, 60, 0);
        guiNode.attachChild(texteVisionNuit);
    }

    // ── Mise à jour barre batterie ────────────────────────────────────────────

    public void updateBatterie(float pct) {
        // pct : 0-100
        float ratio = Math.max(0f, Math.min(1f, pct / 100f));
        // On réduit l'échelle X de la barre
        barreVie.setLocalScale(ratio, 1f, 1f);
        // Décaler à gauche pour que ça parte du bord gauche
        float offsetX = 10 + BARRE_LARGEUR * ratio / 2f;
        barreVie.setLocalTranslation(offsetX, 50, 1);

        // Couleur : vert > 50%, orange 20-50%, rouge < 20%
        if (pct > 50f)      matBarre.setColor("Color", ColorRGBA.Green);
        else if (pct > 20f) matBarre.setColor("Color", ColorRGBA.Orange);
        else                matBarre.setColor("Color", ColorRGBA.Red);
    }

    // ── Autres méthodes ───────────────────────────────────────────────────────

    public void updateInventaire(String texte) { texteInventaire.setText(texte); }

    public void setMessageInteraction(String msg) { texteInteraction.setText(msg); }

    public void setOverlay(String msg) { texteOverlay.setText(msg); }

    public void setVisionNuit(boolean actif) {
        texteVisionNuit.setText(actif ? "[VISION NUIT ON]" : "");
    }

    public void detacher() {
        guiNode.detachChild(texteCroix);
        guiNode.detachChild(texteObjectif);
        guiNode.detachChild(texteInventaire);
        guiNode.detachChild(barreFond);
        guiNode.detachChild(barreVie);
        guiNode.detachChild(texteInteraction);
        guiNode.detachChild(texteOverlay);
        guiNode.detachChild(texteVisionNuit);
    }
}
