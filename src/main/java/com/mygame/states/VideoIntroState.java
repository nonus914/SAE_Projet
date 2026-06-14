package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * Cinematique d'intro (video d'Aymen) : sequence d'images 20 fps + audio OGG.
 * Lancee AU DEMARRAGE du jeu (avant le menu). Pendant la lecture, le labo est
 * precharge en arriere-plan -> appuyer sur JOUER lance le jeu immediatement.
 */
public class VideoIntroState extends BaseAppState {

    private static final int   NB_FRAMES = 1055;
    private static final float FPS       = 20f;
    private static final float DUREE     = NB_FRAMES / FPS; // ~52.8 s

    private SimpleApplication app;
    private Picture   ecran;
    private AudioNode audio;
    private float temps = 0f;
    private int   frameCourante = -1;

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        // Plein ecran en conservant le ratio de la video 848x464 (letterbox)
        float scale = Math.min(W / 848f, H / 464f);
        float w = 848f * scale, h = 464f * scale;

        ecran = new Picture("VideoIntro");
        ecran.setWidth(w);
        ecran.setHeight(h);
        ecran.setPosition((W - w) / 2f, (H - h) / 2f);
        afficherFrame(1);
        app.getGuiNode().attachChild(ecran);

        app.getViewPort().setBackgroundColor(ColorRGBA.Black);
        app.getInputManager().setCursorVisible(false);

        // Son de la video — bien fort
        try {
            audio = new AudioNode(app.getAssetManager(), "Sounds/intro.ogg",
                    AudioData.DataType.Stream);
            audio.setPositional(false);
            audio.setLooping(false);
            audio.setVolume(4f);
            app.getRootNode().attachChild(audio);
            audio.play();
        } catch (Exception e) {
            audio = null;
            System.out.println("[Intro] Audio intro.ogg introuvable - video muette.");
        }

        // Prechargement du labo PENDANT la video -> zero attente a la fin
        new Thread(() -> {
            try {
                app.getAssetManager().loadModel("Models/room/labo_baked.glb");
                app.getAssetManager().loadModel("Models/props/robot_daniel.glb");
                app.getAssetManager().loadModel("Models/props/old_bed.glb");
                app.getAssetManager().loadModel("Models/player/arms_throwing.glb");
                app.getAssetManager().loadModel("Models/props/basic_generator.glb");
                app.getAssetManager().loadModel("Models/props/heavy_robot.glb");
                app.getAssetManager().loadModel("Models/props/island_capri.glb"); // ile de fin (evite le freeze)
                System.out.println("[Intro] Prechargement du labo termine.");
            } catch (Exception ignored) {}
        }, "PreloadLabo").start();
    }

    @Override
    public void update(float tpf) {
        temps += tpf;
        if (temps >= DUREE) {
            // Fin de la video -> le jeu commence tout de suite (labo precharge)
            app.getStateManager().detach(this);
            app.getStateManager().attach(new GameState());
            return;
        }
        int frame = Math.min(NB_FRAMES, (int) (temps * FPS) + 1);
        if (frame != frameCourante) afficherFrame(frame);
    }

    private void afficherFrame(int n) {
        frameCourante = n;
        try {
            TextureKey key = new TextureKey(
                    String.format("Video/intro/frame_%04d.jpg", n), true);
            key.setGenerateMips(false);
            Texture tex = app.getAssetManager().loadTexture(key);
            ecran.setTexture(app.getAssetManager(), (Texture2D) tex, false);
        } catch (Exception e) {
            temps = DUREE; // frame manquante -> on passe directement au jeu
        }
    }

    @Override
    protected void cleanup(Application application) {
        if (audio != null) { audio.stop(); app.getRootNode().detachChild(audio); }
        app.getGuiNode().detachChild(ecran);
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
