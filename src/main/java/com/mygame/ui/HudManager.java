package com.mygame.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;

import java.util.List;

/**
 * HUD du jeu LAB-7 :
 *  - Barre de batterie visuelle (bas gauche)
 *  - Message interaction / ramassage (centre)
 *  - Vision nocturne (haut droite)
 *  - Viseur central
 *  - Objectif (haut gauche)
 *  - Panneau SAC A DOS (touche I) — système Noah
 */
public class HudManager {

    private static final float BARRE_LARGEUR = 200f;
    private static final float PANEL_W = 400f;
    private static final float PANEL_H = 300f;

    private final Node         guiNode;
    private final int          largeurEcran;
    private final int          hauteurEcran;
    private final AssetManager am;
    private final BitmapFont   font;

    // Éléments permanents
    private BitmapText texteCroix;
    private BitmapText texteObjectif;
    private BitmapText texteInteraction;
    private BitmapText texteVisionNuit;
    private Geometry   barreFond;
    private Geometry   barreVie;
    private Material   matBarre;

    // Panneau inventaire (SAC A DOS — système Noah)
    private Node       panneauInventaire;
    private BitmapText texteListeInventaire;
    private boolean    inventaireOuvert = false;

    // ─────────────────────────────────────────────────────────────────────────

    public HudManager(AssetManager am, Node guiNode, int largeur, int hauteur) {
        this.am           = am;
        this.guiNode      = guiNode;
        this.largeurEcran = largeur;
        this.hauteurEcran = hauteur;
        this.font         = am.loadFont("Interface/Fonts/Default.fnt");

        float taille = font.getCharSet().getRenderedSize();

        // ── Viseur ────────────────────────────────────────────────────────────
        texteCroix = new BitmapText(font, false);
        texteCroix.setSize(taille * 1.5f);
        texteCroix.setColor(ColorRGBA.White);
        texteCroix.setText("+");
        texteCroix.setLocalTranslation(largeur / 2f - 5, hauteur / 2f + 8, 0);
        guiNode.attachChild(texteCroix);

        // ── Objectif (haut gauche) ────────────────────────────────────────────
        texteObjectif = new BitmapText(font, false);
        texteObjectif.setSize(taille * 1.1f);
        texteObjectif.setColor(ColorRGBA.White);
        texteObjectif.setText("Objectif : Enfuyez-vous du laboratoire");
        texteObjectif.setLocalTranslation(20, hauteur - 20, 0);
        guiNode.attachChild(texteObjectif);

        // ── Message interaction (centre bas) ──────────────────────────────────
        texteInteraction = new BitmapText(font, false);
        texteInteraction.setSize(taille * 1.2f);
        texteInteraction.setColor(ColorRGBA.Yellow);
        texteInteraction.setText("");
        texteInteraction.setLocalTranslation(largeur / 2f - 120, hauteur / 2f - 60, 0);
        guiNode.attachChild(texteInteraction);

        // ── Vision nocturne ───────────────────────────────────────────────────
        texteVisionNuit = new BitmapText(font, false);
        texteVisionNuit.setSize(taille);
        texteVisionNuit.setColor(ColorRGBA.Green);
        texteVisionNuit.setText("");
        texteVisionNuit.setLocalTranslation(largeur - 160, hauteur - 20, 0);
        guiNode.attachChild(texteVisionNuit);

        // ── Barre batterie (bas gauche) ───────────────────────────────────────
        Quad fondBatShape = new Quad(BARRE_LARGEUR, 16f);
        barreFond = new Geometry("BatterieFond", fondBatShape);
        Material matFond = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matFond.setColor("Color", ColorRGBA.DarkGray);
        barreFond.setMaterial(matFond);
        barreFond.setLocalTranslation(10, 30, 0);
        guiNode.attachChild(barreFond);

        Quad barreShape = new Quad(BARRE_LARGEUR, 16f);
        barreVie = new Geometry("BarreBatterie", barreShape);
        matBarre = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matBarre.setColor("Color", ColorRGBA.Green);
        barreVie.setMaterial(matBarre);
        barreVie.setLocalTranslation(10, 30, 1);
        guiNode.attachChild(barreVie);

        // ── Hint inventaire (bas droite) ──────────────────────────────────────
        BitmapText hint = new BitmapText(font, false);
        hint.setSize(taille * 0.9f);
        hint.setColor(new ColorRGBA(0.7f, 0.7f, 0.7f, 1f));
        hint.setText("[I] Inventaire");
        hint.setLocalTranslation(largeur - 120f, 30, 0);
        guiNode.attachChild(hint);

        // ── Panneau SAC A DOS (caché par défaut) ──────────────────────────────
        construirePanneauInventaire();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panneau SAC A DOS (système Noah)
    // ─────────────────────────────────────────────────────────────────────────

    private void construirePanneauInventaire() {
        panneauInventaire = new Node("PanneauInventaire");

        // Fond semi-transparent
        Quad formeFond = new Quad(PANEL_W, PANEL_H);
        Geometry fond = new Geometry("FondInventaire", formeFond);
        Material matFond = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matFond.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.12f, 0.88f));
        matFond.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        fond.setMaterial(matFond);
        fond.setQueueBucket(Bucket.Transparent);
        panneauInventaire.attachChild(fond);

        // Titre orange
        BitmapText titre = new BitmapText(font, false);
        titre.setSize(font.getCharSet().getRenderedSize() * 1.4f);
        titre.setColor(ColorRGBA.Orange);
        titre.setText("=== SAC A DOS ===");
        titre.setLocalTranslation(PANEL_W / 2f - titre.getLineWidth() / 2f, PANEL_H - 20, 1);
        panneauInventaire.attachChild(titre);

        // Contenu (liste items)
        texteListeInventaire = new BitmapText(font, false);
        texteListeInventaire.setSize(font.getCharSet().getRenderedSize() * 1.1f);
        texteListeInventaire.setColor(ColorRGBA.White);
        texteListeInventaire.setText("Votre sac est vide.\nTrouvez des objets !");
        texteListeInventaire.setLocalTranslation(25, PANEL_H - 65, 1);
        panneauInventaire.attachChild(texteListeInventaire);

        // Hint fermeture
        BitmapText hintFermer = new BitmapText(font, false);
        hintFermer.setSize(font.getCharSet().getRenderedSize() * 0.9f);
        hintFermer.setColor(new ColorRGBA(0.6f, 0.6f, 0.6f, 1f));
        hintFermer.setText("[I] Fermer");
        hintFermer.setLocalTranslation(PANEL_W - 85f, 20, 1);
        panneauInventaire.attachChild(hintFermer);

        // Centrer le panneau
        float posX = (largeurEcran - PANEL_W) / 2f;
        float posY = (hauteurEcran - PANEL_H) / 2f;
        panneauInventaire.setLocalTranslation(posX, posY, 0);
    }

    /**
     * Ouvre / ferme le panneau SAC A DOS (touche I).
     * Met à jour la liste avant d'afficher.
     */
    public void toggleInventaire(List<String> items) {
        inventaireOuvert = !inventaireOuvert;
        if (inventaireOuvert) {
            // Mettre à jour la liste
            if (items.isEmpty()) {
                texteListeInventaire.setText("Votre sac est vide.\nTrouvez des objets !");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String item : items) sb.append("- ").append(item).append("\n");
                texteListeInventaire.setText(sb.toString());
            }
            guiNode.attachChild(panneauInventaire);
        } else {
            guiNode.detachChild(panneauInventaire);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Barre batterie
    // ─────────────────────────────────────────────────────────────────────────

    public void updateBatterie(float pct) {
        float ratio = Math.max(0f, Math.min(1f, pct / 100f));
        // Réduire la largeur de la barre (Quad non redimensionnable → scaleX)
        barreVie.setLocalScale(ratio, 1f, 1f);
        // Ancrer à gauche : l'origine du Quad est en bas-gauche
        // → pas besoin de décaler, le scale réduit vers la droite
        if (pct > 50f)      matBarre.setColor("Color", ColorRGBA.Green);
        else if (pct > 20f) matBarre.setColor("Color", ColorRGBA.Orange);
        else                matBarre.setColor("Color", ColorRGBA.Red);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Autres
    // ─────────────────────────────────────────────────────────────────────────

    public void updateInventaire(String texte) { /* gardé pour compatibilité */ }

    public void setMessageInteraction(String msg) { texteInteraction.setText(msg); }

    public void setOverlay(String msg) { /* pour dialogues futurs */ }

    public void setVisionNuit(boolean actif) {
        texteVisionNuit.setText(actif ? "[VISION NUIT ON]" : "");
    }

    public void detacher() {
        guiNode.detachChild(texteCroix);
        guiNode.detachChild(texteObjectif);
        guiNode.detachChild(texteInteraction);
        guiNode.detachChild(texteVisionNuit);
        guiNode.detachChild(barreFond);
        guiNode.detachChild(barreVie);
        if (inventaireOuvert) guiNode.detachChild(panneauInventaire);
    }
}
