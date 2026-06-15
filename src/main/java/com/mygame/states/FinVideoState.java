package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * Video de fin (les deux issues) : sequence d'images 20 fps + audio OGG.
 * Jouee a la VICTOIRE, juste avant l'epilogue (FinIleState).
 * Skippable (ESPACE / clic / ECHAP). Le booleen "repare" est transmis a
 * l'epilogue pour afficher le bon texte.
 */
public class FinVideoState extends BaseAppState {

    private static final int   NB_FRAMES = 133;
    private static final float FPS       = 20f;
    private static final float DUREE     = NB_FRAMES / FPS; // ~6.65 s

    private final boolean repare;
    private SimpleApplication app;
    private Picture   ecran;
    private AudioNode audio;
    private float temps = 0f;
    private int   frameCourante = -1;
    private boolean termine = false;

    public FinVideoState(boolean repare) { this.repare = repare; }

    private final ActionListener skip = (name, isPressed, tpf) -> {
        if (isPressed && name.startsWith("FinVid")) terminer();
    };

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        // Plein ecran en conservant le ratio de la video 848x480 (letterbox)
        float scale = Math.min(W / 848f, H / 480f);
        float w = 848f * scale, h = 480f * scale;

        ecran = new Picture("FinVideo");
        ecran.setWidth(w);
        ecran.setHeight(h);
        ecran.setPosition((W - w) / 2f, (H - h) / 2f);
        afficherFrame(1);
        app.getGuiNode().attachChild(ecran);

        app.getViewPort().setBackgroundColor(ColorRGBA.Black);
        app.getInputManager().setCursorVisible(false);

        try {
            audio = new AudioNode(app.getAssetManager(), "Sounds/fin.ogg",
                    AudioData.DataType.Stream);
            audio.setPositional(false);
            audio.setLooping(false);
            audio.setVolume(4f);
            app.getRootNode().attachChild(audio);
            audio.play();
        } catch (Exception e) {
            audio = null;
            System.out.println("[Fin] Audio fin.ogg introuvable - video muette.");
        }

        app.getInputManager().addMapping("FinVidSkipSpace", new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addMapping("FinVidSkipEsc",   new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("FinVidSkipClic",  new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(skip,
                "FinVidSkipSpace", "FinVidSkipEsc", "FinVidSkipClic");
    }

    @Override
    public void update(float tpf) {
        if (termine) return;
        temps += tpf;
        if (temps >= DUREE) { terminer(); return; }
        int frame = Math.min(NB_FRAMES, (int) (temps * FPS) + 1);
        if (frame != frameCourante) afficherFrame(frame);
    }

    private void terminer() {
        if (termine) return;
        termine = true;
        app.getStateManager().detach(this);
        app.getStateManager().attach(new FinIleState(repare));
    }

    private void afficherFrame(int n) {
        frameCourante = n;
        try {
            TextureKey key = new TextureKey(
                    String.format("Video/fin/frame_%04d.jpg", n), true);
            key.setGenerateMips(false);
            Texture tex = app.getAssetManager().loadTexture(key);
            ecran.setTexture(app.getAssetManager(), (Texture2D) tex, false);
        } catch (Exception e) {
            temps = DUREE; // frame manquante -> on passe a l'epilogue
        }
    }

    @Override
    protected void cleanup(Application application) {
        if (audio != null) { audio.stop(); app.getRootNode().detachChild(audio); }
        app.getGuiNode().detachChild(ecran);
        app.getInputManager().removeListener(skip);
        for (String m : new String[]{"FinVidSkipSpace", "FinVidSkipEsc", "FinVidSkipClic"})
            if (app.getInputManager().hasMapping(m)) app.getInputManager().deleteMapping(m);
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
