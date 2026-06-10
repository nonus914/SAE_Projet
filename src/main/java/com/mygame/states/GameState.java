package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.mygame.environment.DoorManager;
import com.mygame.environment.Laboratory;
import com.mygame.player.BatterieManager;
import com.mygame.player.Inventory;
import com.mygame.player.PlayerControl;
import com.mygame.ui.HudManager;

import java.util.ArrayList;
import java.util.List;

public class GameState extends BaseAppState {

    private SimpleApplication app;
    private BulletAppState    bullet;
    private PlayerControl     joueur;
    private DoorManager       doorManager;
    private HudManager        hud;
    private BatterieManager   batterie;
    private Inventory         inventaire;

    private AmbientLight   ambiant;
    private DirectionalLight soleil;
    private boolean visionNocturne = false;

    private static final float AMBIENT_NORMAL = 15.0f;
    private final List<PointLight> pointLights = new ArrayList<>();

    // ── Piles ramassables (auto-contact) ─────────────────────────────────────
    private final Node pilesNode = new Node("Piles");

    // ── Objets ramassables (raycasting E key — système Noah) ─────────────────
    private final Node objetsNode = new Node("Objets");

    // ── Enigmes — Digicodes ───────────────────────────────────────────────────
    private static final String CODE_E1       = "1953"; // enigme 1 (Noah)
    private static final String CODE_E3       = "2703"; // enigme 3 (charade)
    private String porteCibleDigicode         = "";     // "001" ou "003"
    private static final float  ROOM3_Z_MIN   = 43f;  // R3 demarre a Z=40 — decalage de 3m
    private static final float  ROOM3_Z_MAX   = 88f;  // sombre jusqu'a la fin de R4 (R4 exit = Z=88)
    private boolean enigme3Resolue    = false;
    private String  codeEntree        = "";
    private float   digiCloseTimer    = -1f; // >0 = compte a rebours avant fermeture

    // ── Enigme 2 — Robot Daniel + Clé à molette (salle 2) ───────────────────
    private static final Vector3f POS_DANIEL = new Vector3f( 3.0f, 1.0f, 25.0f); // R2 elargie : Z=14 → 25
    // Cle a molette : au sol devant le rack mural (Y=0.2), dans l'empreinte X du rack
    // Visible & ramassable SEULEMENT en position accroupie (camera ~1.85m vs ~2.55m debout)
    private static final Vector3f POS_CLE    = new Vector3f(-8.6f, 0.2f, 36.0f); // R2 elargie : Z=21.5 → 36
    // NOTE : police bitmap ASCII — pas d'accents (e accent = blanc, c cedille = absent)
    private static final String[] DIALOGUES_DANIEL = {
        "Hey ! Enfin quelqu'un ! Je suis bloque dans ce labo depuis des heures !",
        "La porte suivante est cassee. Il me faut une cle a molette pour la reparer.",
        "J'en ai vu une sous le rack au fond a gauche. Tu dois t'accroupir pour l'attraper !"
    };
    private Spatial spatialDaniel      = null;
    private Spatial spatialCle         = null;
    private boolean enigme2CleRamassee = false;
    private boolean enigme2Resolue     = false;
    private int     enigme2DiagIdx     = 0;

    private static final String CHARADE =                 // ASCII pur (bitmap font)
        "=== CHARADE - PORTE SALLE 3 ===\n" +
        "\n" +
        "Mon premier est le numero du Sujet.\n" +
        "Mon second est le niveau de batterie\n" +
        "  qui provoque le Game Over.\n" +
        "Mon troisieme est le numero de la salle.\n" +
        "\n" +
        "Mon tout est le code du Digicode.\n" +
        "\n" +
        "[Approchez la porte et appuyez sur E]";

    private final ActionListener actionListener = this::onAction;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void initialize(Application application) {
        this.app = (SimpleApplication) application;

        bullet = new BulletAppState();
        app.getStateManager().attach(bullet);

        // Lumières
        ambiant = new AmbientLight();
        ambiant.setColor(ColorRGBA.White.mult(AMBIENT_NORMAL));
        app.getRootNode().addLight(ambiant);

        soleil = new DirectionalLight();
        soleil.setDirection(new Vector3f(0f, -1f, 0.2f).normalizeLocal());
        soleil.setColor(ColorRGBA.White.mult(0.8f));
        app.getRootNode().addLight(soleil);

        // Point lights d'ambiance par salle (pas en room 3 = salle sombre)
        ajouterLumiereAmbiante(new Vector3f( 3f, 2.5f, 10.0f), new ColorRGBA(1.0f, 0.85f, 0.6f, 1f), 5f, 18f); // R1 chaud
        ajouterLumiereAmbiante(new Vector3f(-3f, 2.5f, 14.5f), new ColorRGBA(0.5f, 0.8f,  1.0f, 1f), 4f, 18f); // R1 froid
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 28.0f), new ColorRGBA(0.3f, 0.6f,  1.0f, 1f), 6f, 22f); // R2 bleu
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 37.0f), new ColorRGBA(0.4f, 1.0f,  0.5f, 1f), 4f, 16f); // R2 vert
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 76.0f), new ColorRGBA(0.2f, 0.9f,  1.0f, 1f), 6f, 28f); // R4 cyan
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f,112.0f), new ColorRGBA(0.6f, 0.2f,  1.0f, 1f), 6f, 28f); // R5 violet

        // Labo + portes
        doorManager = new Laboratory().construire(app.getAssetManager(), app.getRootNode(), bullet);

        // Générateur — room 4, mur gauche, juste après porte 003
        // Dimensions JME : 0.855m (X) × 1.084m (Y=hauteur) × 0.732m (Z) — origine au centre
        // Scale 1.5 → 1.28m × 1.63m × 1.10m — posé sur le sol (Y = demi-hauteur × scale)
        Spatial generator = app.getAssetManager().loadModel("Models/props/basic_generator.glb");
        generator.setLocalScale(1.5f);
        generator.setLocalTranslation(-9.0f, 0.813f, 68.5f); // R4 elargie : Z=43 → 68.5
        app.getRootNode().attachChild(generator);

        // Mains du joueur
        Node nodeCamera = new Node("nodeCamera");
        app.getRootNode().attachChild(nodeCamera);
        Spatial hands = app.getAssetManager().loadModel("Models/player/arms_throwing.glb");
        hands.rotate(0.2f, 0, 0);
        hands.setLocalScale(0.04f);
        nodeCamera.attachChild(hands);

        joueur   = new PlayerControl(bullet, app.getCamera(), nodeCamera, hands);
        batterie = new BatterieManager(600f);
        inventaire = new Inventory();

        // Piles (auto-contact)
        app.getRootNode().attachChild(pilesNode);
        placerPile(new Vector3f( 2f, 1f, 11.5f)); // R1 elargie : Z=5 → 11.5
        placerPile(new Vector3f(-2f, 1f, 34.0f)); // R2 elargie : Z=20 → 34
        placerPile(new Vector3f( 1f, 1f, 58.0f)); // R3 elargie : Z=36 → 58

        // Objets ramassables (E key + raycasting, marqués "Ramassable")
        app.getRootNode().attachChild(objetsNode);
        placerObjet("Cle Salle 1",       new Vector3f(-1f, 1f, 13.0f), ColorRGBA.Yellow);            // R1→13
        placerObjet("Badge Acces",        new Vector3f( 1f, 1f, 37.0f), new ColorRGBA(0.2f, 0.8f, 1f, 1f)); // R2→37
        placerObjet("Carte Acces Rouge",  new Vector3f( 0f, 1f, 61.0f), ColorRGBA.Red);              // R3→61

        // HUD
        hud = new HudManager(
            app.getAssetManager(),
            app.getGuiNode(),
            app.getCamera().getWidth(),
            app.getCamera().getHeight()
        );

        // Caméra FPS
        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setMoveSpeed(0f);
        app.getFlyByCamera().setRotationSpeed(2f);
        app.getInputManager().setCursorVisible(false);

        // Verrouiller les portes à code
        doorManager.verrouiller("001"); // enigme 1 — Noah
        doorManager.verrouiller("002"); // enigme 2 — clé à molette
        doorManager.verrouiller("003"); // enigme 3 — charade

        // ── Enigme 2 — Robot Daniel (PNJ salle 2) ────────────────────────────
        spatialDaniel = app.getAssetManager().loadModel("Models/props/robot_daniel.glb");
        spatialDaniel.setName("Daniel_Robot");
        spatialDaniel.setLocalScale(1.8f);
        // Face a l'entree de la salle 2 (joueur arrive de Z=16 vers Z=40)
        // Rotation 180° autour de Y → regarde vers -Z = vers le joueur qui entre
        spatialDaniel.rotate(0f, FastMath.PI, 0f);
        spatialDaniel.setLocalTranslation(POS_DANIEL.x, 0f, POS_DANIEL.z);
        app.getRootNode().attachChild(spatialDaniel);
        System.out.println("[Enigme2] Daniel chargé. Bounds : " + spatialDaniel.getWorldBound());

        // ── Enigme 2 — Clé à molette (cachée derrière l'armoire, salle 2) ────
        spatialCle = app.getAssetManager().loadModel("Models/props/cle_molette.glb");
        spatialCle.setName("Cle_Molette");
        // Le modele Sketchfab est en cm → xExtent=11.95 a scale=0.8 = 24m.
        // scale=0.02 → xExtent≈0.22m, longueur totale ≈0.45m (grande cle reglable) ✓
        spatialCle.setLocalScale(0.02f);
        // Sol sous le rack (Y=0.2) — seulement visible en s'accroupissant
        spatialCle.setLocalTranslation(POS_CLE.x, POS_CLE.y, POS_CLE.z);
        app.getRootNode().attachChild(spatialCle);
        System.out.println("[Enigme2] Clé placée à Y=" + POS_CLE.y + ". Bounds : " + spatialCle.getWorldBound());

        // ── Mobilier salle 2 (géométries Java) ───────────────────────────────
        creerMeublesRoom2();

        enregistrerTouches();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers placement
    // ─────────────────────────────────────────────────────────────────────────

    /** Objet ramassable via raycasting (touche E). */
    private void placerObjet(String nom, Vector3f pos, ColorRGBA couleur) {
        Geometry g = new Geometry(nom, new Box(0.1f, 0.06f, 0.15f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        g.setMaterial(mat);
        g.setLocalTranslation(pos);
        g.setUserData("Ramassable", true);   // marque comme ramassable (système Noah)
        objetsNode.attachChild(g);
    }

    /** Pile (recharge batterie) ramassée automatiquement au contact. */
    private void placerPile(Vector3f pos) {
        Geometry pile = new Geometry("Pile_" + pilesNode.getQuantity(),
                                     new Cylinder(20, 20, 0.15f, 0.4f, true));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        pile.setMaterial(mat);
        pile.rotate(FastMath.HALF_PI, 0, 0);
        pile.setLocalTranslation(pos);
        pilesNode.attachChild(pile);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Touches
    // ─────────────────────────────────────────────────────────────────────────

    private void enregistrerTouches() {
        app.getInputManager().addMapping("Avancer",         new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("Reculer",         new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("Gauche",          new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("Droite",          new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("Sprint",          new KeyTrigger(KeyInput.KEY_LSHIFT));
        app.getInputManager().addMapping("Accroupir",       new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addMapping("VisionNuit",      new KeyTrigger(KeyInput.KEY_N));
        app.getInputManager().addMapping("Interagir",       new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping("Sauter",          new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addMapping("VoirInventaire",  new KeyTrigger(KeyInput.KEY_I));

        // Chiffres : rangee du haut + pave numerique
        int[][] numKeys = {
            {KeyInput.KEY_0, KeyInput.KEY_NUMPAD0},
            {KeyInput.KEY_1, KeyInput.KEY_NUMPAD1},
            {KeyInput.KEY_2, KeyInput.KEY_NUMPAD2},
            {KeyInput.KEY_3, KeyInput.KEY_NUMPAD3},
            {KeyInput.KEY_4, KeyInput.KEY_NUMPAD4},
            {KeyInput.KEY_5, KeyInput.KEY_NUMPAD5},
            {KeyInput.KEY_6, KeyInput.KEY_NUMPAD6},
            {KeyInput.KEY_7, KeyInput.KEY_NUMPAD7},
            {KeyInput.KEY_8, KeyInput.KEY_NUMPAD8},
            {KeyInput.KEY_9, KeyInput.KEY_NUMPAD9},
        };
        for (int i = 0; i <= 9; i++) {
            app.getInputManager().addMapping("Digi" + i,
                new KeyTrigger(numKeys[i][0]),
                new KeyTrigger(numKeys[i][1]));
        }
        app.getInputManager().addMapping("DigiEffacer", new KeyTrigger(KeyInput.KEY_BACK));
        app.getInputManager().addMapping("DigiValider",  new KeyTrigger(KeyInput.KEY_RETURN),
                                                         new KeyTrigger(KeyInput.KEY_NUMPADENTER));
        app.getInputManager().addMapping("DigiFermer",   new KeyTrigger(KeyInput.KEY_ESCAPE));

        app.getInputManager().addListener(actionListener,
                "Avancer","Reculer","Gauche","Droite",
                "Sprint","Accroupir","VisionNuit","Interagir","Sauter","VoirInventaire",
                "Digi0","Digi1","Digi2","Digi3","Digi4",
                "Digi5","Digi6","Digi7","Digi8","Digi9",
                "DigiEffacer","DigiValider","DigiFermer");
    }

    private void onAction(String name, boolean isPressed, float tpf) {
        // Digicode intercept — si le panneau est ouvert, bloquer les autres actions
        if (hud.isDigicodeOuvert()) {
            if (!isPressed) return;
            if (name.startsWith("Digi")) {
                if (name.equals("DigiEffacer")) {
                    if (!codeEntree.isEmpty()) {
                        codeEntree = codeEntree.substring(0, codeEntree.length() - 1);
                        hud.updateDigicode(codeEntree);
                        hud.setFeedbackDigicode("", ColorRGBA.White);
                    }
                } else if (name.equals("DigiValider")) {
                    validerCodeDigicode();
                } else if (name.equals("DigiFermer")) {
                    hud.fermerDigicode();
                    codeEntree = "";
                } else {
                    // Chiffre 0-9
                    String chiffre = name.replace("Digi", "");
                    if (codeEntree.length() < 4) {
                        codeEntree += chiffre;
                        System.out.println("[Digicode] Saisie : " + codeEntree);
                        hud.updateDigicode(codeEntree);
                        if (codeEntree.length() == 4) validerCodeDigicode();
                    }
                }
            }
            return; // bloquer tout le reste
        }

        switch (name) {
            case "Avancer"        -> joueur.setForward(isPressed);
            case "Reculer"        -> joueur.setBackward(isPressed);
            case "Gauche"         -> joueur.setLeft(isPressed);
            case "Droite"         -> joueur.setRight(isPressed);
            case "Sprint"         -> { joueur.setSprint(isPressed); batterie.setSprint(isPressed); }
            case "Accroupir"      -> { if (isPressed) joueur.setCrouch(!joueur.isCrouch()); }
            case "VisionNuit"     -> { if (isPressed) basculerVisionNuit(); }
            case "Sauter"         -> { if (isPressed) joueur.sauter(); }
            case "VoirInventaire" -> { if (isPressed) hud.toggleInventaire(inventaire.getObjets()); }
            case "Interagir"      -> {
                if (isPressed) {
                    // 0. Dialogue NPC en cours → avancer / fermer
                    if (hud.isDialogueOuvert()) {
                        avancerDialogueEnigme2();
                        return;
                    }

                    // 1. Raycast — boîtier digicode salle 1 (Noah)
                    CollisionResults resultats = new CollisionResults();
                    Ray rayon = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
                    app.getRootNode().collideWith(rayon, resultats);
                    if (resultats.size() > 0 && resultats.getClosestCollision().getDistance() < 3.0f) {
                        String nomCible = resultats.getClosestCollision().getGeometry().getName();
                        if ("Digicode_Interactif_Salle1".equals(nomCible)) {
                            porteCibleDigicode = "001";
                            codeEntree = "";
                            hud.ouvrirDigicode();
                            hud.updateDigicode("");
                            app.getInputManager().setCursorVisible(false);
                            System.out.println("[Digicode] Salle 1. Tapez " + CODE_E1);
                            return;
                        }
                    }

                    // 2. Enigme 2 — Ramasser la clé à molette (proximité + accroupi obligatoire)
                    if (!enigme2CleRamassee && spatialCle != null) {
                        Vector3f pp = joueur.getCharacterControl().getPhysicsLocation();
                        if (pp.distanceSquared(POS_CLE) < 2.5f * 2.5f) {
                            if (joueur.isCrouch()) {
                                enigme2CleRamassee = true;
                                spatialCle.removeFromParent();
                                spatialCle = null;
                                inventaire.ajouter("Cle a molette");
                                hud.setMessageInteraction("Cle a molette trouvee !");
                                System.out.println("[Enigme2] Cle a molette ramassee !");
                            } else {
                                // Trop haut debout — signaler qu'il faut s'accroupir
                                hud.setMessageInteraction("[C] S'accroupir pour attraper la cle !");
                            }
                            return;
                        }
                    }

                    // 3. Enigme 2 — Parler à Daniel (proximité)
                    if (spatialDaniel != null) {
                        Vector3f pp = joueur.getCharacterControl().getPhysicsLocation();
                        if (pp.distanceSquared(POS_DANIEL) < 3.5f * 3.5f) {
                            enigme2DiagIdx = 0;
                            hud.ouvrirDialogue("Daniel", DIALOGUES_DANIEL[0]);
                            System.out.println("[Enigme2] Dialogue avec Daniel commencé");
                            return;
                        }
                    }

                    // 4. Porte verrouillee par proximité
                    if (doorManager.isPorteProcheVerrouillee()) {
                        String numPorte = doorManager.getNumeroPorteProche();
                        if ("002".equals(numPorte)) {
                            // Enigme 2 — réparer avec la clé à molette
                            if (enigme2CleRamassee) {
                                enigme2Resolue = true;
                                doorManager.deverrouiller("002");
                                hud.setMessageInteraction("Porte reparee avec la cle a molette !");
                                System.out.println("[Enigme2] Porte 002 deverrouillee !");
                            } else {
                                hud.setMessageInteraction("[!] Il faut un outil pour reparer la porte...");
                            }
                        } else {
                            // Enigme 1 / 3 — digicode
                            porteCibleDigicode = numPorte;
                            codeEntree = "";
                            hud.ouvrirDigicode();
                            hud.updateDigicode("");
                            app.getInputManager().setCursorVisible(false);
                            System.out.println("[Digicode] Porte " + numPorte + ". Tapez le code.");
                        }
                    } else {
                        // 5. Ouvrir porte normale (non verrouillée)
                        boolean portOuverte = doorManager.interagir();
                        // 6. Sinon ramasser objet (raycasting)
                        if (!portOuverte) tenterDeRamasser();
                    }
                }
            }
        }
    }

    private void validerCodeDigicode() {
        // Choisir le bon code selon la porte ciblée
        String bonCode = "001".equals(porteCibleDigicode) ? CODE_E1 : CODE_E3;

        if (codeEntree.equals(bonCode)) {
            hud.setFeedbackDigicode("CODE ACCEPTE !", ColorRGBA.Green);
            doorManager.deverrouiller(porteCibleDigicode);

            if ("003".equals(porteCibleDigicode)) {
                // Enigme 3 résolue → rétablir l'éclairage, cacher la charade
                enigme3Resolue = true;
                hud.setCharade("");
                if (!visionNocturne) {
                    ambiant.setColor(ColorRGBA.White.mult(AMBIENT_NORMAL));
                }
            }
            digiCloseTimer = 1.2f;
        } else {
            hud.setFeedbackDigicode("CODE INCORRECT !", ColorRGBA.Red);
            codeEntree = "";
            hud.updateDigicode("");
        }
    }

    // ── Enigme 2 — Dialogue Daniel ────────────────────────────────────────────

    private void avancerDialogueEnigme2() {
        enigme2DiagIdx++;
        if (enigme2DiagIdx < DIALOGUES_DANIEL.length) {
            hud.setLigneDialogue("Daniel", DIALOGUES_DANIEL[enigme2DiagIdx]);
        } else {
            enigme2DiagIdx = 0;
            hud.fermerDialogue();
        }
    }

    // ── Enigme 2 — Mobilier salle 2 ──────────────────────────────────────────

    /**
     * Cree les meubles de la salle 2 (geometries Java simples).
     * Room 2 : JME Z ≈ 16 a 40, X ≈ -10 a 10, sol Y = 0.  (apres scaling x1.5)
     * Formule : new_Z = 4 + 1.5 * old_Z
     */
    private void creerMeublesRoom2() {
        // Tables de labo (mur gauche + mur droit)
        creerMeuble("Table_Labo1_R2",
            new Vector3f(-7.5f, 0.48f, 21.0f), new Vector3f(1.8f, 0.48f, 0.8f),  // Z:11.5→21.0
            new ColorRGBA(0.22f, 0.25f, 0.28f, 1f));
        creerMeuble("Table_Labo2_R2",
            new Vector3f( 7.5f, 0.48f, 29.0f), new Vector3f(1.8f, 0.48f, 0.8f),  // Z:16.5→29.0
            new ColorRGBA(0.22f, 0.25f, 0.28f, 1f));

        // Caisses sur table gauche
        creerMeuble("Caisse1_R2",
            new Vector3f(-7.5f, 1.34f, 20.5f), new Vector3f(0.34f, 0.34f, 0.34f), // Z:11.0→20.5
            new ColorRGBA(0.40f, 0.34f, 0.20f, 1f));
        creerMeuble("Caisse2_R2",
            new Vector3f(-6.8f, 1.34f, 22.0f), new Vector3f(0.28f, 0.28f, 0.28f), // Z:12.1→22.0
            new ColorRGBA(0.36f, 0.31f, 0.18f, 1f));

        // Petit ecran sur table droite (flat panel labo)
        creerMeuble("Ecran_R2",
            new Vector3f( 7.5f, 1.52f, 28.0f), new Vector3f(0.48f, 0.38f, 0.05f), // Z:16.0→28.0
            new ColorRGBA(0.07f, 0.07f, 0.10f, 1f));

        // Rack mural sci-fi (mur gauche, coin fond) — Java geometry, zero GLB
        // Cle a molette au sol en dessous (POS_CLE) — visible seulement accroupi
        creerRackMural(-9.0f, 37.0f); // Z:22.0→37.0

        // Caisses au sol (milieu salle)
        creerMeuble("Caisse_Sol1_R2",
            new Vector3f(-2.1f, 0.42f, 30.5f), new Vector3f(0.55f, 0.42f, 0.55f), // Z:17.5→30.5
            new ColorRGBA(0.40f, 0.34f, 0.20f, 1f));
        creerMeuble("Caisse_Sol2_R2",
            new Vector3f(-2.9f, 0.42f, 30.5f), new Vector3f(0.55f, 0.42f, 0.55f), // Z:17.5→30.5
            new ColorRGBA(0.36f, 0.31f, 0.18f, 1f));

        // Etagere murale (mur droit, entree salle)
        creerMeuble("Etagere_R2",
            new Vector3f( 9.3f, 1.80f, 20.0f), new Vector3f(0.15f, 0.80f, 2.0f),  // Z:10.5→20.0
            new ColorRGBA(0.20f, 0.22f, 0.26f, 1f));

        // Pilier support sci-fi (milieu salle)
        creerMeuble("Pilier1_R2",
            new Vector3f( 0.0f, 1.50f, 23.5f), new Vector3f(0.20f, 1.50f, 0.20f), // Z:13.0→23.5
            new ColorRGBA(0.30f, 0.32f, 0.36f, 1f));

        // Etabli (mur du fond salle 2, face a l'entree de la salle 3)
        creerMeuble("Etabli_R2",
            new Vector3f( 4.0f, 0.48f, 38.5f), new Vector3f(1.5f, 0.48f, 0.6f),   // Z:23.0→38.5
            new ColorRGBA(0.25f, 0.27f, 0.30f, 1f));

        System.out.println("[Enigme2] Mobilier salle 2 cree (rack mural Java + 9 elements).");
    }

    /**
     * Rack mural sci-fi — 13 geometries Java (zero GLB, zero temps de chargement).
     * Centre a (x, 2.2f, z). Bas : 0.95m — cle a molette au sol en dessous.
     * Face visible : direction -Z (vers le joueur qui arrive de la salle 1).
     * Dimensions : 1.20m large x 2.50m haut x 0.36m profond.
     */
    private void creerRackMural(float x, float z) {
        // == Boitier principal (anthracite sombre) ==
        creerMeuble("Rack_Corps",
            new Vector3f(x, 2.2f, z),
            new Vector3f(0.60f, 1.25f, 0.18f),
            new ColorRGBA(0.11f, 0.13f, 0.15f, 1f));

        // == 4 bays d'equipement (face avant, alternance claire/sombre) ==
        float[] baysY = { 3.05f, 2.75f, 2.45f, 2.15f };
        ColorRGBA[] baysCol = {
            new ColorRGBA(0.21f, 0.24f, 0.28f, 1f),
            new ColorRGBA(0.16f, 0.18f, 0.21f, 1f),
            new ColorRGBA(0.21f, 0.24f, 0.28f, 1f),
            new ColorRGBA(0.16f, 0.18f, 0.21f, 1f)
        };
        for (int i = 0; i < 4; i++) {
            creerMeuble("Rack_Bay" + i,
                new Vector3f(x, baysY[i], z - 0.19f),
                new Vector3f(0.54f, 0.09f, 0.015f),
                baysCol[i]);
        }

        // == LEDs de statut (une par bay — cyan, rouge, vert, ambre) ==
        float ledX = x + 0.46f;
        float ledZ  = z - 0.21f;
        ColorRGBA[] ledCols = {
            new ColorRGBA(0.0f, 1.0f, 1.0f, 1f),   // cyan  : actif
            new ColorRGBA(1.0f, 0.1f, 0.1f, 1f),   // rouge : erreur
            new ColorRGBA(0.1f, 1.0f, 0.1f, 1f),   // vert  : ok
            new ColorRGBA(1.0f, 0.5f, 0.0f, 1f)    // ambre : alerte
        };
        for (int i = 0; i < 4; i++) {
            creerMeuble("Rack_LED" + i,
                new Vector3f(ledX, baysY[i], ledZ),
                new Vector3f(0.022f, 0.022f, 0.01f),
                ledCols[i]);
        }

        // == Bandeau etiquette (haut, gris metal) ==
        creerMeuble("Rack_Label",
            new Vector3f(x, 3.36f, z - 0.19f),
            new Vector3f(0.54f, 0.055f, 0.01f),
            new ColorRGBA(0.52f, 0.57f, 0.62f, 1f));

        // == Grille de ventilation (bas chassis) ==
        creerMeuble("Rack_Ventil",
            new Vector3f(x, 1.08f, z - 0.19f),
            new Vector3f(0.54f, 0.04f, 0.01f),
            new ColorRGBA(0.08f, 0.09f, 0.11f, 1f));

        // == Equerres de support mural (bas gauche + droit) ==
        creerMeuble("Rack_Eq_G",
            new Vector3f(x - 0.52f, 1.05f, z + 0.04f),
            new Vector3f(0.04f, 0.12f, 0.12f),
            new ColorRGBA(0.13f, 0.15f, 0.17f, 1f));
        creerMeuble("Rack_Eq_D",
            new Vector3f(x + 0.52f, 1.05f, z + 0.04f),
            new Vector3f(0.04f, 0.12f, 0.12f),
            new ColorRGBA(0.13f, 0.15f, 0.17f, 1f));

        System.out.println("[Enigme2] Rack mural Java cree (13 elements, 0 GLB).");
    }

    /**
     * Crée un meuble stylisé (Box Geometry, matériau Unshaded).
     * @param demi  demi-dimensions (halfExtent) en X, Y, Z
     */
    private void creerMeuble(String nom, Vector3f pos, Vector3f demi, ColorRGBA couleur) {
        Geometry g = new Geometry(nom, new Box(demi.x, demi.y, demi.z));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        g.setMaterial(mat);
        g.setLocalTranslation(pos);
        app.getRootNode().attachChild(g);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void ajouterLumiereAmbiante(Vector3f pos, ColorRGBA couleur, float intensite, float rayon) {
        PointLight pl = new PointLight();
        pl.setPosition(pos);
        pl.setColor(couleur.mult(intensite));
        pl.setRadius(rayon);
        app.getRootNode().addLight(pl);
        pointLights.add(pl);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Raycasting (système Noah)
    // ─────────────────────────────────────────────────────────────────────────

    private void tenterDeRamasser() {
        CollisionResults resultats = new CollisionResults();
        Ray rayon = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
        app.getRootNode().collideWith(rayon, resultats);

        if (resultats.size() > 0) {
            Geometry cible    = resultats.getClosestCollision().getGeometry();
            float    distance = resultats.getClosestCollision().getDistance();

            if (distance < 3.0f && Boolean.TRUE.equals(cible.getUserData("Ramassable"))) {
                String nom = cible.getName();
                if (inventaire.ajouter(nom)) {
                    cible.removeFromParent();
                    hud.setMessageInteraction("Ramasse : " + nom);
                    System.out.println("[GameState] Ramasse : " + nom);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void update(float tpf) {
        if (!isEnabled()) return;
        joueur.update(tpf);

        Vector3f pos = joueur.getCharacterControl().getPhysicsLocation();

        // Batterie
        batterie.update(tpf);
        hud.updateBatterie(batterie.getPourcentage());

        if (batterie.isGameOver()) {
            app.getStateManager().detach(this);
            app.getRootNode().detachAllChildren();
            app.getStateManager().attach(new GameOverState());
        }

        // Piles au contact
        for (int i = pilesNode.getQuantity() - 1; i >= 0; i--) {
            Spatial pile = pilesNode.getChild(i);
            if (pos.distanceSquared(pile.getWorldTranslation()) < 2f * 2f) {
                batterie.rechargerAFond();
                pile.removeFromParent();
                hud.setMessageInteraction("Pile ramassee ! Batterie rechargee !");
            }
        }

        // ── Timer fermeture digicode (JME-safe, pas de Thread) ───────────────
        if (digiCloseTimer > 0) {
            digiCloseTimer -= tpf;
            if (digiCloseTimer <= 0) {
                hud.fermerDigicode();
                codeEntree = "";
                digiCloseTimer = -1f;
                // Réinitialiser les touches de mouvement bloquées pendant la saisie
                joueur.setForward(false);
                joueur.setBackward(false);
                joueur.setLeft(false);
                joueur.setRight(false);
                joueur.setSprint(false);
                batterie.setSprint(false);
            }
        }

        // ── Enigme 3 — salle sombre ───────────────────────────────────────────
        boolean inSalleSombre = !enigme3Resolue && pos.z > ROOM3_Z_MIN && pos.z < ROOM3_Z_MAX;
        if (inSalleSombre) {
            if (visionNocturne) {
                hud.setCharade(CHARADE);
            } else {
                hud.setCharade("");
                // Presque noir sans vision nocturne
                ambiant.setColor(new ColorRGBA(0.03f, 0.03f, 0.03f, 1f));
                // (hint "[N]" géré en fin de section hints ci-dessous)
            }
        } else if (!inSalleSombre && !enigme3Resolue && pos.z <= ROOM3_Z_MIN) {
            hud.setCharade("");
            if (!visionNocturne) ambiant.setColor(ColorRGBA.White.mult(AMBIENT_NORMAL));
        } else if (enigme3Resolue) {
            hud.setCharade("");
        }

        // ── Hints interaction (porte / NPC / objet) ───────────────────────────
        // Ne pas écraser le message si dialogue ou digicode est ouvert
        if (!hud.isDialogueOuvert() && !hud.isDigicodeOuvert()) {
            boolean porteProche = doorManager.update(pos);
            if (porteProche) {
                // Porte proche — verrouillée ou non ?
                if (doorManager.isPorteProcheVerrouillee()) {
                    String np = doorManager.getNumeroPorteProche();
                    if ("002".equals(np)) {
                        hud.setMessageInteraction(enigme2CleRamassee
                            ? "[E] Reparer la porte avec la cle a molette"
                            : "[!] La porte est cassee - trouvez un outil");
                    } else {
                        hud.setMessageInteraction("[E] Entrer le code Digicode");
                    }
                } else {
                    hud.setMessageInteraction("[E] Ouvrir la porte");
                }
            } else if (spatialDaniel != null
                       && pos.distanceSquared(POS_DANIEL) < 3.5f * 3.5f) {
                // Proximite Daniel
                hud.setMessageInteraction("[E] Parler a Daniel");
            } else if (!enigme2CleRamassee && spatialCle != null
                       && pos.distanceSquared(POS_CLE) < 2.5f * 2.5f) {
                // Proximite cle a molette — accroupi requis (sol sous le rack)
                if (joueur.isCrouch()) {
                    hud.setMessageInteraction("[E] Ramasser la cle a molette");
                } else {
                    hud.setMessageInteraction("[C] S'accroupir pour voir sous le rack...");
                }
            } else {
                // Raycasting passif (digicode salle 1 + objets ramassables)
                CollisionResults r = new CollisionResults();
                app.getRootNode().collideWith(
                    new Ray(app.getCamera().getLocation(), app.getCamera().getDirection()), r);
                if (r.size() > 0 && r.getClosestCollision().getDistance() < 3f) {
                    Geometry cible = r.getClosestCollision().getGeometry();
                    if ("Digicode_Interactif_Salle1".equals(cible.getName())) {
                        hud.setMessageInteraction("[E] Utiliser le Digicode");
                    } else if (Boolean.TRUE.equals(cible.getUserData("Ramassable"))) {
                        hud.setMessageInteraction("[E] Ramasser : " + cible.getName());
                    } else {
                        // Rien en vue — proposer vision nocturne si salle sombre
                        boolean inSombre = !enigme3Resolue && pos.z > ROOM3_Z_MIN && pos.z < ROOM3_Z_MAX;
                        hud.setMessageInteraction((inSombre && !visionNocturne) ? "[N] Activer la vision nocturne" : "");
                    }
                } else {
                    boolean inSombre = !enigme3Resolue && pos.z > ROOM3_Z_MIN && pos.z < ROOM3_Z_MAX;
                    hud.setMessageInteraction((inSombre && !visionNocturne) ? "[N] Activer la vision nocturne" : "");
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void basculerVisionNuit() {
        visionNocturne = !visionNocturne;
        if (visionNocturne) {
            // Effet neon vert intense — ambiance "lunettes vision nocturne"
            ambiant.setColor(new ColorRGBA(0f, 3.5f, 0.3f, 1f)); // vert vif
            soleil.setColor(new ColorRGBA(0f, 1.2f, 0.1f, 1f));  // ombre verte douce
            soleil.setDirection(new com.jme3.math.Vector3f(-0.3f, -0.8f, -0.2f).normalizeLocal());
            app.getViewPort().setBackgroundColor(new ColorRGBA(0f, 0.04f, 0.01f, 1f));
            hud.setVisionNuit(true);
        } else {
            ambiant.setColor(ColorRGBA.White.mult(AMBIENT_NORMAL));
            soleil.setColor(ColorRGBA.White.mult(2.0f));
            soleil.setDirection(new com.jme3.math.Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
            app.getViewPort().setBackgroundColor(ColorRGBA.Black);
            hud.setVisionNuit(false);
        }
    }

    @Override
    protected void cleanup(Application application) {
        app.getStateManager().detach(bullet);
        app.getRootNode().detachAllChildren();
        app.getRootNode().removeLight(ambiant);
        app.getRootNode().removeLight(soleil);
        for (PointLight pl : pointLights) app.getRootNode().removeLight(pl);
        pointLights.clear();
        app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(true);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{
            "Avancer","Reculer","Gauche","Droite",
            "Sprint","Accroupir","VisionNuit","Interagir","Sauter","VoirInventaire",
            "Digi0","Digi1","Digi2","Digi3","Digi4",
            "Digi5","Digi6","Digi7","Digi8","Digi9",
            "DigiEffacer","DigiValider","DigiFermer"
        }) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
        if (hud != null) hud.detacher();
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
