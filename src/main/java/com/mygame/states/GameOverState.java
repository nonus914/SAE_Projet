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
 * Écran Game Over — batterie à 0 ou mort du joueur.
 */
public class GameOverState extends BaseAppState {

    private SimpleApplication app;
    private Node guiNode;
    private BitmapText texteGameOver;
    private BitmapText texteRejouer;

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        if (name.equals("GORejouer")) rejouer();
        if (name.equals("GOMenu")) retourMenu();
    };

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;
        this.guiNode = app.getGuiNode();

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        float cx = app.getCamera().getWidth() / 2f;
        float cy = app.getCamera().getHeight();

        texteGameOver = new BitmapText(font, false);
        texteGameOver.setSize(font.getCharSet().getRenderedSize() * 3f);
        texteGameOver.setColor(ColorRGBA.Red);
        texteGameOver.setText("GAME OVER");
        texteGameOver.setLocalTranslation(cx - texteGameOver.getLineWidth() / 2f, cy * 0.6f, 0);
        guiNode.attachChild(texteGameOver);

        texteRejouer = new BitmapText(font, false);
        texteRejouer.setSize(font.getCharSet().getRenderedSize() * 1.3f);
        texteRejouer.setColor(ColorRGBA.White);
        texteRejouer.setText("R = Rejouer   M = Menu principal");
        texteRejouer.setLocalTranslation(cx - texteRejouer.getLineWidth() / 2f, cy * 0.4f, 0);
        guiNode.attachChild(texteRejouer);

        app.getInputManager().addMapping("GORejouer", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addMapping("GOMenu", new KeyTrigger(KeyInput.KEY_M));
        app.getInputManager().addListener(actionListener, "GORejouer", "GOMenu");

        app.getInputManager().setCursorVisible(true);
    }

    private void rejouer() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new GameState());
    }

    private void retourMenu() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new MenuState());
    }

    @Override
    protected void cleanup(Application application) {
        guiNode.detachChild(texteGameOver);
        guiNode.detachChild(texteRejouer);
        app.getInputManager().removeListener(actionListener);
        app.getInputManager().deleteMapping("GORejouer");
        app.getInputManager().deleteMapping("GOMenu");
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
