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
    private BitmapText texteDigiIndice;   // mot a decoder / charade
    private boolean    digicodeOuvert = false;

    // Overlay charade (vision nocturne room 3)
    private BitmapText texteCharade;

    // Banniere objectif encadree (centre haut)
    private Node       panneauObjectif;
    private BitmapText texteObjectifEncadre;
    private boolean    objectifVisible = false;

    // Panneau Dialogue NPC (Enigme 2 — Daniel)
    private Node       panneauDialogue;
    private BitmapText texteDialogueSpeaker;
    private BitmapText texteDialogueContenu;
    private boolean    dialogueOuvert = false;

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
        texteObjectif.setText(""); // objectif desormais affiche dans le cadre central
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
        matFond.setColor("Color", new ColorRGBA(0.05f, 0.06f, 0.10f, 1f)); // opaque → texte lisible
        fond.setMaterial(matFond);
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

        // ── Panneau Dialogue NPC (Enigme 2) ───────────────────────────────────
        construireDialogue();

        // ── Charade overlay ───────────────────────────────────────────────────
        texteCharade = new BitmapText(font, false);
        texteCharade.setSize(font.getCharSet().getRenderedSize() * 1.05f);
        texteCharade.setColor(new ColorRGBA(0f, 1f, 0f, 1f)); // vert vision nuit
        texteCharade.setText("");
        texteCharade.setLocalTranslation(20, hauteurEcran * 0.82f, 3f);
        guiNode.attachChild(texteCharade);

        construireObjectif();
    }

    /** Banniere OBJECTIF : cadre sombre borde cyan (centre haut), titre + contenu. */
    private void construireObjectif() {
        float w = largeurEcran * 0.54f, h = 78f;
        float x = (largeurEcran - w) / 2f;
        float y = hauteurEcran - 112f;
        panneauObjectif = new Node("Objectif");
        panneauObjectif.setLocalTranslation(x, y, 6f);
        panneauObjectif.attachChild(quad(0, 0, w, h, 0.5f, new ColorRGBA(0.03f, 0.05f, 0.09f, 1f), false));
        ColorRGBA c = new ColorRGBA(0f, 0.85f, 1f, 1f);
        panneauObjectif.attachChild(quad(0,     h - 3, w, 3, 1f, c, false));
        panneauObjectif.attachChild(quad(0,     0,     w, 3, 1f, c, false));
        panneauObjectif.attachChild(quad(0,     0,     3, h, 1f, c, false));
        panneauObjectif.attachChild(quad(w - 3, 0,     3, h, 1f, c, false));
        // bandeau titre
        panneauObjectif.attachChild(quad(3, h - 28, w - 6, 25, 0.8f, new ColorRGBA(0f, 0.30f, 0.42f, 1f), false));
        BitmapText titre = txt("OBJECTIF", 1.05f, new ColorRGBA(0.6f, 1f, 1f, 1f));
        titre.setLocalTranslation(w / 2f - titre.getLineWidth() / 2f, h - 7, 2f);
        panneauObjectif.attachChild(titre);
        // contenu
        texteObjectifEncadre = txt("", 1.0f, new ColorRGBA(1f, 1f, 0.85f, 1f));
        texteObjectifEncadre.setLocalTranslation(16, h - 40, 2f);
        panneauObjectif.attachChild(texteObjectifEncadre);
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

        // Fond sombre OPAQUE (pour que le texte soit bien lisible)
        Geometry fond = quad(0, 0, dw, dh, 0.5f,
                new ColorRGBA(0.04f, 0.05f, 0.08f, 1f), false);
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

        // Indice (mot a decoder / charade) — defini a l'ouverture
        texteDigiIndice = txt("", 0.95f, new ColorRGBA(1f, 0.85f, 0.4f, 1f));
        texteDigiIndice.setLocalTranslation(dw / 2f - 170, dh - 58, 2f);
        panneauDigicode.attachChild(texteDigiIndice);

        // Saisie — utilise des points comme cases vides (pas de probleme de rendu)
        texteDigiSaisie = txt(". . . .", 2.5f, new ColorRGBA(0f, 1f, 1f, 1f));
        texteDigiSaisie.setLocalTranslation(dw / 2f - 80, dh - 120, 2f);
        panneauDigicode.attachChild(texteDigiSaisie);

        // Feedback
        texteDigiFeedback = txt("", 1.2f, ColorRGBA.White);
        texteDigiFeedback.setLocalTranslation(dw / 2f - 90, 65, 2f);
        panneauDigicode.attachChild(texteDigiFeedback);

        // Hints
        BitmapText hint = txt("0-9 saisir  |  ENTREE valider  |  ECHAP ou BOUGER fermer",
                0.85f, new ColorRGBA(0.6f, 0.6f, 0.6f, 1f));
        hint.setLocalTranslation(dw / 2f - 200, 28, 2f);
        panneauDigicode.attachChild(hint);
    }

    // ── Construction panneau Dialogue NPC ─────────────────────────────────────

    private void construireDialogue() {
        float dw = largeurEcran * 0.72f; // ~72% de la largeur écran
        float dh = 190f;
        float dx = (largeurEcran - dw) / 2f;
        float dy = 80f; // au-dessus de la zone batterie

        panneauDialogue = new Node("Dialogue");
        panneauDialogue.setLocalTranslation(dx, dy, 5f); // z=5 au-dessus du reste du HUD

        // ── Fond entièrement opaque (alpha=FALSE = rendu avant les BitmapText) ──
        // Ne PAS utiliser alpha=true ici : en Bucket.Transparent le z-sort peut
        // placer le fond par-dessus les textes. En opaque il render avant eux.
        panneauDialogue.attachChild(
            quad(0, 0, dw, dh, 0f, new ColorRGBA(0f, 0f, 0f, 1f), false));

        // ── Bandeau nom speaker (fond orange sombre) ──────────────────────────
        float bandeH = 38f;
        panneauDialogue.attachChild(
            quad(0, dh - bandeH, dw, bandeH, 1f, new ColorRGBA(0.6f, 0.25f, 0f, 1f), false));

        // ── Bordure orange vive (4px) ─────────────────────────────────────────
        ColorRGBA orange = new ColorRGBA(1f, 0.55f, 0.08f, 1f);
        panneauDialogue.attachChild(quad(0,      dh - 4, dw, 4,  2f, orange, false)); // haut
        panneauDialogue.attachChild(quad(0,      0,      dw, 4,  2f, orange, false)); // bas
        panneauDialogue.attachChild(quad(0,      0,      4,  dh, 2f, orange, false)); // gauche
        panneauDialogue.attachChild(quad(dw - 4, 0,      4,  dh, 2f, orange, false)); // droite

        // ── Nom du speaker (blanc sur bandeau orange) ─────────────────────────
        texteDialogueSpeaker = txt("", 1.35f, ColorRGBA.White);
        texteDialogueSpeaker.setLocalTranslation(14, dh - bandeH + 10, 3f);
        panneauDialogue.attachChild(texteDialogueSpeaker);

        // ── Texte du dialogue (jaune vif → ultra-lisible sur fond noir) ───────
        texteDialogueContenu = txt("", 1.2f, new ColorRGBA(1f, 1f, 0.75f, 1f));
        texteDialogueContenu.setLocalTranslation(14, dh - bandeH - 18, 3f);
        panneauDialogue.attachChild(texteDialogueContenu);

        // ── Hint continuer ────────────────────────────────────────────────────
        BitmapText hintCont = txt("[E] Continuer", 0.9f,
                new ColorRGBA(0.7f, 0.7f, 0.7f, 1f));
        hintCont.setLocalTranslation(dw - 160f, 14, 3f);
        panneauDialogue.attachChild(hintCont);
    }

    // ── API Dialogue NPC ──────────────────────────────────────────────────────

    /** Ouvre le panneau avec speaker + première ligne de dialogue. */
    public void ouvrirDialogue(String speaker, String ligne) {
        if (!dialogueOuvert) {
            dialogueOuvert = true;
            texteDialogueSpeaker.setText(speaker);
            texteDialogueContenu.setText(ligne);
            guiNode.attachChild(panneauDialogue);
        }
    }

    /** Met à jour le contenu du dialogue (ligne suivante). */
    public void setLigneDialogue(String speaker, String ligne) {
        texteDialogueSpeaker.setText(speaker);
        texteDialogueContenu.setText(ligne);
    }

    public void fermerDialogue() {
        if (dialogueOuvert) {
            dialogueOuvert = false;
            guiNode.detachChild(panneauDialogue);
        }
    }

    public boolean isDialogueOuvert() { return dialogueOuvert; }

    // ─────────────────────────────────────────────────────────────────────────

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

    /** Affiche l'indice (mot a decoder / charade) dans le panneau digicode. */
    public void setIndiceDigicode(String indice) {
        if (texteDigiIndice == null) return;
        texteDigiIndice.setText(indice == null ? "" : indice);
        texteDigiIndice.setLocalTranslation(250f - texteDigiIndice.getLineWidth() / 2f, 240f - 58f, 2f);
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

    /** Banniere objectif encadree (centre haut). Vide = cachee. */
    public void setObjectifEncadre(String msg) {
        if (msg == null || msg.isEmpty()) {
            if (objectifVisible) { guiNode.detachChild(panneauObjectif); objectifVisible = false; }
            return;
        }
        texteObjectifEncadre.setText(msg);
        float w = largeurEcran * 0.54f;
        texteObjectifEncadre.setLocalTranslation(w / 2f - texteObjectifEncadre.getLineWidth() / 2f, 78f - 40f, 2f);
        if (!objectifVisible) { guiNode.attachChild(panneauObjectif); objectifVisible = true; }
    }

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
        if (objectifVisible)  guiNode.detachChild(panneauObjectif);
        if (inventaireOuvert) guiNode.detachChild(panneauInventaire);
        if (digicodeOuvert)   guiNode.detachChild(panneauDigicode);
        if (dialogueOuvert)   guiNode.detachChild(panneauDialogue);
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
