package com.mygame.npc;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * PNJ statique représenté par un bloc coloré.
 * Possède un set de lignes de dialogue.
 */
public class PNJ {

    private final Geometry geometrie;
    private final String[] lignesDialogue;
    private final String nom;

    public PNJ(String nom, Vector3f position, String[] lignes,
               AssetManager am, Node rootNode) {
        this.nom = nom;
        this.lignesDialogue = lignes;

        // Corps (bloc cyan)
        Box corps = new Box(0.3f, 0.9f, 0.3f);
        geometrie = new Geometry(nom, corps);
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.2f, 0.7f, 0.7f, 1f));
        geometrie.setMaterial(mat);
        geometrie.setLocalTranslation(position);
        geometrie.setUserData("type", "pnj");
        geometrie.setUserData("nom", nom);
        rootNode.attachChild(geometrie);

        // Tête (bloc plus clair au-dessus)
        Box tete = new Box(0.25f, 0.25f, 0.25f);
        Geometry geoTete = new Geometry(nom + "_tete", tete);
        Material matTete = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        matTete.setColor("Color", new ColorRGBA(0.9f, 0.75f, 0.6f, 1f));
        geoTete.setMaterial(matTete);
        geoTete.setLocalTranslation(position.add(0, 1.4f, 0));
        rootNode.attachChild(geoTete);
    }

    public String[] getLignesDialogue() { return lignesDialogue; }
    public Geometry getGeometrie()      { return geometrie; }
    public String getNom()              { return nom; }
}
