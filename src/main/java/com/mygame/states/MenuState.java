package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Menu principal — style cyber/neon (design Aymen).
 * Structure identique a l'original Swing : header + 3 boutons en boites.
 */
public class MenuState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private Node uiNode;

    // Dimensions boutons (pour detection clic souris)
    private float btnX, btnW = 830f, btnH = 60f;
    private float btnJouerY, btnCommandesY, btnQuitterY;

    private boolean commandesVisible = false;
    private Node    panneauCommandes;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        switch (name) {
            case "MenuJouer"     -> lancerJeu();
            case "MenuQuitter"   -> app.stop();
            case "MenuCommandes" -> toggleCommandes();
            case "MenuClic"      -> detecterClicSouris();
        }
    };

    @Override
    protected void initialize(Application application) {
        this.app     = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        float base      = font.getCharSet().getRenderedSize();

        uiNode = new Node("MenuUI");

        // ── Fond plein ecran ──────────────────────────────────────────────────
        uiNode.attachChild(fondEcran(W, H, "Interface/bg/menu.png"));

        // ── Layout centre — maquette : colonne ~66% de l'ecran, aeree ────────
        btnW = Math.min(660f, W - 120f);
        btnH = 50f;
        btnX = (W - btnW) / 2f;
        float contentTop = H * 0.86f;

        // ── Header : cadre fin cyan, fond sombre translucide ──────────────────
        float headerH = 150f;
        float headerY = contentTop - headerH;
        uiNode.attachChild(boite(btnX, headerY, btnW, headerH,
                new ColorRGBA(0.03f, 0.05f, 0.07f, 0.80f)));

        // Titre : cyan vif, grand
        BitmapText subject = txt(font, "SUBJECT: 27", 1.6f, new ColorRGBA(0f, 0.95f, 1f, 1f));
        centrer(subject, W, headerY + headerH - 34, 3f);
        uiNode.attachChild(subject);

        // Sous-titre : blanc, lettres espacees (style cyber de la maquette)
        BitmapText sequence = txt(font, espacer("SEQUENCE D'EVEIL - SUJET 27"), 1.0f, BLANC);
        centrer(sequence, W, headerY + headerH - 78, 3f);
        uiNode.attachChild(sequence);

        // Infos techniques discretes (coins bas du header)
        BitmapText techL = txt(font, "STBL_INIT_0.94", 0.7f, CYN_D);
        techL.setLocalTranslation(btnX + 14, headerY + 22, 3f);
        uiNode.attachChild(techL);

        BitmapText techR = txt(font, "LOC: SEC_B_WNG_04", 0.7f, CYN_D);
        techR.setLocalTranslation(btnX + btnW - 150, headerY + 22, 3f);
        uiNode.attachChild(techR);

        // ── Boutons : fins, sombres, lettres espacees ─────────────────────────
        float gap = 16f;
        btnJouerY     = headerY - 30f - btnH;
        btnCommandesY = btnJouerY - gap - btnH;
        btnQuitterY   = btnCommandesY - gap - btnH;

        uiNode.attachChild(boutonBoite(font, "JOUER",      btnX, btnJouerY,     btnW, btnH, CYN));
        uiNode.attachChild(boutonBoite(font, "COMMANDES",  btnX, btnCommandesY, btnW, btnH, CYN));
        uiNode.attachChild(boutonBoite(font, "QUITTER",    btnX, btnQuitterY,   btnW, btnH, CYN));

        guiNode.attachChild(uiNode);

        // ── Panneau commandes (cache) ─────────────────────────────────────────
        construirePanneauCommandes(font, W, H);

        // ── Touches ───────────────────────────────────────────────────────────
        app.getInputManager().addMapping("MenuJouer",    new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("MenuQuitter",  new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("MenuCommandes",new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addMapping("MenuClic",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(actionListener,
                "MenuJouer","MenuQuitter","MenuCommandes","MenuClic");

        app.getInputManager().setCursorVisible(true);
        app.getViewPort().setBackgroundColor(
                new ColorRGBA(0.03f, 0.03f, 0.05f, 1f));
    }

    // ── Detection clic souris ─────────────────────────────────────────────────

    private void detecterClicSouris() {
        Vector2f c = app.getInputManager().getCursorPosition();
        float cx = c.x, cy = c.y;
        if (cx >= btnX && cx <= btnX + btnW) {
            if (cy >= btnJouerY     && cy <= btnJouerY     + btnH) { lancerJeu();       return; }
            if (cy >= btnCommandesY && cy <= btnCommandesY + btnH) { toggleCommandes(); return; }
            if (cy >= btnQuitterY   && cy <= btnQuitterY   + btnH) { app.stop();        }
        }
    }

    // ── Helpers visuels ───────────────────────────────────────────────────────

    /** Bouton fin : fond sombre quasi opaque, bordure cyan, lettres espacees. */
    private Node boutonBoite(BitmapFont font, String label,
                             float x, float y, float w, float h,
                             ColorRGBA couleur) {
        Node n = new Node("Btn_" + label);
        n.attachChild(boite(x, y, w, h, new ColorRGBA(0.04f, 0.05f, 0.07f, 0.92f)));

        BitmapText t = txt(font, espacer(label), 1.1f, couleur);
        t.setLocalTranslation(x + w / 2f - t.getLineWidth() / 2f,
                              y + h / 2f + t.getSize() * 0.4f, 3f);
        n.attachChild(t);
        return n;
    }

    /** "JOUER" -> "J O U E R" (lettres espacees, style cyber de la maquette). */
    private static String espacer(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (i < s.length() - 1) sb.append(' ');
        }
        return sb.toString();
    }

    /** Fond rectangulaire avec bordure cyan (style translucide d'origine). */
    private Node boite(float x, float y, float w, float h, ColorRGBA fond) {
        Node n = new Node("box");
        Geometry bg = geo("bg", x, y, w, h, 0.5f, fond, true);
        n.attachChild(bg);
        // Bordure (4 barres)
        n.attachChild(barre(x,       y + h - 1, w,  1));  // haut
        n.attachChild(barre(x,       y,          w,  1));  // bas
        n.attachChild(barre(x,       y,          1,  h));  // gauche
        n.attachChild(barre(x + w - 1, y,        1,  h));  // droite
        return n;
    }

    private Geometry barre(float x, float y, float w, float h) {
        return geo("b", x, y, w, h, 2f, CYN, false);
    }

    private Geometry geo(String name, float x, float y, float w, float h,
                         float z, ColorRGBA color, boolean alpha) {
        Geometry g = new Geometry(name, new Quad(w, h));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        if (alpha) mat.getAdditionalRenderState().setBlendMode(
                com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        // IMPORTANT : on reste dans le bucket Gui (pas Transparent) → le tri par Z
        // avec les textes fonctionne, et le blending rend le fond translucide.
        g.setLocalTranslation(x, y, z);
        return g;
    }

    private Geometry fondEcran(int w, int h, String path) {
        Geometry bg = new Geometry("BG", new Quad(w, h));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture tex = app.getAssetManager().loadTexture(path);
            mat.setTexture("ColorMap", tex);
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
        t.setColor(c);
        t.setText(s);
        return t;
    }

    private static void centrer(BitmapText t, int W, float y, float z) {
        t.setLocalTranslation(W / 2f - t.getLineWidth() / 2f, y, z);
    }

    // Palette (ASCII safe)
    private static final ColorRGBA CYN   = new ColorRGBA(0f, 1f,    1f,    1f);
    private static final ColorRGBA CYN_D = new ColorRGBA(0f, 0.38f, 0.38f, 0.9f);
    private static final ColorRGBA BLANC = new ColorRGBA(0.9f, 0.9f, 0.9f, 1f);

    // ── Panneau commandes ─────────────────────────────────────────────────────

    private void construirePanneauCommandes(BitmapFont font, int W, int H) {
        panneauCommandes = new Node("Commandes");
        // Z +10 : passe DEVANT les textes du menu (sinon ils transparaissent)
        panneauCommandes.setLocalTranslation(0, 0, 10f);
        float pw = 520f, ph = 340f;
        float px = (W - pw) / 2f, py = (H - ph) / 2f;

        // Fond presque opaque -> texte des commandes lisible (Z+10 = devant le menu)
        panneauCommandes.attachChild(boite(px, py, pw, ph,
                new ColorRGBA(0.05f, 0.07f, 0.09f, 0.97f)));

        BitmapText titre = txt(font, "COMMANDES", 1.2f, CYN);
        centrer(titre, W, py + ph - 28, 3f);
        panneauCommandes.attachChild(titre);

        String[][] cmds = {
            {"W / S / A / D",  "Se deplacer"},
            {"ESPACE",         "Sauter"},
            {"SHIFT",          "Sprint"},
            {"C",              "S'accroupir"},
            {"E",              "Interagir / Ramasser"},
            {"I",              "Inventaire SAC A DOS"},
            {"N",              "Vision nocturne"},
        };
        float lineH = 36f;
        float startY = py + ph - 68;
        for (String[] cmd : cmds) {
            BitmapText line = txt(font,
                    String.format("%-18s ->  %s", cmd[0], cmd[1]), 0.95f, BLANC);
            line.setLocalTranslation(px + 25, startY, 3f);
            panneauCommandes.attachChild(line);
            startY -= lineH;
        }

        BitmapText fermer = txt(font, "[C] Fermer", 0.85f, CYN_D);
        fermer.setLocalTranslation(px + pw - 95, py + 14, 3f);
        panneauCommandes.attachChild(fermer);
    }

    private void toggleCommandes() {
        commandesVisible = !commandesVisible;
        if (commandesVisible) guiNode.attachChild(panneauCommandes);
        else                  guiNode.detachChild(panneauCommandes);
    }

    private void lancerJeu() {
        app.getStateManager().detach(this);
        // Video d'intro (Aymen) ; le labo se precharge pendant -> jeu des la fin
        app.getStateManager().attach(new VideoIntroState());
    }

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(uiNode);
        if (commandesVisible) guiNode.detachChild(panneauCommandes);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{"MenuJouer","MenuQuitter","MenuCommandes","MenuClic"}) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
