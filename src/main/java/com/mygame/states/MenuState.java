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
 * Menu principal — style cyber/néon (d'après le design d'Aymen).
 *
 *  SUBJECT: 27  /  SÉQUENCE D'ÉVEIL
 *  ─────────────────────────────────
 *  ▶ JOUER          [ENTRÉE]
 *  ▶ COMMANDES      [C]
 *  ▶ QUITTER        [ECHAP]
 */
public class MenuState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private Node uiNode;   // tous les éléments GUI regroupés

    private boolean commandesVisible = false;
    private Node    panneauCommandes;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        switch (name) {
            case "MenuJouer"    -> lancerJeu();
            case "MenuQuitter"  -> app.stop();
            case "MenuCommandes"-> toggleCommandes();
        }
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void initialize(Application application) {
        this.app     = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        BitmapFont font  = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        float base       = font.getCharSet().getRenderedSize();

        uiNode = new Node("MenuUI");

        // ── Fond ──────────────────────────────────────────────────────────────
        uiNode.attachChild(CyberUI.fond(app.getAssetManager(), W, H,
                "Interface/bg/menu.png"));

        // ── Cadre néon principal ──────────────────────────────────────────────
        float fw = 660f, fh = 180f;
        float fx = (W - fw) / 2f;
        float fy = H * 0.55f;
        uiNode.attachChild(CyberUI.cadreNeon(app.getAssetManager(), fx, fy, fw, fh));

        // ── Textes dans le cadre ──────────────────────────────────────────────
        BitmapText subject = CyberUI.texte(font, "SUBJECT: 27", 1.0f, CyberUI.CYAN);
        CyberUI.centrerX(subject, W, fy + fh - 35, 3f);
        uiNode.attachChild(subject);

        BitmapText sequence = CyberUI.texte(font,
            "S É Q U E N C E   D ' É V E I L   -   S U J E T   2 7",
            1.2f, CyberUI.BLANC);
        CyberUI.centrerX(sequence, W, fy + fh - 75, 3f);
        uiNode.attachChild(sequence);

        BitmapText sep = CyberUI.texte(font,
            "──────────────────────────────────────────────────────",
            0.8f, CyberUI.CYAN_DARK);
        CyberUI.centrerX(sep, W, fy + fh - 120, 3f);
        uiNode.attachChild(sep);

        BitmapText techL = CyberUI.texte(font, "STBL_INIT_0.94", 0.8f, CyberUI.CYAN_DARK);
        techL.setLocalTranslation(fx + 15, fy + 15, 3f);
        uiNode.attachChild(techL);

        BitmapText techR = CyberUI.texte(font, "LOC: SEC_B_WNG_04", 0.8f, CyberUI.CYAN_DARK);
        techR.setLocalTranslation(fx + fw - 160, fy + 15, 3f);
        uiNode.attachChild(techR);

        // ── Boutons ───────────────────────────────────────────────────────────
        float btnY = fy - 55;
        uiNode.attachChild(bouton(font, "▶  JOUER",      "[ENTRÉE]", W, btnY,        CyberUI.CYAN));
        uiNode.attachChild(bouton(font, "▶  COMMANDES",  "[C]",      W, btnY - 45,   CyberUI.BLANC));
        uiNode.attachChild(bouton(font, "▶  QUITTER",    "[ECHAP]",  W, btnY - 90,   CyberUI.GRIS));

        guiNode.attachChild(uiNode);

        // ── Panneau commandes (caché) ─────────────────────────────────────────
        construirePanneauCommandes(font, W, H);

        // ── Touches ───────────────────────────────────────────────────────────
        app.getInputManager().addMapping("MenuJouer",    new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("MenuQuitter",  new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("MenuCommandes",new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addListener(actionListener,
            "MenuJouer", "MenuQuitter", "MenuCommandes");

        app.getInputManager().setCursorVisible(true);
        app.getViewPort().setBackgroundColor(
            new com.jme3.math.ColorRGBA(0.03f, 0.03f, 0.05f, 1f));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private BitmapText bouton(BitmapFont font, String label, String touche,
                              int W, float y, com.jme3.math.ColorRGBA couleur) {
        BitmapText t = CyberUI.texte(font, label + "   " + touche, 1.3f, couleur);
        CyberUI.centrerX(t, W, y, 3f);
        return t;
    }

    private void construirePanneauCommandes(BitmapFont font, int W, int H) {
        panneauCommandes = new Node("Commandes");
        float pw = 500f, ph = 320f;
        float px = (W - pw) / 2f, py = (H - ph) / 2f;
        panneauCommandes.attachChild(
            CyberUI.cadreNeon(app.getAssetManager(), px, py, pw, ph));

        String[][] cmds = {
            {"W / Z",    "Avancer"},
            {"S",        "Reculer"},
            {"A / Q",    "Gauche"},
            {"D",        "Droite"},
            {"ESPACE",   "Sauter"},
            {"SHIFT",    "Sprint"},
            {"C",        "S'accroupir"},
            {"E",        "Interagir / Ramasser"},
            {"I",        "Ouvrir l'inventaire"},
            {"N",        "Vision nocturne"},
        };
        float lineH = 26f;
        float startY = py + ph - 55;
        for (String[] cmd : cmds) {
            BitmapText line = CyberUI.texte(font,
                String.format("  %-12s  →  %s", cmd[0], cmd[1]),
                0.95f, CyberUI.BLANC);
            line.setLocalTranslation(px + 20, startY, 3f);
            panneauCommandes.attachChild(line);
            startY -= lineH;
        }
        BitmapText titre = CyberUI.texte(font, "COMMANDES", 1.2f, CyberUI.CYAN);
        CyberUI.centrerX(titre, W, py + ph - 25, 3f);
        panneauCommandes.attachChild(titre);

        BitmapText fermer = CyberUI.texte(font, "[C] Fermer", 0.9f, CyberUI.CYAN_DARK);
        fermer.setLocalTranslation(px + pw - 100, py + 18, 3f);
        panneauCommandes.attachChild(fermer);
    }

    private void toggleCommandes() {
        commandesVisible = !commandesVisible;
        if (commandesVisible) guiNode.attachChild(panneauCommandes);
        else                  guiNode.detachChild(panneauCommandes);
    }

    private void lancerJeu() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new GameState());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(uiNode);
        if (commandesVisible) guiNode.detachChild(panneauCommandes);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{"MenuJouer","MenuQuitter","MenuCommandes"}) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
