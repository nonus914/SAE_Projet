package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Écran du menu principal.
 * Appuyer sur Entrée pour jouer, Échap pour quitter.
 */
public class MenuState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private BitmapText titre;
    private BitmapText instrJouer;
    private BitmapText instrQuitter;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        if (name.equals("MenuJouer")) lancerJeu();
        if (name.equals("MenuQuitter")) app.stop();
    };

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        // Titre du jeu
        titre = new BitmapText(font, false);
        titre.setSize(font.getCharSet().getRenderedSize() * 3f);
        titre.setColor(ColorRGBA.Red);
        titre.setText("LAB-7");
        titre.setLocalTranslation(
            app.getCamera().getWidth() / 2f - titre.getLineWidth() / 2f,
            app.getCamera().getHeight() * 0.65f,
            0
        );
        guiNode.attachChild(titre);

        // Instruction jouer
        instrJouer = new BitmapText(font, false);
        instrJouer.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        instrJouer.setColor(ColorRGBA.White);
        instrJouer.setText("Appuyez sur ENTREE pour jouer");
        instrJouer.setLocalTranslation(
            app.getCamera().getWidth() / 2f - instrJouer.getLineWidth() / 2f,
            app.getCamera().getHeight() * 0.45f,
            0
        );
        guiNode.attachChild(instrJouer);

        // Instruction quitter
        instrQuitter = new BitmapText(font, false);
        instrQuitter.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        instrQuitter.setColor(ColorRGBA.Gray);
        instrQuitter.setText("ECHAP pour quitter");
        instrQuitter.setLocalTranslation(
            app.getCamera().getWidth() / 2f - instrQuitter.getLineWidth() / 2f,
            app.getCamera().getHeight() * 0.35f,
            0
        );
        guiNode.attachChild(instrQuitter);

        // Mapping touches
        app.getInputManager().addMapping("MenuJouer", new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("MenuQuitter", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, "MenuJouer", "MenuQuitter");
    }

    private void lancerJeu() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new GameState());
    }

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(titre);
        guiNode.detachChild(instrJouer);
        guiNode.detachChild(instrQuitter);
        app.getInputManager().removeListener(actionListener);
        app.getInputManager().deleteMapping("MenuJouer");
        app.getInputManager().deleteMapping("MenuQuitter");
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
