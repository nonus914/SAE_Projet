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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.mygame.environment.DoorManager;
import com.mygame.environment.Laboratory;
import com.mygame.player.PlayerControl;
import com.mygame.ui.HudManager;

public class GameState extends BaseAppState {

    private SimpleApplication app;
    private BulletAppState bullet;
    private PlayerControl joueur;
    private DoorManager doorManager;
    private HudManager hud;

    private AmbientLight ambiant;
    private DirectionalLight soleil;
    private boolean visionNocturne = false;

    private final ActionListener actionListener = this::onAction;

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        bullet = new BulletAppState();
        app.getStateManager().attach(bullet);

        // Lumières — ambient fort pour simuler un éclairage baked "fullbright"
        // (PBR Lighting respecte les couleurs réelles de chaque matériau)
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
        Spatial hands = app.getAssetManager().loadModel("Models/player/arms_throwing.j3o");
        hands.rotate(0.2f, 0, 0);
        hands.setLocalScale(0.04f);
        nodeCamera.attachChild(hands);

        joueur = new PlayerControl(bullet, app.getCamera(), nodeCamera, hands);

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
            case "Sprint"     -> joueur.setSprint(isPressed);
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

        // Détection porte proche → prompt HUD
        Vector3f posJoueur = joueur.getCharacterControl().getPhysicsLocation();
        boolean porteProche = doorManager.update(posJoueur);
        hud.setMessageInteraction(porteProche ? "[E] Ouvrir la porte" : "");
    }

    private void basculerVisionNuit() {
        visionNocturne = !visionNocturne;
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
