package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.mygame.states.MenuState;

/**
 * Point d'entrée du jeu LAB-7.
 * Lance l'état Menu au démarrage.
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Subject 27");
        settings.setResolution(1280, 720);
        settings.setFrameRate(60);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);   // cache les stats de debug (FPS, triangles...)
        setDisplayFps(false);        // cache le compteur FPS
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        // Menu d'abord (JOUER / COMMANDES) ; JOUER lance la video d'intro,
        // pendant laquelle le labo se precharge -> le jeu demarre des la fin.
        stateManager.attach(new MenuState());
    }

    @Override
    public void simpleUpdate(float tpf) {}
}
