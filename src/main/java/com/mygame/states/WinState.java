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
 * Écran de victoire — style cyber/néon (design Aymen).
 *
 *  STATUS: MISSION COMPLETE
 *  ╔═══════════ VICTOIRE ════════════╗
 *  ║  Bien joué !!!                  ║
 *  ╚═════════════════════════════════╝
 *  ▶ RECOMMENCER  [R]
 *  ▶ QUITTER      [ECHAP]
 */
public class WinState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private Node uiNode;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        if (name.equals("WinRejouer")) rejouer();
        if (name.equals("WinMenu"))    retourMenu();
        if (name.equals("WinQuitter")) app.stop();
    };

    @Override
    protected void initialize(Application application) {
        this.app     = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        uiNode = new Node("WinUI");

        // ── Fond victoire ─────────────────────────────────────────────────────
        uiNode.attachChild(CyberUI.fond(app.getAssetManager(), W, H,
                "Interface/bg/victoire.png"));

        // ── Cadre néon ────────────────────────────────────────────────────────
        float fw = 640f, fh = 200f;
        float fx = (W - fw) / 2f;
        float fy = H * 0.52f;
        uiNode.attachChild(CyberUI.cadreNeon(app.getAssetManager(), fx, fy, fw, fh));

        // ── Textes dans le cadre ──────────────────────────────────────────────
        BitmapText status = CyberUI.texte(font, "STATUS: MISSION COMPLETE", 0.85f, CyberUI.CYAN);
        CyberUI.centrerX(status, W, fy + fh - 30, 3f);
        uiNode.attachChild(status);

        BitmapText titre = CyberUI.texte(font, "VICTOIRE", 3.5f, CyberUI.CYAN);
        CyberUI.centrerX(titre, W, fy + fh - 95, 3f);
        uiNode.attachChild(titre);

        // ── Sous-texte ────────────────────────────────────────────────────────
        BitmapText sub = CyberUI.texte(font, "Bien joué !!!", 1.5f, CyberUI.BLANC);
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
        app.getInputManager().addMapping("WinRejouer", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addMapping("WinMenu",    new KeyTrigger(KeyInput.KEY_M));
        app.getInputManager().addMapping("WinQuitter", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, "WinRejouer", "WinMenu", "WinQuitter");

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
        for (String m : new String[]{"WinRejouer","WinMenu","WinQuitter"}) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
