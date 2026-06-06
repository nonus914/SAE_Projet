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

    // Panneau Digicode (enigme 3)
    private Node       panneauDigicode;
    private BitmapText texteDigiSaisie;    // affiche "_ _ _ _" ou "2 7 _ _"
    private BitmapText texteDigiFeedback; // ERREUR / CODE ACCEPTE
    private boolean    digicodeOuvert = false;

    // Overlay charade (vision nocturne room 3)
    private BitmapText texteCharade;

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

        // ── Panneau Digicode ──────────────────────────────────────────────────
        construireDigicode();

        // ── Charade overlay ───────────────────────────────────────────────────
        texteCharade = new BitmapText(font, false);
        texteCharade.setSize(font.getCharSet().getRenderedSize() * 1.05f);
        texteCharade.setColor(new ColorRGBA(0f, 1f, 0f, 1f)); // vert vision nuit
        texteCharade.setText("");
        texteCharade.setLocalTranslation(20, hauteurEcran * 0.82f, 3f);
        guiNode.attachChild(texteCharade);
    }

    // ── Construction panneau Digicode ─────────────────────────────────────────

    private void construireDigicode() {
        // Le node est positionne au coin bas-gauche du panneau
        float dw = 500f, dh = 240f;
        float dx = (largeurEcran - dw) / 2f;
        float dy = (hauteurEcran - dh) / 2f;

        panneauDigicode = new Node("Digicode");
        panneauDigicode.setLocalTranslation(dx, dy, 0); // centrer le node

        // Coordonnees RELATIVES au node (0,0 = bas-gauche du panneau)

        // Fond sombre
        Geometry fond = quad(0, 0, dw, dh, 0.5f,
                new ColorRGBA(0.06f, 0.06f, 0.08f, 0.95f), true);
        panneauDigicode.attachChild(fond);

        // Bordure cyan
        panneauDigicode.attachChild(barre(0,      dh - 2, dw, 2)); // haut
        panneauDigicode.attachChild(barre(0,      0,      dw, 2)); // bas
        panneauDigicode.attachChild(barre(0,      0,      2,  dh)); // gauche
        panneauDigicode.attachChild(barre(dw - 2, 0,      2,  dh)); // droite

        // Titre (fixe, centre a la main)
        BitmapText titre = txt("DIGICODE  -  ENTREZ LE CODE", 1.1f,
                new ColorRGBA(0f, 1f, 1f, 1f));
        titre.setLocalTranslation(dw / 2f - 120, dh - 28, 2f);
        panneauDigicode.attachChild(titre);

        // Saisie — utilise des points comme cases vides (pas de probleme de rendu)
        texteDigiSaisie = txt(". . . .", 2.5f, new ColorRGBA(0f, 1f, 1f, 1f));
        texteDigiSaisie.setLocalTranslation(dw / 2f - 80, dh - 120, 2f);
        panneauDigicode.attachChild(texteDigiSaisie);

        // Feedback
        texteDigiFeedback = txt("", 1.2f, ColorRGBA.White);
        texteDigiFeedback.setLocalTranslation(dw / 2f - 90, 65, 2f);
        panneauDigicode.attachChild(texteDigiFeedback);

        // Hints
        BitmapText hint = txt("0-9 saisir  |  RETOUR effacer  |  ENTREE valider",
                0.85f, new ColorRGBA(0.6f, 0.6f, 0.6f, 1f));
        hint.setLocalTranslation(dw / 2f - 160, 28, 2f);
        panneauDigicode.attachChild(hint);
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

    // ── API Digicode ──────────────────────────────────────────────────────────

    public void ouvrirDigicode() {
        if (!digicodeOuvert) {
            digicodeOuvert = true;
            updateDigicode("");
            texteDigiFeedback.setText("");
            guiNode.attachChild(panneauDigicode);
        }
    }

    public void fermerDigicode() {
        if (digicodeOuvert) {
            digicodeOuvert = false;
            guiNode.detachChild(panneauDigicode);
        }
    }

    public boolean isDigicodeOuvert() { return digicodeOuvert; }

    /** Met a jour l'affichage (ex: "27" -> "2  7  .  ."). */
    public void updateDigicode(String saisie) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append("  ");
            // Chiffre saisi = cyan clair, case vide = point gris
            sb.append(i < saisie.length() ? saisie.charAt(i) : '.');
        }
        texteDigiSaisie.setText(sb.toString());
    }

    public void setFeedbackDigicode(String msg, ColorRGBA couleur) {
        texteDigiFeedback.setText(msg);
        texteDigiFeedback.setColor(couleur);
    }

    // ── Charade (vision nocturne room 3) ──────────────────────────────────────

    public void setCharade(String texte) {
        texteCharade.setText(texte);
        // Re-centrer si texte non vide
        if (!texte.isEmpty()) {
            texteCharade.setLocalTranslation(20, hauteurEcran * 0.80f, 3f);
        }
    }

    // ── Autres ───────────────────────────────────────────────────────────────

    public void updateInventaire(String texte) { /* compatibilite */ }

    public void setMessageInteraction(String msg) { texteInteraction.setText(msg); }

    public void setOverlay(String msg) { /* non utilise */ }

    public void setVisionNuit(boolean actif) {
        if (actif) {
            texteVisionNuit.setText(">> VISION NOCTURNE <<");
            texteVisionNuit.setColor(new ColorRGBA(0f, 1f, 0.2f, 1f));
            texteVisionNuit.setSize(font.getCharSet().getRenderedSize() * 1.1f);
        } else {
            texteVisionNuit.setText("");
        }
    }

    public void detacher() {
        guiNode.detachChild(texteCroix);
        guiNode.detachChild(texteObjectif);
        guiNode.detachChild(texteInteraction);
        guiNode.detachChild(texteVisionNuit);
        guiNode.detachChild(barreFond);
        guiNode.detachChild(barreVie);
        guiNode.detachChild(texteCharade);
        if (inventaireOuvert) guiNode.detachChild(panneauInventaire);
        if (digicodeOuvert)   guiNode.detachChild(panneauDigicode);
    }

    // ── Helpers internes ─────────────────────────────────────────────────────

    private Geometry quad(float x, float y, float w, float h, float z,
                          ColorRGBA col, boolean alpha) {
        Geometry g = new Geometry("q", new Quad(w, h));
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", col);
        if (alpha) mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        g.setMaterial(mat);
        if (alpha) g.setQueueBucket(Bucket.Transparent);
        g.setLocalTranslation(x, y, z);
        return g;
    }

    private Geometry barre(float x, float y, float w, float h) {
        return quad(x, y, w, h, 2f, new ColorRGBA(0f, 1f, 1f, 1f), false);
    }

    private BitmapText txt(String s, float scale, ColorRGBA c) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(font.getCharSet().getRenderedSize() * scale);
        t.setColor(c);
        t.setText(s);
        return t;
    }
}
