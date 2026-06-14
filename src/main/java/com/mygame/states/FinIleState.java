package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/**
 * Epilogue en 3 temps :
 *   0) ecran texte (suspense, fond noir, titre qui grandit)
 *   1) revelation de l'ile (Vue 1) — "le Sujet 27 est libre" / A SUIVRE
 *   2) menu (recommencer / menu / quitter)
 * Deux variantes selon le choix de l'enigme 4 (repare = preuves / sabote = silence).
 */
public class FinIleState extends BaseAppState {

    private final boolean repare;
    private SimpleApplication app;
    private Node guiNode;
    private Node mondeNode, texteNode, ileUiNode, menuNode;
    private Spatial ile;
    private BitmapFont font;
    private int W, H;

    private int   phase  = 0;   // 0 texte · 1 ile · 2 menu
    private float phaseT = 0f;
    private BitmapText grandTitre;

    private float bx, bw;
    private static final float BTN_H = 56f;
    private float btnY1, btnY2, btnY3;

    private static final ColorRGBA CYN   = new ColorRGBA(0f, 1f, 1f, 1f);
    private static final ColorRGBA ROUGE = new ColorRGBA(1f, 0.25f, 0.2f, 1f);
    private static final ColorRGBA OR    = new ColorRGBA(1f, 0.85f, 0.2f, 1f);
    private static final ColorRGBA BLANC = new ColorRGBA(0.95f, 0.95f, 0.95f, 1f);
    private static final ColorRGBA GRIS  = new ColorRGBA(0.7f, 0.7f, 0.75f, 1f);

    public FinIleState(boolean repare) { this.repare = repare; }

    private final ActionListener act = (name, p, tpf) -> {
        if (!p) return;
        switch (name) {
            case "FinRejouer" -> aller(new GameState());
            case "FinMenu"    -> aller(new MenuState());
            case "FinQuitter" -> app.stop();
            case "FinSuite"   -> avancer();
            case "FinClic"    -> { if (phase < 2) avancer(); else clicMenu(); }
        }
    };

    private void avancer() {
        if (phase == 0) passerIle();
        else if (phase == 1) passerMenu();
    }

    private void aller(BaseAppState s) {
        app.getStateManager().detach(this);
        app.getStateManager().attach(s);
    }

    private void clicMenu() {
        Vector2f c = app.getInputManager().getCursorPosition();
        if (c.x < bx || c.x > bx + bw) return;
        if (c.y >= btnY1 && c.y <= btnY1 + BTN_H)      aller(new GameState());
        else if (c.y >= btnY2 && c.y <= btnY2 + BTN_H) aller(new MenuState());
        else if (c.y >= btnY3 && c.y <= btnY3 + BTN_H) app.stop();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void initialize(Application application) {
        this.app     = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();
        this.W = app.getCamera().getWidth();
        this.H = app.getCamera().getHeight();
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(true);

        // ── Ile (Capri) — modele geo-reference : on le RECENTRE a l'origine ──
        mondeNode = new Node("FinMonde");
        try {
            ile = app.getAssetManager().loadModel("Models/props/island_capri.glb");
            mondeNode.attachChild(ile);
            mondeNode.updateGeometricState();
            if (ile.getWorldBound() instanceof BoundingBox) {
                BoundingBox bb = (BoundingBox) ile.getWorldBound();
                Vector3f c = bb.getCenter();
                float ext = Math.max(bb.getXExtent(), bb.getZExtent());
                // Capri est geo-reference (coords ~millions) : la mise a l'echelle
                // d'abord rend la recentrage precise (pas de tremblement flottant).
                float scale = 150f / Math.max(1f, ext);     // ile large (~300 m)
                float baseY = c.y - bb.getYExtent();
                ile.setLocalScale(scale);
                ile.setLocalTranslation(-c.x * scale, -baseY * scale, -c.z * scale); // base au sol Y=0
            }
        } catch (Exception e) { ile = null; }
        AmbientLight amb = new AmbientLight();
        amb.setColor(ColorRGBA.White.mult(1.35f));
        mondeNode.addLight(amb);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.4f, -0.7f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.5f));
        mondeNode.addLight(sun);

        // ── Phase 0 : ecran texte (fond noir) ───────────────────────────────
        app.getViewPort().setBackgroundColor(ColorRGBA.Black);
        texteNode = new Node("FinTexte");
        BitmapText l1 = txt(repare ? "TU AS GARDE LA LUMIERE ALLUMEE..."
                                   : "TU AS TOUT FAIT SAUTER...", 1.2f, BLANC);
        centrer(l1, H * 0.66f); texteNode.attachChild(l1);

        grandTitre = txt(repare ? "LA VERITE ECLATERA" : "LE LABO N'EXISTE PLUS", 1.0f,
                         repare ? CYN : ROUGE);
        centrer(grandTitre, H * 0.52f); texteNode.attachChild(grandTitre);

        BitmapText l3 = txt(repare ? "Les dossiers classifies sont entre tes mains."
                                   : "Ses preuves, ses secrets, ses victimes... disparus.", 0.95f, GRIS);
        centrer(l3, H * 0.40f); texteNode.attachChild(l3);

        BitmapText hint = txt("[ Clic ou ESPACE pour continuer ]", 0.8f, GRIS);
        centrer(hint, 44); texteNode.attachChild(hint);
        guiNode.attachChild(texteNode);

        // ── Entrees ──────────────────────────────────────────────────────────
        app.getInputManager().addMapping("FinRejouer", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addMapping("FinMenu",    new KeyTrigger(KeyInput.KEY_M));
        app.getInputManager().addMapping("FinQuitter", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("FinSuite",   new KeyTrigger(KeyInput.KEY_SPACE),
                                                       new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("FinClic",    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(act,
                "FinRejouer","FinMenu","FinQuitter","FinSuite","FinClic");
    }

    private void passerIle() {
        phase = 1; phaseT = 0f;
        guiNode.detachChild(texteNode);
        app.getViewPort().setBackgroundColor(new ColorRGBA(0.45f, 0.68f, 0.88f, 1f)); // ciel
        app.getRootNode().attachChild(mondeNode);
        // VUE 1 : camera posee en hauteur SUR l'ile, regard libre a la souris
        app.getCamera().setLocation(new Vector3f(0f, 24f, 95f));
        app.getCamera().lookAt(new Vector3f(0f, 10f, -30f), Vector3f.UNIT_Y);
        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setMoveSpeed(0f);
        app.getFlyByCamera().setRotationSpeed(1.6f);
        app.getInputManager().setCursorVisible(false);

        ileUiNode = new Node("FinIleUI");
        // Bandes sombres pour rendre le texte lisible sur le ciel clair
        ileUiNode.attachChild(geo(0, H - 96, W, 56, 1f, new ColorRGBA(0f, 0f, 0f, 0.5f), true));
        ileUiNode.attachChild(geo(0, 26,    W, 98, 1f, new ColorRGBA(0f, 0f, 0f, 0.5f), true));
        BitmapText libre = txt("LE SUJET 27 EST LIBRE.", 1.3f, BLANC);
        centrer(libre, H - 60); ileUiNode.attachChild(libre);
        BitmapText suite = txt("SAE  GENIE  LOGICIEL", 1.6f, OR);
        centrer(suite, 80); ileUiNode.attachChild(suite);
        BitmapText hint = txt("[ Clic ou ESPACE ]", 0.8f, new ColorRGBA(1f, 1f, 1f, 0.9f));
        centrer(hint, 44); ileUiNode.attachChild(hint);
        guiNode.attachChild(ileUiNode);
    }

    private void passerMenu() {
        phase = 2; phaseT = 0f;
        app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(true);
        if (ileUiNode != null) guiNode.detachChild(ileUiNode);
        menuNode = new Node("FinMenuUI");
        bw = Math.min(640f, W - 120f);
        bx = (W - bw) / 2f;

        BitmapText suite = txt("SAE  GENIE  LOGICIEL", 1.5f, OR);
        centrer(suite, H - 70); menuNode.attachChild(suite);

        btnY1 = H * 0.44f;
        btnY2 = btnY1 - 14f - BTN_H;
        btnY3 = btnY2 - 14f - BTN_H;
        menuNode.attachChild(bouton("RECOMMENCER", bx, btnY1));
        menuNode.attachChild(bouton("MENU",        bx, btnY2));
        menuNode.attachChild(bouton("QUITTER",     bx, btnY3));
        guiNode.attachChild(menuNode);
    }

    @Override
    public void update(float tpf) {
        phaseT += tpf;
        if (phase == 0) {
            if (grandTitre != null) { // le titre GRANDIT (suspense)
                grandTitre.setSize(font.getCharSet().getRenderedSize() * (1.0f + phaseT * 0.30f));
                centrer(grandTitre, H * 0.52f);
            }
            if (phaseT > 6.5f) passerIle();
        } else {
            // Vue 1 : l'ile reste FIXE (on regarde autour a la souris)
            if (phase == 1 && phaseT > 14f) passerMenu();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Node bouton(String label, float x, float y) {
        Node n = new Node("Btn");
        n.attachChild(boite(x, y, bw, BTN_H, new ColorRGBA(0.04f, 0.06f, 0.09f, 0.9f)));
        BitmapText t = txt(label, 1.1f, CYN);
        t.setLocalTranslation(x + bw / 2f - t.getLineWidth() / 2f,
                              y + BTN_H / 2f + t.getSize() * 0.4f, 3f);
        n.attachChild(t);
        return n;
    }

    private Node boite(float x, float y, float w, float h, ColorRGBA fond) {
        Node n = new Node("box");
        n.attachChild(geo(x, y, w, h, 0.5f, fond, true));
        n.attachChild(geo(x,         y + h - 1, w, 1, 2f, CYN, false));
        n.attachChild(geo(x,         y,         w, 1, 2f, CYN, false));
        n.attachChild(geo(x,         y,         1, h, 2f, CYN, false));
        n.attachChild(geo(x + w - 1, y,         1, h, 2f, CYN, false));
        return n;
    }

    private Geometry geo(float x, float y, float w, float h, float z, ColorRGBA c, boolean alpha) {
        Geometry g = new Geometry("g", new Quad(w, h));
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", c);
        if (alpha) m.getAdditionalRenderState().setBlendMode(
                com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(m);
        g.setLocalTranslation(x, y, z);
        return g;
    }

    private BitmapText txt(String s, float scale, ColorRGBA c) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(font.getCharSet().getRenderedSize() * scale);
        t.setColor(c);
        t.setText(s);
        return t;
    }

    private void centrer(BitmapText t, float y) {
        t.setLocalTranslation(W / 2f - t.getLineWidth() / 2f, y, 3f);
    }

    @Override
    protected void cleanup(Application application) {
        if (texteNode != null) guiNode.detachChild(texteNode);
        if (ileUiNode != null) guiNode.detachChild(ileUiNode);
        if (menuNode  != null) guiNode.detachChild(menuNode);
        if (mondeNode != null) app.getRootNode().detachChild(mondeNode);
        app.getInputManager().removeListener(act);
        for (String m : new String[]{"FinRejouer","FinMenu","FinQuitter","FinSuite","FinClic"})
            if (app.getInputManager().hasMapping(m)) app.getInputManager().deleteMapping(m);
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
