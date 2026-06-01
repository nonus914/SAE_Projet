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
import com.jme3.scene.Node;

/**
 * Écran de victoire — le joueur s'est échappé du laboratoire.
 */
public class WinState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private BitmapText texteVictoire;
    private BitmapText texteSous;
    private BitmapText texteOptions;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        if (name.equals("WinMenu")) retourMenu();
        if (name.equals("WinQuitter")) app.stop();
    };

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        float cx = app.getCamera().getWidth() / 2f;
        float cy = app.getCamera().getHeight();

        texteVictoire = new BitmapText(font, false);
        texteVictoire.setSize(font.getCharSet().getRenderedSize() * 3f);
        texteVictoire.setColor(ColorRGBA.Green);
        texteVictoire.setText("EVASION REUSSIE !");
        texteVictoire.setLocalTranslation(cx - texteVictoire.getLineWidth() / 2f, cy * 0.65f, 0);
        guiNode.attachChild(texteVictoire);

        texteSous = new BitmapText(font, false);
        texteSous.setSize(font.getCharSet().getRenderedSize() * 1.3f);
        texteSous.setColor(ColorRGBA.White);
        texteSous.setText("Vous avez reussi a vous echapper du LAB-7.");
        texteSous.setLocalTranslation(cx - texteSous.getLineWidth() / 2f, cy * 0.5f, 0);
        guiNode.attachChild(texteSous);

        texteOptions = new BitmapText(font, false);
        texteOptions.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        texteOptions.setColor(ColorRGBA.Gray);
        texteOptions.setText("M = Menu   ECHAP = Quitter");
        texteOptions.setLocalTranslation(cx - texteOptions.getLineWidth() / 2f, cy * 0.38f, 0);
        guiNode.attachChild(texteOptions);

        app.getInputManager().addMapping("WinMenu", new KeyTrigger(KeyInput.KEY_M));
        app.getInputManager().addMapping("WinQuitter", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, "WinMenu", "WinQuitter");

        app.getInputManager().setCursorVisible(true);
    }

    private void retourMenu() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new MenuState());
    }

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(texteVictoire);
        guiNode.detachChild(texteSous);
        guiNode.detachChild(texteOptions);
        app.getInputManager().removeListener(actionListener);
        app.getInputManager().deleteMapping("WinMenu");
        app.getInputManager().deleteMapping("WinQuitter");
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
