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

        // (Le boitier digicode mural a ete supprime : le digicode s'ouvre
        //  directement en s'approchant de la porte verrouillee + touche E.)

        return doorManager;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Materiaux — style INDUSTRIEL BRUT (textures procedurales tuilables).
    //   • Murs/plafonds → beton banche use   • Sols → dalle beton usee
    //   • Poutres/piliers → acier peint rive • Tuyaux → metal rouille
    //   • Gaines → tole galva                • Portes → acier brosse
    //   • Neons/LED → couleur de LEUR SALLE + GlowColor (halo bloom)
    //   Teintes sombres + legere couleur de salle → l'ambiance neon est gardee.
    // ─────────────────────────────────────────────────────────────────────────

    private final java.util.Map<String, com.jme3.texture.Texture> texCache = new java.util.HashMap<>();
    private final java.util.Set<com.jme3.scene.Mesh> uvAjustes = new java.util.HashSet<>();

    private void corrigerMateriaux(Spatial s, AssetManager am) {
        if (s instanceof Geometry) {
            Geometry geo = (Geometry) s;
            geo.setMaterial(choisirMateriau(geo, am));
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

    /** Choisit le materiau d'une geometrie selon son nom / son materiau glTF. */
    private Material choisirMateriau(Geometry geo, AssetManager am) {
        Material src = geo.getMaterial();
        String gn = geo.getName() == null ? "" : geo.getName().toLowerCase();
        String mn = (src != null && src.getName() != null) ? src.getName().toLowerCase() : "";
        float z = geo.getWorldBound() != null
                ? geo.getWorldBound().getCenter().z
                : geo.getWorldTranslation().z;

        // 1. Neons / LED → couleur de LA SALLE + halo bloom.
        if (mn.contains("emit") || mn.contains("neon") || mn.contains("led")) {
            ColorRGBA col = couleurNeonSalle(z);
            Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", col);
            mat.setColor("GlowColor", col);
            return mat;
        }
        // 2. Ecrans → teinte d'origine + halo.
        if (mn.contains("screen")) {
            ColorRGBA c = couleurBase(src, new ColorRGBA(0f, 0.45f, 0.35f, 1f));
            Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", c);
            mat.setColor("GlowColor", c);
            return mat;
        }
        // 3. Signalisation peinte : vive, sans halo.
        if (mn.contains("warning") || mn.contains("hazard"))
            return unshaded(am, couleurBase(src, ColorRGBA.Yellow));
        // 4. Cables : noir mat.
        if (mn.contains("cable"))
            return unshaded(am, new ColorRGBA(0.05f, 0.05f, 0.06f, 1f));

        // ── Surfaces texturees ────────────────────────────────────────────────
        // AMBIANCE PAR SALLE : murs, sols, plafonds et structure baignent
        // FRANCHEMENT dans la couleur de leur salle (comme le rendu Blender).
        ColorRGBA salle  = couleurNeonSalle(z);
        ColorRGBA teinteMur = new ColorRGBA(0.26f + salle.r * 0.45f,
                                            0.26f + salle.g * 0.45f,
                                            0.26f + salle.b * 0.45f, 1f);
        ColorRGBA teinteSol = new ColorRGBA(0.34f + salle.r * 0.26f,
                                            0.34f + salle.g * 0.26f,
                                            0.34f + salle.b * 0.26f, 1f);
        ColorRGBA teintePlafond = new ColorRGBA(0.14f + salle.r * 0.16f,
                                                0.14f + salle.g * 0.16f,
                                                0.14f + salle.b * 0.16f, 1f);
        ColorRGBA teinteAcier = new ColorRGBA(0.30f + salle.r * 0.20f,
                                              0.30f + salle.g * 0.20f,
                                              0.30f + salle.b * 0.20f, 1f);

        if (gn.startsWith("floor") || mn.contains("wornfloor"))
            return texture(am, geo, "beton_sol.png", 3.0f, teinteSol);
        if (gn.startsWith("ceiling"))
            return texture(am, geo, "beton_mur.png", 3.0f, teintePlafond);
        if (gn.startsWith("wall") || mn.startsWith("bkm")
                || mn.contains("wallconcrete") || mn.contains("damagedwall"))
            return texture(am, geo, "beton_mur.png", 2.5f, teinteMur);
        if (gn.startsWith("pipe") || mn.contains("rusty") || mn.contains("pipeorange"))
            return texture(am, geo, "metal_rouille.png", 1.0f, new ColorRGBA(0.60f, 0.60f, 0.60f, 1f));
        if (gn.startsWith("duct") || gn.startsWith("vgrate") || mn.contains("vent"))
            return texture(am, geo, "tole_galva.png", 1.2f, new ColorRGBA(0.45f, 0.46f, 0.50f, 1f));
        if (gn.startsWith("door") || gn.startsWith("djamb") || gn.startsWith("dlintel")
                || gn.startsWith("dhaz") || gn.startsWith("exit") || mn.contains("doorframe"))
            return texture(am, geo, "metal_porte.png", 1.5f, new ColorRGBA(0.52f, 0.54f, 0.58f, 1f));
        if (gn.startsWith("skim_"))
            return unshaded(am, new ColorRGBA(0.10f, 0.10f, 0.12f, 1f)); // plinthe sombre
        // Structure metallique par defaut : poutres, piliers, supports, gantry...
        return texture(am, geo, "acier_peint.png", 1.5f, teinteAcier);
    }

    /** BaseColor du materiau glTF d'origine, ou couleur par defaut. */
    private static ColorRGBA couleurBase(Material src, ColorRGBA defaut) {
        if (src != null) {
            MatParam p = src.getParam("BaseColor");
            if (p != null && p.getValue() instanceof ColorRGBA) return (ColorRGBA) p.getValue();
        }
        return defaut;
    }

    private com.jme3.texture.Texture chargerTexture(AssetManager am, String nom) {
        return texCache.computeIfAbsent(nom, n -> {
            com.jme3.texture.Texture t = am.loadTexture("Textures/labo/" + n);
            t.setWrap(com.jme3.texture.Texture.WrapMode.Repeat);
            return t;
        });
    }

    /**
     * Materiau texture (Unshaded) avec repetition adaptee a la taille REELLE de
     * la surface : une tuile = tailleTuileM metres → pas de texture etiree.
     */
    private Material texture(AssetManager am, Geometry geo, String nom,
                             float tailleTuileM, ColorRGBA teinte) {
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", chargerTexture(am, nom));
        mat.setColor("Color", teinte); // multiplie la texture → ambiance sombre
        com.jme3.scene.Mesh mesh = geo.getMesh();
        if (mesh != null && uvAjustes.add(mesh)
                && geo.getWorldBound() instanceof com.jme3.bounding.BoundingBox) {
            com.jme3.bounding.BoundingBox bb = (com.jme3.bounding.BoundingBox) geo.getWorldBound();
            float[] dims = { bb.getXExtent() * 2f, bb.getYExtent() * 2f, bb.getZExtent() * 2f };
            java.util.Arrays.sort(dims);
            float su = Math.max(1f, Math.round(dims[2] / tailleTuileM));
            float sv = Math.max(1f, Math.round(dims[1] / tailleTuileM));
            try {
                mesh.scaleTextureCoordinates(new com.jme3.math.Vector2f(su, sv));
            } catch (Exception ignored) { /* mesh sans UV : texture etiree, acceptable */ }
        }
        return mat;
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
