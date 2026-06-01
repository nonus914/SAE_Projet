package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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
    private BulletAppState bullet;
    private PlayerControl joueur;
    private DoorManager doorManager;
    private HudManager hud;
    private BatterieManager batterie;
    private Inventory inventaire;

    private AmbientLight ambiant;
    private DirectionalLight soleil;
    private boolean visionNocturne = false;

    // ── Piles ramassables (système de Lotfi) ─────────────────────────────────
    private final Node pilesNode = new Node("Piles");
    private final List<Vector3f> positionsPiles = new ArrayList<>();

    private final ActionListener actionListener = this::onAction;

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        bullet = new BulletAppState();
        app.getStateManager().attach(bullet);

        // Lumières — ambient fort pour un éclairage baked "fullbright"
        ambiant = new AmbientLight();
        ambiant.setColor(ColorRGBA.White.mult(4.0f));
        app.getRootNode().addLight(ambiant);

        soleil = new DirectionalLight();
        soleil.setDirection(new Vector3f(-0.3f, -1f, -0.5f).normalizeLocal());
        soleil.setColor(ColorRGBA.White.mult(0.5f));
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

        joueur = new PlayerControl(bullet, app.getCamera(), nodeCamera, hands);

        // Batterie — 10 minutes pour s'échapper
        batterie = new BatterieManager(600f);

        // Inventaire
        inventaire = new Inventory();

        // Piles à ramasser (positions dans le labo — à ajuster)
        app.getRootNode().attachChild(pilesNode);
        placerPile(new Vector3f(2f,  1f,  5f));
        placerPile(new Vector3f(-2f, 1f, 20f));
        placerPile(new Vector3f(1f,  1f, 36f));

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

        enregistrerTouches();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void placerPile(Vector3f pos) {
        Cylinder shape = new Cylinder(20, 20, 0.15f, 0.4f, true);
        Geometry pile = new Geometry("Pile_" + pilesNode.getQuantity(), shape);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        pile.setMaterial(mat);
        pile.rotate(FastMath.HALF_PI, 0, 0);
        pile.setLocalTranslation(pos);
        pilesNode.attachChild(pile);
        positionsPiles.add(pos.clone());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void enregistrerTouches() {
        app.getInputManager().addMapping("Avancer",    new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("Reculer",    new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("Gauche",     new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("Droite",     new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("Sprint",     new KeyTrigger(KeyInput.KEY_LSHIFT));
        app.getInputManager().addMapping("Accroupir",  new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addMapping("VisionNuit", new KeyTrigger(KeyInput.KEY_N));
        app.getInputManager().addMapping("Interagir",  new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping("Sauter",     new KeyTrigger(KeyInput.KEY_SPACE));

        app.getInputManager().addListener(actionListener,
                "Avancer", "Reculer", "Gauche", "Droite",
                "Sprint", "Accroupir", "VisionNuit", "Interagir", "Sauter");
    }

    private void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "Avancer"    -> joueur.setForward(isPressed);
            case "Reculer"    -> joueur.setBackward(isPressed);
            case "Gauche"     -> joueur.setLeft(isPressed);
            case "Droite"     -> joueur.setRight(isPressed);
            case "Sprint"     -> { joueur.setSprint(isPressed); batterie.setSprint(isPressed); }
            case "Accroupir"  -> { if (isPressed) joueur.setCrouch(!joueur.isSprint()); }
            case "VisionNuit" -> { if (isPressed) basculerVisionNuit(); }
            case "Interagir"  -> { if (isPressed) doorManager.interagir(); }
            case "Sauter"     -> { if (isPressed) joueur.sauter(); }
        }
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) return;
        joueur.update(tpf);

        Vector3f posJoueur = joueur.getCharacterControl().getPhysicsLocation();

        // ── Batterie ─────────────────────────────────────────────────────────
        batterie.update(tpf);
        hud.updateBatterie(batterie.getPourcentage());

        // ── Inventaire ───────────────────────────────────────────────────────
        hud.updateInventaire(inventaire.toAffichage());

        if (batterie.estVide()) {
            // TODO : déclencher GameOverState
            System.out.println("[GameState] Batterie vide → Game Over");
        }

        // ── Ramassage de piles ────────────────────────────────────────────────
        for (int i = pilesNode.getQuantity() - 1; i >= 0; i--) {
            Spatial pile = pilesNode.getChild(i);
            if (posJoueur.distanceSquared(pile.getWorldTranslation()) < 2f * 2f) {
                batterie.rechargerAFond();
                pile.removeFromParent();
                hud.setMessageInteraction("Pile ramassée ! Batterie rechargée !");
                System.out.println("[GameState] Pile ramassée.");
            }
        }

        // ── Portes ───────────────────────────────────────────────────────────
        boolean porteProche = doorManager.update(posJoueur);
        if (porteProche) hud.setMessageInteraction("[E] Ouvrir la porte");
        else if (pilesNode.getQuantity() == 0 || posJoueur.distanceSquared(
                pilesNode.getQuantity() > 0
                    ? pilesNode.getChild(0).getWorldTranslation()
                    : posJoueur) > 4f) {
            hud.setMessageInteraction("");
        }
    }

    private void basculerVisionNuit() {
        visionNocturne = !visionNocturne;
        batterie.setVisionNocturne(visionNocturne);
        if (visionNocturne) {
            ambiant.setColor(new ColorRGBA(0f, 1f, 0f, 1f));
            app.getViewPort().setBackgroundColor(new ColorRGBA(0f, 0.05f, 0f, 1f));
            hud.setVisionNuit(true);
        } else {
            ambiant.setColor(ColorRGBA.White.mult(4.0f));
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
            "Sprint","Accroupir","VisionNuit","Interagir","Sauter"
        }) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
        if (hud != null) hud.detacher();
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
