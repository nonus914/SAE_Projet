package com.mygame.player;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Contrôle FPS du joueur avec physique BulletAppState.
 * Gère déplacement, saut, accroupissement et bob des mains.
 */
public class PlayerControl {

    private static final float HAUTEUR_NORMAL = 1.7f;
    private static final float HAUTEUR_ACCROUPI = 1.0f;
    private static final float VITESSE_NORMAL = 5f;
    private static final float VITESSE_SPRINT = 10f;

    private final CharacterControl characterControl;
    private final Camera camera;
    private final Node nodeCamera;
    private final Spatial hands;

    private boolean forward, backward, left, right;
    private boolean sprint = false;
    private boolean crouch = false;
    private float walkTime = 0f;

    public PlayerControl(BulletAppState bullet, Camera camera, Node nodeCamera, Spatial hands) {
        this.camera = camera;
        this.nodeCamera = nodeCamera;
        this.hands = hands;

        CapsuleCollisionShape capsule = new CapsuleCollisionShape(0.4f, 0.9f, 1);
        // stepHeight = 0.4 m → peut monter des marches normales (~17-20 cm chacune)
        characterControl = new CharacterControl(capsule, 0.4f);
        characterControl.setJumpSpeed(10f);
        characterControl.setFallSpeed(30f);
        characterControl.setGravity(30f);
        // Spawn dans la Salle 1 (z≈3), face à Door_001 (z≈8)
        characterControl.setPhysicsLocation(new Vector3f(0f, 2f, 3f));

        bullet.getPhysicsSpace().add(characterControl);
    }

    public void update(float tpf) {
        float vitesse = sprint ? VITESSE_SPRINT : VITESSE_NORMAL;

        Vector3f dir = camera.getDirection().clone();
        Vector3f gauche = camera.getLeft().clone();
        dir.y = 0;
        gauche.y = 0;
        dir.normalizeLocal();
        gauche.normalizeLocal();

        Vector3f mouvement = new Vector3f();
        if (forward)  mouvement.addLocal(dir);
        if (backward) mouvement.subtractLocal(dir);
        if (left)     mouvement.addLocal(gauche);
        if (right)    mouvement.subtractLocal(gauche);

        boolean bouge = mouvement.length() > 0;
        if (bouge) mouvement.normalizeLocal().multLocal(vitesse);
        characterControl.setWalkDirection(mouvement.multLocal(tpf));

        // Hauteur caméra selon accroupissement
        float hauteurCible = crouch ? HAUTEUR_ACCROUPI : HAUTEUR_NORMAL;
        Vector3f pos = characterControl.getPhysicsLocation().clone();
        pos.y += hauteurCible;
        camera.setLocation(pos);

        // Synchroniser le noeud caméra pour les mains
        nodeCamera.setLocalTranslation(pos);
        nodeCamera.setLocalRotation(camera.getRotation());

        // Animation bob des mains
        if (bouge) {
            walkTime += tpf * 8f;
            float bobX = FastMath.sin(walkTime) * 0.05f;
            float bobY = FastMath.abs(FastMath.sin(walkTime)) * 0.08f;
            float handsZ = crouch ? 2.0f : 2.5f;
            hands.setLocalTranslation(bobX, -0.4f + bobY, handsZ);
        } else {
            walkTime = 0f;
            float handsZ = crouch ? 2.0f : 2.5f;
            hands.setLocalTranslation(0f, -0.4f, handsZ);
        }
    }

    // --- Setters actions ---
    public void setForward(boolean v)  { forward = v; }
    public void setBackward(boolean v) { backward = v; }
    public void setLeft(boolean v)     { left = v; }
    public void setRight(boolean v)    { right = v; }
    public void setSprint(boolean v)   { sprint = v; }
    public void setCrouch(boolean v)   { crouch = v; }

    public boolean isSprint()  { return sprint; }
    public boolean isCrouch()  { return crouch; }

    /** Fait sauter le joueur si il est au sol. */
    public void sauter() {
        if (characterControl.onGround()) {
            characterControl.jump();
        }
    }

    public CharacterControl getCharacterControl() { return characterControl; }
}
