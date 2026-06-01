package com.mygame.environment;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Laboratory {

    public DoorManager construire(AssetManager am, Node rootNode, BulletAppState bullet) {
        DoorManager doorManager = new DoorManager(bullet, am);

        Spatial laboModel = am.loadModel("Models/room/labo_baked.glb");
        supprimerControles(laboModel);
        forcerUnshaded(laboModel, am);   // PBR → Unshaded pour afficher la texture baked

        // ── 1. Attacher au rootNode pour calculer les transforms monde ────────
        rootNode.attachChild(laboModel);
        rootNode.updateGeometricState();

        // ── 2. Trouver panneaux + stripes (transforms monde corrects) ─────────
        List<Spatial> portes = doorManager.detecterPortes(laboModel);

        // Sauvegarder transforms monde et détacher du modèle
        List<Vector3f>   worldPos = new ArrayList<>();
        List<Quaternion> worldRot = new ArrayList<>();
        List<Vector3f>   worldScl = new ArrayList<>();

        for (Spatial porte : portes) {
            worldPos.add(porte.getWorldTranslation().clone());
            worldRot.add(porte.getWorldRotation().clone());
            worldScl.add(porte.getWorldScale().clone());
            porte.getParent().detachChild(porte);
        }

        // ── 3. Collision du labo SANS les portes ─────────────────────────────
        CollisionShape shape = CollisionShapeFactory.createMeshShape(laboModel);
        RigidBodyControl rbc  = new RigidBodyControl(shape, 0f);
        laboModel.addControl(rbc);
        bullet.getPhysicsSpace().add(rbc);

        // ── 4. Réattacher les portes au rootNode avec leurs transforms monde ──
        for (int i = 0; i < portes.size(); i++) {
            Spatial porte = portes.get(i);
            rootNode.attachChild(porte);
            porte.setLocalTranslation(worldPos.get(i));
            porte.setLocalRotation(worldRot.get(i));
            porte.setLocalScale(worldScl.get(i));
        }

        // ── 5. Recalculer les transforms AVANT de créer les blockers physics ──
        //      (garantit que getWorldBound() est correct pour chaque porte)
        rootNode.updateGeometricState();

        // ── 6. Enregistrer la physique (blocker box) de chaque porte ─────────
        //      Le updateGeometricState() juste au-dessus garantit que le
        //      world-transform de chaque porte est correct AVANT d'appeler
        //      addControl / getPhysicsSpace().add → la boîte est bien positionnée.
        for (Spatial porte : portes) {
            doorManager.enregistrerExtrait(porte, rootNode);
        }

        return doorManager;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void forcerUnshaded(Spatial s, AssetManager am) {
        if (s instanceof Geometry) {
            Geometry geo    = (Geometry) s;
            Material ancien = geo.getMaterial();

            Material unshaded = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");

            // Parcourir TOUS les paramètres du matériau PBR pour trouver une texture
            Texture tex = null;
            if (ancien != null) {
                for (MatParam p : ancien.getParams()) {
                    if (p != null && p.getValue() instanceof Texture) {
                        tex = (Texture) p.getValue();
                        break;
                    }
                }
            }

            if (tex != null) {
                unshaded.setTexture("ColorMap", tex);
                unshaded.setColor("Color", ColorRGBA.White);
            } else {
                unshaded.setColor("Color", new ColorRGBA(0.75f, 0.73f, 0.68f, 1f));
            }
            geo.setMaterial(unshaded);
        }
        if (s instanceof Node) {
            for (Spatial enfant : ((Node) s).getChildren()) {
                forcerUnshaded(enfant, am);
            }
        }
    }

    private void supprimerControles(Spatial s) {
        try {
            Field f = Spatial.class.getDeclaredField("controls");
            f.setAccessible(true);
            ((java.util.List<?>) f.get(s)).clear();
        } catch (Exception ignored) {}
        if (s instanceof Node) {
            for (Spatial enfant : ((Node) s).getChildren()) {
                supprimerControles(enfant);
            }
        }
    }
}
