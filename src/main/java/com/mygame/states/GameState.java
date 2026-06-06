package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.mygame.environment.DoorManager;
import com.mygame.environment.Laboratory;
import com.mygame.player.BatterieManager;
import com.mygame.player.Inventory;
import com.mygame.player.PlayerControl;
import com.mygame.ui.HudManager;

import java.util.ArrayList;
import java.util.List;

public class GameState extends BaseAppState {

    private SimpleApplication app;
    private BulletAppState    bullet;
    private PlayerControl     joueur;
    private DoorManager       doorManager;
    private HudManager        hud;
    private BatterieManager   batterie;
    private Inventory         inventaire;

    private AmbientLight   ambiant;
    private DirectionalLight soleil;
    private boolean visionNocturne = false;

    // ── Piles ramassables (auto-contact) ─────────────────────────────────────
    private final Node pilesNode = new Node("Piles");

    // ── Objets ramassables (raycasting E key — système Noah) ─────────────────
    private final Node objetsNode = new Node("Objets");

    // ── Enigme 3 — Digicode ───────────────────────────────────────────────────
    private static final String CODE_E3       = "2702";
    private static final float  ROOM3_Z_MIN   = 26f;
    private static final float  ROOM3_Z_MAX   = 56f; // sombre jusqu'a la sortie
    private boolean enigme3Resolue = false;
    private String  codeEntree     = "";

    private static final String CHARADE =
        "=== CHARADE — PORTE SALLE 4 ===\n" +
        "\n" +
        "Mon premier est le numero du Sujet.\n" +
        "Mon second est le niveau de batterie\n" +
        "  qui provoque le Game Over.\n" +
        "Mon troisieme est le numero de la salle.\n" +
        "\n" +
        "Mon tout est le code du Digicode.\n" +
        "\n" +
        "[Approchez la porte et appuyez sur E]";

    private final ActionListener actionListener = this::onAction;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        bullet = new BulletAppState();
        app.getStateManager().attach(bullet);

        // Lumières
        ambiant = new AmbientLight();
        ambiant.setColor(ColorRGBA.White.mult(8.0f));
        app.getRootNode().addLight(ambiant);

        soleil = new DirectionalLight();
        soleil.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        soleil.setColor(ColorRGBA.White.mult(2.0f));
        app.getRootNode().addLight(soleil);

        // Labo + portes
        doorManager = new Laboratory().construire(app.getAssetManager(), app.getRootNode(), bullet);

        // Mains du joueur
        Node nodeCamera = new Node("nodeCamera");
        app.getRootNode().attachChild(nodeCamera);
        Spatial hands = app.getAssetManager().loadModel("Models/player/arms_throwing.glb");
        hands.rotate(0.2f, 0, 0);
        hands.setLocalScale(0.04f);
        nodeCamera.attachChild(hands);

        joueur   = new PlayerControl(bullet, app.getCamera(), nodeCamera, hands);
        batterie = new BatterieManager(600f);
        inventaire = new Inventory();

        // Piles (auto-contact)
        app.getRootNode().attachChild(pilesNode);
        placerPile(new Vector3f( 2f, 1f,  5f));
        placerPile(new Vector3f(-2f, 1f, 20f));
        placerPile(new Vector3f( 1f, 1f, 36f));

        // Objets ramassables (E key + raycasting, marqués "Ramassable")
        app.getRootNode().attachChild(objetsNode);
        placerObjet("Clé Salle 1",   new Vector3f(-1f, 1f,  6f), ColorRGBA.Yellow);
        placerObjet("Badge Accès",   new Vector3f( 1f, 1f, 22f), new ColorRGBA(0.2f, 0.8f, 1f, 1f));
        placerObjet("Carte d'accès Rouge", new Vector3f(0f, 1f, 38f), ColorRGBA.Red);

        // HUD
        hud = new HudManager(
            app.getAssetManager(),
            app.getGuiNode(),
            app.getCamera().getWidth(),
            app.getCamera().getHeight()
        );

        // Caméra FPS
        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setMoveSpeed(0f);
        app.getFlyByCamera().setRotationSpeed(2f);
        app.getInputManager().setCursorVisible(false);

        // Verrouiller la porte 003 (necessite le code Digicode)
        doorManager.verrouiller("003");

        enregistrerTouches();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers placement
    // ─────────────────────────────────────────────────────────────────────────

    /** Objet ramassable via raycasting (touche E). */
    private void placerObjet(String nom, Vector3f pos, ColorRGBA couleur) {
        Geometry g = new Geometry(nom, new Box(0.1f, 0.06f, 0.15f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        g.setMaterial(mat);
        g.setLocalTranslation(pos);
        g.setUserData("Ramassable", true);   // marque comme ramassable (système Noah)
        objetsNode.attachChild(g);
    }

    /** Pile (recharge batterie) ramassée automatiquement au contact. */
    private void placerPile(Vector3f pos) {
        Geometry pile = new Geometry("Pile_" + pilesNode.getQuantity(),
                                     new Cylinder(20, 20, 0.15f, 0.4f, true));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        pile.setMaterial(mat);
        pile.rotate(FastMath.HALF_PI, 0, 0);
        pile.setLocalTranslation(pos);
        pilesNode.attachChild(pile);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Touches
    // ─────────────────────────────────────────────────────────────────────────

    private void enregistrerTouches() {
        app.getInputManager().addMapping("Avancer",         new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("Reculer",         new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("Gauche",          new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("Droite",          new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("Sprint",          new KeyTrigger(KeyInput.KEY_LSHIFT));
        app.getInputManager().addMapping("Accroupir",       new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addMapping("VisionNuit",      new KeyTrigger(KeyInput.KEY_N));
        app.getInputManager().addMapping("Interagir",       new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping("Sauter",          new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addMapping("VoirInventaire",  new KeyTrigger(KeyInput.KEY_I));

        // Touches chiffres pour Digicode
        int[] numKeys = {KeyInput.KEY_0,KeyInput.KEY_1,KeyInput.KEY_2,KeyInput.KEY_3,
                         KeyInput.KEY_4,KeyInput.KEY_5,KeyInput.KEY_6,KeyInput.KEY_7,
                         KeyInput.KEY_8,KeyInput.KEY_9};
        for (int i = 0; i <= 9; i++) {
            app.getInputManager().addMapping("Digi" + i, new KeyTrigger(numKeys[i]));
        }
        app.getInputManager().addMapping("DigiEffacer", new KeyTrigger(KeyInput.KEY_BACK));
        app.getInputManager().addMapping("DigiValider",  new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("DigiFermer",   new KeyTrigger(KeyInput.KEY_ESCAPE));

        app.getInputManager().addListener(actionListener,
                "Avancer","Reculer","Gauche","Droite",
                "Sprint","Accroupir","VisionNuit","Interagir","Sauter","VoirInventaire",
                "Digi0","Digi1","Digi2","Digi3","Digi4",
                "Digi5","Digi6","Digi7","Digi8","Digi9",
                "DigiEffacer","DigiValider","DigiFermer");
    }

    private void onAction(String name, boolean isPressed, float tpf) {
        // Digicode intercept — si le panneau est ouvert, bloquer les autres actions
        if (hud.isDigicodeOuvert()) {
            if (!isPressed) return;
            if (name.startsWith("Digi")) {
                if (name.equals("DigiEffacer")) {
                    if (!codeEntree.isEmpty()) {
                        codeEntree = codeEntree.substring(0, codeEntree.length() - 1);
                        hud.updateDigicode(codeEntree);
                        hud.setFeedbackDigicode("", ColorRGBA.White);
                    }
                } else if (name.equals("DigiValider")) {
                    validerCodeDigicode();
                } else if (name.equals("DigiFermer")) {
                    hud.fermerDigicode();
                    codeEntree = "";
                } else {
                    // Chiffre 0-9
                    String chiffre = name.replace("Digi", "");
                    if (codeEntree.length() < 4) {
                        codeEntree += chiffre;
                        hud.updateDigicode(codeEntree);
                        if (codeEntree.length() == 4) validerCodeDigicode();
                    }
                }
            }
            return; // bloquer tout le reste
        }

        switch (name) {
            case "Avancer"        -> joueur.setForward(isPressed);
            case "Reculer"        -> joueur.setBackward(isPressed);
            case "Gauche"         -> joueur.setLeft(isPressed);
            case "Droite"         -> joueur.setRight(isPressed);
            case "Sprint"         -> { joueur.setSprint(isPressed); batterie.setSprint(isPressed); }
            case "Accroupir"      -> { if (isPressed) joueur.setCrouch(!joueur.isCrouch()); }
            case "VisionNuit"     -> { if (isPressed) basculerVisionNuit(); }
            case "Sauter"         -> { if (isPressed) joueur.sauter(); }
            case "VoirInventaire" -> { if (isPressed) hud.toggleInventaire(inventaire.getObjets()); }
            case "Interagir"      -> {
                if (isPressed) {
                    // 1. Porte verrouillee (enigme 3) → ouvrir digicode
                    if (doorManager.isPorteProcheVerrouillee()) {
                        codeEntree = "";
                        hud.ouvrirDigicode();
                        hud.updateDigicode("");
                    } else {
                        // 2. Ouvrir porte normale
                        boolean portOuverte = doorManager.interagir();
                        // 3. Sinon ramasser objet
                        if (!portOuverte) tenterDeRamasser();
                    }
                }
            }
        }
    }

    private void validerCodeDigicode() {
        if (codeEntree.equals(CODE_E3)) {
            hud.setFeedbackDigicode("CODE ACCEPTE !", ColorRGBA.Green);
            enigme3Resolue = true;
            doorManager.deverrouiller("003");
            hud.setCharade("");
            // Fermer apres 1 seconde (approximation via flag)
            new Thread(() -> {
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                hud.fermerDigicode();
                codeEntree = "";
            }).start();
        } else {
            hud.setFeedbackDigicode("CODE INCORRECT !", ColorRGBA.Red);
            codeEntree = "";
            hud.updateDigicode("");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Raycasting (système Noah)
    // ─────────────────────────────────────────────────────────────────────────

    private void tenterDeRamasser() {
        CollisionResults resultats = new CollisionResults();
        Ray rayon = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
        app.getRootNode().collideWith(rayon, resultats);

        if (resultats.size() > 0) {
            Geometry cible    = resultats.getClosestCollision().getGeometry();
            float    distance = resultats.getClosestCollision().getDistance();

            if (distance < 3.0f && Boolean.TRUE.equals(cible.getUserData("Ramassable"))) {
                String nom = cible.getName();
                if (inventaire.ajouter(nom)) {
                    cible.removeFromParent();
                    hud.setMessageInteraction("Ramassé : " + nom);
                    System.out.println("[GameState] Ramassé : " + nom);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void update(float tpf) {
        if (!isEnabled()) return;
        joueur.update(tpf);

        Vector3f pos = joueur.getCharacterControl().getPhysicsLocation();

        // Batterie
        batterie.update(tpf);
        hud.updateBatterie(batterie.getPourcentage());

        if (batterie.isGameOver()) {
            app.getStateManager().detach(this);
            app.getRootNode().detachAllChildren();
            app.getStateManager().attach(new GameOverState());
        }

        // Piles au contact
        for (int i = pilesNode.getQuantity() - 1; i >= 0; i--) {
            Spatial pile = pilesNode.getChild(i);
            if (pos.distanceSquared(pile.getWorldTranslation()) < 2f * 2f) {
                batterie.rechargerAFond();
                pile.removeFromParent();
                hud.setMessageInteraction("Pile ramassée ! Batterie rechargée !");
            }
        }

        // ── Enigme 3 — salle sombre ───────────────────────────────────────────
        boolean inSalleSombre = !enigme3Resolue && pos.z > ROOM3_Z_MIN && pos.z < ROOM3_Z_MAX;
        if (inSalleSombre) {
            if (visionNocturne) {
                hud.setCharade(CHARADE);
            } else {
                hud.setCharade("");
                // Presque noir sans vision nocturne
                ambiant.setColor(new ColorRGBA(0.03f, 0.03f, 0.03f, 1f));
                hud.setMessageInteraction("[N] Activer la vision nocturne");
            }
        } else if (!inSalleSombre && !enigme3Resolue && pos.z <= ROOM3_Z_MIN) {
            hud.setCharade("");
            if (!visionNocturne) ambiant.setColor(ColorRGBA.White.mult(8.0f));
        } else if (enigme3Resolue) {
            hud.setCharade("");
        }

        // Prompt "Regarder objet → [E] Ramasser" (raycasting passif)
        boolean porteProche = doorManager.update(pos);
        if (porteProche) {
            if (doorManager.isPorteProcheVerrouillee())
                hud.setMessageInteraction("[E] Entrer le code Digicode");
            else
                hud.setMessageInteraction("[E] Ouvrir la porte");
        } else {
            // Vérifie si le joueur vise un objet ramassable
            CollisionResults r = new CollisionResults();
            app.getRootNode().collideWith(
                new Ray(app.getCamera().getLocation(), app.getCamera().getDirection()), r);
            if (r.size() > 0
                    && r.getClosestCollision().getDistance() < 3f
                    && Boolean.TRUE.equals(r.getClosestCollision().getGeometry().getUserData("Ramassable"))) {
                hud.setMessageInteraction("[E] Ramasser : "
                    + r.getClosestCollision().getGeometry().getName());
            } else {
                hud.setMessageInteraction("");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void basculerVisionNuit() {
        visionNocturne = !visionNocturne;
        if (visionNocturne) {
            ambiant.setColor(new ColorRGBA(0f, 1f, 0f, 1f));
            app.getViewPort().setBackgroundColor(new ColorRGBA(0f, 0.05f, 0f, 1f));
            hud.setVisionNuit(true);
        } else {
            ambiant.setColor(ColorRGBA.White.mult(8.0f));
            app.getViewPort().setBackgroundColor(ColorRGBA.Black);
            hud.setVisionNuit(false);
        }
    }

    @Override
    protected void cleanup(Application application) {
        app.getStateManager().detach(bullet);
        app.getRootNode().detachAllChildren();
        app.getRootNode().removeLight(ambiant);
        app.getRootNode().removeLight(soleil);
        app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(true);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{
            "Avancer","Reculer","Gauche","Droite",
            "Sprint","Accroupir","VisionNuit","Interagir","Sauter","VoirInventaire",
            "Digi0","Digi1","Digi2","Digi3","Digi4",
            "Digi5","Digi6","Digi7","Digi8","Digi9",
            "DigiEffacer","DigiValider","DigiFermer"
        }) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
        if (hud != null) hud.detacher();
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
