package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.mygame.ui.CyberUI;

/**
 * Écran Game Over — style cyber/néon (design Aymen).
 *
 *  STATUS: TERMINATED
 *  ╔═══════════ GAME OVER ═══════════╗
 *  ║  Ce n'est pas grave…            ║
 *  ╚═════════════════════════════════╝
 *  ▶ RECOMMENCER  [R]
 *  ▶ QUITTER      [ECHAP]
 */
public class GameOverState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private Node uiNode;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        if (name.equals("GORejouer")) rejouer();
        if (name.equals("GOQuitter")) app.stop();
        if (name.equals("GOMenu"))    retourMenu();
    };

    @Override
    protected void initialize(Application application) {
        this.app     = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        uiNode = new Node("GameOverUI");

        // ── Fond ──────────────────────────────────────────────────────────────
        uiNode.attachChild(CyberUI.fond(app.getAssetManager(), W, H,
                "Interface/bg/menu.png")); // même fond sombre que le menu

        // ── Cadre néon ────────────────────────────────────────────────────────
        float fw = 640f, fh = 200f;
        float fx = (W - fw) / 2f;
        float fy = H * 0.52f;
        uiNode.attachChild(CyberUI.cadreNeon(app.getAssetManager(), fx, fy, fw, fh));

        // ── Textes dans le cadre ──────────────────────────────────────────────
        BitmapText status = CyberUI.texte(font, "STATUS: TERMINATED", 0.85f, CyberUI.CYAN);
        CyberUI.centrerX(status, W, fy + fh - 30, 3f);
        uiNode.attachChild(status);

        BitmapText titre = CyberUI.texte(font, "GAME OVER", 3.2f, CyberUI.CYAN);
        CyberUI.centrerX(titre, W, fy + fh - 90, 3f);
        uiNode.attachChild(titre);

        // ── Sous-texte ────────────────────────────────────────────────────────
        BitmapText sub = CyberUI.texte(font,
            "Ce n'est pas grave, retente ta chance.", 1.3f, CyberUI.BLANC);
        CyberUI.centrerX(sub, W, fy - 35, 3f);
        uiNode.attachChild(sub);

        // ── Boutons ───────────────────────────────────────────────────────────
        BitmapText btnRetry = CyberUI.texte(font, "▶  RECOMMENCER   [R]", 1.3f, CyberUI.CYAN);
        CyberUI.centrerX(btnRetry, W, fy - 85, 3f);
        uiNode.attachChild(btnRetry);

        BitmapText btnMenu = CyberUI.texte(font, "▶  MENU   [M]", 1.1f, CyberUI.BLANC);
        CyberUI.centrerX(btnMenu, W, fy - 125, 3f);
        uiNode.attachChild(btnMenu);

        BitmapText btnQuit = CyberUI.texte(font, "▶  QUITTER   [ECHAP]", 1.1f, CyberUI.GRIS);
        CyberUI.centrerX(btnQuit, W, fy - 160, 3f);
        uiNode.attachChild(btnQuit);

        guiNode.attachChild(uiNode);

        // ── Touches ───────────────────────────────────────────────────────────
        app.getInputManager().addMapping("GORejouer", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addMapping("GOMenu",    new KeyTrigger(KeyInput.KEY_M));
        app.getInputManager().addMapping("GOQuitter", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, "GORejouer", "GOMenu", "GOQuitter");

        app.getInputManager().setCursorVisible(true);
        app.getViewPort().setBackgroundColor(
            new com.jme3.math.ColorRGBA(0.03f, 0.03f, 0.05f, 1f));
    }

    private void rejouer() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new GameState());
    }

    private void retourMenu() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new MenuState());
    }

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(uiNode);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{"GORejouer","GOMenu","GOQuitter"}) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
