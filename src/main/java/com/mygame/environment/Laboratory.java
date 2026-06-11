package com.mygame.environment;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Laboratory {

    public DoorManager construire(AssetManager am, Node rootNode, BulletAppState bullet) {
        DoorManager doorManager = new DoorManager(bullet, am);

        Spatial laboModel = am.loadModel("Models/room/labo_baked.glb");
        supprimerControles(laboModel);
        listerEtSupprimerEscaliers(laboModel);

        rootNode.attachChild(laboModel);
        rootNode.updateGeometricState();

        // Convertir les materiaux PBR du glTF en Unshaded, en CONSERVANT la
        // texture bakee (murs) ou la couleur d'origine (metal, neons).
        corrigerMateriaux(laboModel, am);

        List<Spatial> portes = doorManager.detecterPortes(laboModel);

        List<Vector3f>   worldPos = new ArrayList<>();
        List<Quaternion> worldRot = new ArrayList<>();
        List<Vector3f>   worldScl = new ArrayList<>();

        for (Spatial porte : portes) {
            worldPos.add(porte.getWorldTranslation().clone());
            worldRot.add(porte.getWorldRotation().clone());
            worldScl.add(porte.getWorldScale().clone());
            porte.getParent().detachChild(porte);
        }

        CollisionShape shape = CollisionShapeFactory.createMeshShape(laboModel);
        RigidBodyControl rbc  = new RigidBodyControl(shape, 0f);
        laboModel.addControl(rbc);
        bullet.getPhysicsSpace().add(rbc);

        for (int i = 0; i < portes.size(); i++) {
            Spatial porte = portes.get(i);
            rootNode.attachChild(porte);
            porte.setLocalTranslation(worldPos.get(i));
            porte.setLocalRotation(worldRot.get(i));
            porte.setLocalScale(worldScl.get(i));
        }

        rootNode.updateGeometricState();

        for (Spatial porte : portes) {
            doorManager.enregistrerExtrait(porte, rootNode);
        }

        // Tableau enigme 1
        com.jme3.scene.shape.Quad quadTableau = new com.jme3.scene.shape.Quad(6.0f, 3.0f);
        Geometry objetTableau = new Geometry("Tableau_Indices", quadTableau);
        Material matTableau = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            com.jme3.texture.Texture texTableau = am.loadTexture("Textures/tableau_enigme.png");
            texTableau.setMagFilter(com.jme3.texture.Texture.MagFilter.Nearest);
            texTableau.setMinFilter(com.jme3.texture.Texture.MinFilter.NearestNoMipMaps);
            matTableau.setTexture("ColorMap", texTableau);
        } catch (Exception e) {
            matTableau.setColor("Color", ColorRGBA.White);
        }
        objetTableau.setMaterial(matTableau);
        objetTableau.setLocalTranslation(new Vector3f(2.0f, 1.5f, -3.0f));
        rootNode.attachChild(objetTableau);

        // Digicode salle 1
        com.jme3.scene.shape.Box boiteDigicode = new com.jme3.scene.shape.Box(0.1f, 0.15f, 0.05f);
        Geometry objetDigicode = new Geometry("Digicode_Interactif_Salle1", boiteDigicode);
        Material matDigicode = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matDigicode.setColor("Color", ColorRGBA.DarkGray);
        objetDigicode.setMaterial(matDigicode);
        objetDigicode.setLocalTranslation(new Vector3f(2.5f, 1.2f, -3.0f));
        RigidBodyControl rbcDigicode = new RigidBodyControl(
                CollisionShapeFactory.createBoxShape(objetDigicode), 0f);
        objetDigicode.addControl(rbcDigicode);
        bullet.getPhysicsSpace().add(rbcDigicode);
        rootNode.attachChild(objetDigicode);

        return doorManager;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Correction des materiaux — labo BAKED, ambiance sombre + code couleur salle.
    //   • Murs/sols bakes (BKM_*) → texture de Blender, legerement assombrie.
    //   • Neons / LED            → couleur de LEUR SALLE (R1 bleu fonce … R5 violet)
    //                              + GlowColor → halo via BloomFilter (GlowMode.Objects).
    //   • Metal / beton nu       → couleur du modele, bien assombrie.
    // ─────────────────────────────────────────────────────────────────────────

    private static final float BAKED_DIM = 0.60f; // assombrit les textures bakees (murs/sols)
    private static final float FLAT_DIM  = 0.30f; // assombrit le metal/beton sans texture

    private void corrigerMateriaux(Spatial s, AssetManager am) {
        if (s instanceof Geometry) {
            Geometry geo = (Geometry) s;
            // Z monde (centre de la bbox) → determine la salle.
            // Valide : appele apres updateGeometricState().
            float z = geo.getWorldBound() != null
                    ? geo.getWorldBound().getCenter().z
                    : geo.getWorldTranslation().z;
            geo.setMaterial(versUnshadedBaked(geo.getMaterial(), z, am));
        }
        if (s instanceof Node) {
            for (Spatial enfant : new ArrayList<>(((Node) s).getChildren())) {
                corrigerMateriaux(enfant, am);
            }
        }
    }

    /**
     * Couleur du neon selon la salle (position Z monde). Code couleur du jeu :
     *  R1 bleu fonce • R2 vert • R3 orange • R4 bleu clair • R5 violet.
     * Limites Z mesurees sur le modele (murs avant ~16/40/64/88, sols suivants).
     */
    private static ColorRGBA couleurNeonSalle(float z) {
        if (z <  22f) return new ColorRGBA(0.06f, 0.18f, 0.95f, 1f); // R1 bleu fonce
        if (z <  46f) return new ColorRGBA(0.12f, 0.95f, 0.25f, 1f); // R2 vert
        if (z <  70f) return new ColorRGBA(1.00f, 0.45f, 0.05f, 1f); // R3 orange
        if (z < 100f) return new ColorRGBA(0.30f, 0.80f, 1.00f, 1f); // R4 bleu clair
        return              new ColorRGBA(0.60f, 0.12f, 1.00f, 1f);  // R5 violet
    }

    /**
     * Materiau PBR (glTF) → Unshaded, en respectant l'ambiance sombre :
     *  • texture bakee → ColorMap assombri par BAKED_DIM
     *  • neon / LED    → couleur de la salle + GlowColor (halo)
     *  • ecran         → garde sa teinte + leger halo
     *  • metal / beton → couleur du modele assombrie par FLAT_DIM
     */
    private static Material versUnshadedBaked(Material src, float z, AssetManager am) {
        if (src != null) {
            // 1. Surface bakee : on garde la texture, assombrie pour le mood.
            MatParamTexture tex = src.getTextureParam("BaseColorMap");
            if (tex != null && tex.getTextureValue() != null) {
                Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setTexture("ColorMap", tex.getTextureValue());
                mat.setColor("Color", new ColorRGBA(BAKED_DIM, BAKED_DIM, BAKED_DIM, 1f));
                return mat;
            }
            MatParam base = src.getParam("BaseColor");
            if (base != null && base.getValue() instanceof ColorRGBA) {
                ColorRGBA c  = (ColorRGBA) base.getValue();
                String    mn = src.getName() == null ? "" : src.getName().toLowerCase();

                // 2. Neons / LED → couleur de LA SALLE + halo.
                if (mn.contains("emit") || mn.contains("neon") || mn.contains("led")) {
                    ColorRGBA col = couleurNeonSalle(z);
                    Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", col);
                    mat.setColor("GlowColor", col); // halo via BloomFilter (GlowMode.Objects)
                    return mat;
                }

                // 3. Ecrans → gardent leur teinte propre + leger halo.
                if (mn.contains("screen")) {
                    Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", c);
                    mat.setColor("GlowColor", c);
                    return mat;
                }

                // 4. Signalisation peinte (warning/hazard) : vive, sans halo.
                boolean vif = mn.contains("warning") || mn.contains("hazard");
                float k = vif ? 1.0f : FLAT_DIM; // reste : metal/beton assombri
                return unshaded(am, new ColorRGBA(c.r * k, c.g * k, c.b * k, c.a));
            }
        }
        return unshaded(am, new ColorRGBA(0.15f, 0.15f, 0.15f, 1f)); // fallback sombre
    }

    /** Materiau Unshaded : couleur exacte, independante de tout eclairage. */
    private static Material unshaded(AssetManager am, ColorRGBA color) {
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        return mat;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void listerEtSupprimerEscaliers(Spatial s) {
        String nom   = s.getName() == null ? "" : s.getName();
        String lower = nom.toLowerCase();
        System.out.println("[LAB MESH] " + nom + " (" + s.getClass().getSimpleName() + ")");

        boolean aSupprimer = !nom.isEmpty() && (
            lower.contains("stair") || lower.contains("escal") ||
            lower.contains("step")  || lower.contains("marche") ||
            lower.contains("ramp")  || lower.contains("slab") || lower.contains("deck")
        );
        if (aSupprimer) {
            if (s.getParent() != null) s.getParent().detachChild(s);
            else                       s.setCullHint(Spatial.CullHint.Always);
            System.out.println("[LAB] SUPPRIME : " + nom);
            return;
        }
        if (s instanceof Node) {
            for (Spatial enfant : new ArrayList<>(((Node) s).getChildren())) {
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
            for (Spatial enfant : ((Node) s).getChildren()) supprimerControles(enfant);
        }
    }
}
