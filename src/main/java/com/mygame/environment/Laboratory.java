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
        listerEtSupprimerEscaliers(laboModel);

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

        // ── 7. Enigme 1 (Noah) — Tableau d'indices + borne Digicode salle 1 ───
        // Tableau accroché au mur du fond de la salle 1 (derrière le spawn)
        com.jme3.scene.shape.Quad quadTableau = new com.jme3.scene.shape.Quad(6.0f, 3.0f);
        Geometry objetTableau = new Geometry("Tableau_Indices", quadTableau);
        Material matTableau = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            com.jme3.texture.Texture texTableau = am.loadTexture("Textures/tableau_enigme.png");
            texTableau.setMagFilter(com.jme3.texture.Texture.MagFilter.Nearest);
            texTableau.setMinFilter(com.jme3.texture.Texture.MinFilter.NearestNoMipMaps);
            matTableau.setTexture("ColorMap", texTableau);
        } catch (Exception e) {
            System.out.println("[Laboratory] tableau_enigme.png introuvable — couleur blanche par défaut");
            matTableau.setColor("Color", ColorRGBA.White);
        }
        objetTableau.setMaterial(matTableau);
        // Position : mur du fond salle 1 (Z=-3 = derrière le spawn Z=3, face au joueur qui regarde en arrière)
        objetTableau.setLocalTranslation(new Vector3f(2.0f, 1.5f, -3.0f));
        rootNode.attachChild(objetTableau);

        // Borne digicode salle 1 (boîtier interactif — raycasting par nom)
        com.jme3.scene.shape.Box boiteDigicode = new com.jme3.scene.shape.Box(0.1f, 0.15f, 0.05f);
        Geometry objetDigicode = new Geometry("Digicode_Interactif_Salle1", boiteDigicode);
        Material matDigicode = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matDigicode.setColor("Color", ColorRGBA.DarkGray);
        objetDigicode.setMaterial(matDigicode);
        objetDigicode.setLocalTranslation(new Vector3f(2.5f, 1.2f, -3.0f));
        // Physique (nécessaire pour que le raycasting fonctionne)
        RigidBodyControl rbcDigicode = new RigidBodyControl(
                CollisionShapeFactory.createBoxShape(objetDigicode), 0f);
        objetDigicode.addControl(rbcDigicode);
        bullet.getPhysicsSpace().add(rbcDigicode);
        rootNode.attachChild(objetDigicode);

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

    /**
     * Parcourt le modele, affiche tous les noms, et supprime les escaliers.
     * Mots-cles cherches (insensible a la casse) : stair, escal, step, marche, ramp
     */
    private void listerEtSupprimerEscaliers(Spatial s) {
        String nom   = s.getName() == null ? "" : s.getName();
        String lower = nom.toLowerCase();

        // Afficher pour debug
        System.out.println("[LAB MESH] " + nom + " (" + s.getClass().getSimpleName() + ")");

        // Supprimer si c'est un escalier
        // Supprimer escaliers ET estrades (deck slabs + colonnes + rails)
        boolean aSupprimer = !nom.isEmpty() && (
            lower.contains("stair")  ||
            lower.contains("escal")  ||
            lower.contains("step")   ||
            lower.contains("marche") ||
            lower.contains("ramp")   ||
            lower.contains("slab")   ||   // plateau de l'estrade
            lower.contains("deck")        // toute la structure deck (colonnes, rails, plateau)
        );

        if (aSupprimer) {
            // Détacher du parent = invisible ET retiré de la physique
            if (s.getParent() != null) {
                s.getParent().detachChild(s);
            } else {
                s.setCullHint(Spatial.CullHint.Always);
            }
            System.out.println("[LAB] SUPPRIME : " + nom);
            return;
        }

        if (s instanceof Node) {
            for (Spatial enfant : new java.util.ArrayList<>(((Node) s).getChildren())) {
                listerEtSupprimerEscaliers(enfant);
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
