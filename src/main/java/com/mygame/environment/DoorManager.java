package com.mygame.environment;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gère les portes du labo (Door_001 … Door_004).
 *
 * Chaque porte a :
 *  - un BoxCollisionShape qui bloque le mouvement du joueur
 *  - un view-blocker (Geometry quad opaque) qui bouche l'embrasure visuellement
 *
 * Quand E est pressé les deux sont supprimés → le joueur peut traverser et voir.
 *
 * PRÉCONDITION : Laboratory appelle rootNode.updateGeometricState() AVANT
 * enregistrerExtrait() pour que les world-transforms soient corrects.
 */
public class DoorManager {

    private static final float PORTEE = 3.0f;

    private final BulletAppState bullet;
    private final AssetManager   assetManager;

    private final List<Spatial>   aExtraire = new ArrayList<>();
    private final List<DoorGroup> groupes   = new ArrayList<>();
    private DoorGroup groupeProche = null;

    public DoorManager(BulletAppState bullet, AssetManager assetManager) {
        this.bullet       = bullet;
        this.assetManager = assetManager;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Détection
    // ─────────────────────────────────────────────────────────────────────────

    public List<Spatial> detecterPortes(Spatial model) {
        Map<String, Spatial> panels  = new HashMap<>();
        Map<String, Spatial> stripes = new HashMap<>();
        collecter(model, panels, stripes);

        for (Map.Entry<String, Spatial> e : panels.entrySet()) {
            String  num    = e.getKey();
            Spatial panel  = e.getValue();
            Spatial stripe = stripes.get(num);
            groupes.add(new DoorGroup(num, panel, stripe));
            aExtraire.add(panel);
            if (stripe != null) aExtraire.add(stripe);
        }

        if (groupes.isEmpty()) {
            System.out.println("[DoorManager] AVERTISSEMENT : aucune porte. Hierarchie :");
            afficherHierarchie(model, 0);
        } else {
            System.out.println("[DoorManager] " + groupes.size() + " porte(s) detectee(s).");
        }
        return aExtraire;
    }

    /**
     * Enregistre physique + view-blocker pour un panneau de porte.
     *
     * Appelé depuis Laboratory APRÈS rootNode.updateGeometricState() → les
     * world-transforms sont corrects → la boîte physique et le view-blocker
     * sont placés exactement à l'embrasure.
     */
    public void enregistrerExtrait(Spatial s, Node rootNode) {
        for (DoorGroup g : groupes) {
            if (g.panel == s) {

                // ── Dimensions depuis la bounding box monde ───────────────
                s.updateModelBound();
                BoundingVolume bv = s.getWorldBound();
                float halfX, halfY, halfZ;
                Vector3f center;

                if (bv instanceof BoundingBox) {
                    BoundingBox wb = (BoundingBox) bv;
                    center = wb.getCenter().clone();
                    halfX  = Math.max(wb.getXExtent(), 0.1f);
                    halfY  = Math.max(wb.getYExtent(), 0.1f);
                    halfZ  = Math.max(wb.getZExtent(), 0.30f);
                } else {
                    center = s.getWorldTranslation().clone();
                    halfX  = 1.2f;
                    halfY  = 1.6f;
                    halfZ  = 0.30f;
                }

                // ── 1. Blocker physique (empêche de traverser) ────────────
                RigidBodyControl rbc = new RigidBodyControl(
                        new BoxCollisionShape(new Vector3f(halfX, halfY, halfZ)), 0f);
                s.addControl(rbc);
                bullet.getPhysicsSpace().add(rbc);
                g.rbc = rbc;

                // ── 2. View-blocker (empêche de voir la salle suivante) ───
                //    Plaquette opaque légèrement plus grande que le panel,
                //    positionnée au milieu de l'embrasure.
                //    On en crée deux (face avant + face arrière) pour bloquer
                //    la vue depuis les deux côtés.
                g.viewBlocker = creerViewBlocker(
                        "VB_" + g.numero,
                        center, halfX + 0.05f, halfY + 0.05f,
                        rootNode);

                System.out.printf(
                    "[DoorManager] Door_%s  physique + viewblocker @ (%.2f, %.2f, %.2f)"
                    + "  taille (%.2f x %.2f x %.2f)%n",
                    g.numero,
                    center.x, center.y, center.z,
                    halfX * 2, halfY * 2, halfZ * 2);
                return;
            }
            // stripe → pas de physique, pas de view-blocker
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update / Interaction
    // ─────────────────────────────────────────────────────────────────────────

    public boolean update(Vector3f posJoueur) {
        groupeProche = null;
        float meilleureDistSq = PORTEE * PORTEE;
        for (DoorGroup g : groupes) {
            if (g.ouverte) continue;
            float d2 = posJoueur.distanceSquared(g.panel.getWorldTranslation());
            if (d2 < meilleureDistSq) {
                meilleureDistSq = d2;
                groupeProche = g;
            }
        }
        return groupeProche != null;
    }

    public boolean interagir() {
        if (groupeProche == null) return false;
        System.out.println("[DoorManager] Ouverture Door_" + groupeProche.numero);
        groupeProche.ouvrir(bullet);
        groupeProche = null;
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée un nœud de deux géométries planes (face avant + face arrière)
     * qui bouchent visuellement l'embrasure depuis les deux côtés.
     */
    private Node creerViewBlocker(String nom,
                                  Vector3f centre,
                                  float halfX, float halfY,
                                  Node rootNode) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.80f, 0.78f, 0.72f, 1f)); // beige mur

        Node vbNode = new Node(nom);
        vbNode.setLocalTranslation(centre);

        // Face côté Room N (z - 0.02)
        Geometry face1 = new Geometry(nom + "_A", new Box(halfX, halfY, 0.01f));
        face1.setMaterial(mat);
        face1.setLocalTranslation(0, 0, -0.02f);
        vbNode.attachChild(face1);

        // Face côté Room N+1 (z + 0.02)
        Geometry face2 = new Geometry(nom + "_B", new Box(halfX, halfY, 0.01f));
        face2.setMaterial(mat);
        face2.setLocalTranslation(0, 0, 0.02f);
        vbNode.attachChild(face2);

        rootNode.attachChild(vbNode);
        return vbNode;
    }

    private void collecter(Spatial s,
                           Map<String, Spatial> panels,
                           Map<String, Spatial> stripes) {
        String nom   = s.getName() == null ? "" : s.getName();
        String lower = nom.toLowerCase();
        if (lower.matches("door_\\d+")) {
            panels.put(nom.substring(nom.lastIndexOf('_') + 1), s);
            return;
        }
        if (lower.matches("door_stripe_\\d+")) {
            stripes.put(nom.substring(nom.lastIndexOf('_') + 1), s);
            return;
        }
        if (s instanceof Node) {
            for (Spatial e : new ArrayList<>(((Node) s).getChildren())) {
                collecter(e, panels, stripes);
            }
        }
    }

    private void afficherHierarchie(Spatial s, int prof) {
        System.out.println("  ".repeat(prof) + s.getName()
            + " [" + s.getClass().getSimpleName() + "]");
        if (s instanceof Node) {
            for (Spatial e : ((Node) s).getChildren()) afficherHierarchie(e, prof + 1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static class DoorGroup {
        final String  numero;
        final Spatial panel;
        final Spatial stripe;
        RigidBodyControl rbc;
        Node             viewBlocker;
        boolean          ouverte = false;

        DoorGroup(String numero, Spatial panel, Spatial stripe) {
            this.numero = numero;
            this.panel  = panel;
            this.stripe = stripe;
        }

        void ouvrir(BulletAppState bullet) {
            ouverte = true;
            // Cacher le mesh de la porte
            panel.setCullHint(Spatial.CullHint.Always);
            if (stripe != null) stripe.setCullHint(Spatial.CullHint.Always);
            // Retirer la collision
            if (rbc != null) bullet.getPhysicsSpace().remove(rbc);
            // Retirer le view-blocker visuel
            if (viewBlocker != null) viewBlocker.setCullHint(Spatial.CullHint.Always);
        }
    }
}
