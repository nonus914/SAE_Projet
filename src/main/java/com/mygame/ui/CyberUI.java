package com.mygame.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Utilitaires graphiques pour les écrans cyber-néon (style Aymen).
 * Fond d'écran + cadre néon + textes stylisés.
 */
public final class CyberUI {

    // ── Palette ───────────────────────────────────────────────────────────────
    public static final ColorRGBA CYAN      = new ColorRGBA(0f, 1f, 1f, 1f);
    public static final ColorRGBA CYAN_DARK = new ColorRGBA(0f, 0.39f, 0.39f, 0.85f);
    public static final ColorRGBA BLANC     = ColorRGBA.White;
    public static final ColorRGBA GRIS      = new ColorRGBA(0.65f, 0.65f, 0.65f, 1f);

    private CyberUI() {}

    // ── Fond plein écran ──────────────────────────────────────────────────────

    /**
     * Crée un quad plein écran avec une texture de fond.
     * Si la texture ne se charge pas, utilise un fond noir.
     */
    public static Geometry fond(AssetManager am, int w, int h, String texPath) {
        Geometry bg = new Geometry("BG", new Quad(w, h));
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture tex = am.loadTexture(texPath);
            mat.setTexture("ColorMap", tex);
        } catch (Exception e) {
            mat.setColor("Color", new ColorRGBA(0.04f, 0.04f, 0.06f, 1f)); // fallback noir
        }
        bg.setMaterial(mat);
        bg.setLocalTranslation(0, 0, -1);
        return bg;
    }

    // ── Cadre néon avec coins lumineux ────────────────────────────────────────

    /**
     * Crée le cadre néon avec coins lumineux (style Aymen).
     * x, y = coin bas-gauche en coordonnées GuiNode.
     */
    public static Node cadreNeon(AssetManager am, float x, float y, float w, float h) {
        Node frame = new Node("CadreNeon");
        float cs = 28f; // taille des coins
        float ep = 2f;  // épaisseur du trait

        // Coins néon brillants
        // Haut Gauche
        frame.attachChild(barre(am, x,           y + h - ep, cs, ep));
        frame.attachChild(barre(am, x,           y + h - cs, ep, cs));
        // Haut Droite
        frame.attachChild(barre(am, x + w - cs,  y + h - ep, cs, ep));
        frame.attachChild(barre(am, x + w - ep,  y + h - cs, ep, cs));
        // Bas Gauche
        frame.attachChild(barre(am, x,           y,          cs, ep));
        frame.attachChild(barre(am, x,           y,          ep, cs));
        // Bas Droite
        frame.attachChild(barre(am, x + w - cs,  y,          cs, ep));
        frame.attachChild(barre(am, x + w - ep,  y,          ep, cs));

        // Contour fin sombre
        frame.attachChild(contourFin(am, x, y, w, h));

        return frame;
    }

    private static Geometry barre(AssetManager am, float x, float y, float w, float h) {
        Geometry g = new Geometry("b", new Quad(w, h));
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", CYAN);
        g.setMaterial(mat);
        g.setLocalTranslation(x, y, 2f);
        return g;
    }

    private static Geometry contourFin(AssetManager am, float x, float y, float w, float h) {
        // Fond semi-transparent derrière le cadre
        Geometry g = new Geometry("fondCadre", new Quad(w, h));
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.05f, 0.08f, 0.10f, 0.82f));
        mat.getAdditionalRenderState().setBlendMode(
            com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        g.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        g.setLocalTranslation(x, y, 0.5f);
        return g;
    }

    // ── Textes ────────────────────────────────────────────────────────────────

    public static BitmapText texte(BitmapFont font, String txt, float scale, ColorRGBA color) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(font.getCharSet().getRenderedSize() * scale);
        t.setColor(color);
        t.setText(txt);
        return t;
    }

    /** Centre horizontalement un texte dans l'écran. */
    public static void centrerX(BitmapText t, int screenW, float y, float z) {
        t.setLocalTranslation(screenW / 2f - t.getLineWidth() / 2f, y, z);
    }

    /** Ligne séparatrice (tirets cyan). */
    public static BitmapText separateur(BitmapFont font, int screenW) {
        BitmapText sep = texte(font,
            "─────────────────────────────────────────────",
            0.7f, CYAN_DARK);
        centrerX(sep, screenW, 0, 0);
        return sep;
    }
}
