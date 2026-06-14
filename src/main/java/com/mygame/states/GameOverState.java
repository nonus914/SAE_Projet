package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Ecran Game Over - style cyber/neon (design Aymen).
 * STATUS: TERMINATED  /  GAME OVER  /  boutons RECOMMENCER + QUITTER
 */
public class GameOverState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private Node uiNode;

    private static final ColorRGBA CYN   = new ColorRGBA(0f, 1f, 1f, 1f);
    private static final ColorRGBA CYN_D = new ColorRGBA(0f, 0.38f, 0.38f, 0.9f);
    private static final ColorRGBA BLANC = new ColorRGBA(0.9f, 0.9f, 0.9f, 1f);

    private float bx, bw, btnH = 60f;
    private float btnY1, btnY2, btnY3;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        if (name.equals("GORejouer")) rejouer();
        if (name.equals("GOMenu"))    retourMenu();
        if (name.equals("GOQuitter")) app.stop();
        if (name.equals("GOClic"))    detecterClic();
    };

    private void detecterClic() {
        com.jme3.math.Vector2f c = app.getInputManager().getCursorPosition();
        if (c.x < bx || c.x > bx + bw) return;
        if (c.y >= btnY1 && c.y <= btnY1 + btnH)      rejouer();
        else if (c.y >= btnY2 && c.y <= btnY2 + btnH) retourMenu();
        else if (c.y >= btnY3 && c.y <= btnY3 + btnH) app.stop();
    }

    @Override
    protected void initialize(Application application) {
        this.app     = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        uiNode = new Node("GameOverUI");
        uiNode.attachChild(fondEcran(W, H, "Interface/bg/menu.png"));

        bw = Math.min(830f, W - 100f);
        bx = (W - bw) / 2f;
        float headerH = 200f;
        float headerY = H * 0.52f;

        // Cadre header
        uiNode.attachChild(boite(bx, headerY, bw, headerH,
                new ColorRGBA(0.05f, 0.07f, 0.09f, 0.88f)));

        BitmapText status = txt(font, "STATUS: TERMINATED", 0.9f, CYN);
        centrer(status, W, headerY + headerH - 28, 3f);
        uiNode.attachChild(status);

        BitmapText titre = txt(font, "GAME OVER", 3.2f, CYN);
        centrer(titre, W, headerY + headerH - 95, 3f);
        uiNode.attachChild(titre);

        // Sous-texte (ASCII - sans accents)
        BitmapText sub = txt(font, "Ce n'est pas grave, retente ta chance.", 1.2f, BLANC);
        centrer(sub, W, headerY - 32, 3f);
        uiNode.attachChild(sub);

        // Boutons (cliquables a la souris)
        float gap = 12f;
        btnY1 = headerY - gap - btnH - 50f;
        btnY2 = btnY1 - gap - btnH;
        btnY3 = btnY2 - gap - btnH;

        uiNode.attachChild(boutonBoite(font, "RECOMMENCER", bx, btnY1, bw, btnH, CYN));
        uiNode.attachChild(boutonBoite(font, "MENU",        bx, btnY2, bw, btnH, CYN));
        uiNode.attachChild(boutonBoite(font, "QUITTER",     bx, btnY3, bw, btnH, CYN));

        guiNode.attachChild(uiNode);

        app.getInputManager().addMapping("GORejouer", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addMapping("GOMenu",    new KeyTrigger(KeyInput.KEY_M));
        app.getInputManager().addMapping("GOQuitter", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("GOClic",
                new com.jme3.input.controls.MouseButtonTrigger(com.jme3.input.MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(actionListener, "GORejouer","GOMenu","GOQuitter","GOClic");

        app.getInputManager().setCursorVisible(true);
        app.getViewPort().setBackgroundColor(new ColorRGBA(0.03f, 0.03f, 0.05f, 1f));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Node boutonBoite(BitmapFont font, String label,
                             float x, float y, float w, float h, ColorRGBA couleur) {
        Node n = new Node("Btn");
        n.attachChild(boite(x, y, w, h, new ColorRGBA(0.07f, 0.07f, 0.09f, 0.92f)));
        BitmapText t = txt(font, label, 1.3f, couleur);
        t.setLocalTranslation(x + w / 2f - t.getLineWidth() / 2f,
                              y + h / 2f + t.getSize() * 0.4f, 3f);
        n.attachChild(t);
        return n;
    }

    private Node boite(float x, float y, float w, float h, ColorRGBA fond) {
        Node n = new Node("box");
        n.attachChild(geo(x, y, w, h, 0.5f, fond, true));
        n.attachChild(barre(x,         y + h - 1, w, 1));
        n.attachChild(barre(x,         y,         w, 1));
        n.attachChild(barre(x,         y,         1, h));
        n.attachChild(barre(x + w - 1, y,         1, h));
        return n;
    }

    private Geometry barre(float x, float y, float w, float h) {
        return geo(x, y, w, h, 2f, CYN, false);
    }

    private Geometry geo(float x, float y, float w, float h,
                         float z, ColorRGBA color, boolean alpha) {
        Geometry g = new Geometry("g", new Quad(w, h));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        if (alpha) mat.getAdditionalRenderState()
                .setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        // Bucket Gui conserve (pas Transparent) → les fonds de boites s'affichent
        g.setLocalTranslation(x, y, z);
        return g;
    }

    private Geometry fondEcran(int w, int h, String path) {
        Geometry bg = new Geometry("BG", new Quad(w, h));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        try { mat.setTexture("ColorMap",
                app.getAssetManager().loadTexture(path));
        } catch (Exception e) {
            mat.setColor("Color", new ColorRGBA(0.04f, 0.04f, 0.06f, 1f));
        }
        bg.setMaterial(mat);
        bg.setLocalTranslation(0, 0, -1);
        return bg;
    }

    private static BitmapText txt(BitmapFont f, String s, float scale, ColorRGBA c) {
        BitmapText t = new BitmapText(f, false);
        t.setSize(f.getCharSet().getRenderedSize() * scale);
        t.setColor(c); t.setText(s); return t;
    }

    private static void centrer(BitmapText t, int W, float y, float z) {
        t.setLocalTranslation(W / 2f - t.getLineWidth() / 2f, y, z);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void rejouer()    { app.getStateManager().detach(this); app.getStateManager().attach(new GameState()); }
    private void retourMenu() { app.getStateManager().detach(this); app.getStateManager().attach(new MenuState()); }

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(uiNode);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{"GORejouer","GOMenu","GOQuitter","GOClic"})
            if (app.getInputManager().hasMapping(m)) app.getInputManager().deleteMapping(m);
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
