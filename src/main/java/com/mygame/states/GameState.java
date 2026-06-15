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
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.ColorOverlayFilter;
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

    private static final float AMBIENT_NORMAL = 0.15f; 
    private final List<PointLight> pointLights = new ArrayList<>();
    private FilterPostProcessor fpp; 
    private ColorOverlayFilter overlay; 
    private Node handsRoot; 
    private com.jme3.renderer.ViewPort handsView;

    // ── Piles ramassables (auto-contact) ─────────────────────────────────────
    private final Node pilesNode = new Node("Piles");

    // ── Objets ramassables (raycasting E key — système Noah) ─────────────────
    private final Node objetsNode = new Node("Objets");

    // ── Enigmes — Digicodes ───────────────────────────────────────────────────
    private static final String CODE_E1       = "1945"; 
    private static final String CODE_E3       = "2703"; 
    private String porteCibleDigicode         = "";     
    // Modifié : Le noir total arrive dès le pas de la porte (Z=40)
    private static final float  ROOM3_Z_MIN   = 40f;  
    private static final float  ROOM3_Z_MAX   = 88f;  
    private boolean enigme3Resolue    = false;
    private String  codeEntree        = "";
    private float   digiCloseTimer    = -1f; 

    // ── Enigme 2 — Robot Daniel + Clé à molette (salle 2) ───────────────────
    private static final Vector3f POS_DANIEL = new Vector3f( 4.0f, 1.0f, 25.0f); 
    private static final Vector3f POS_CLE    = new Vector3f(8.6f, 0.2f, 36.0f); 
    private static final Vector3f POS_CROWBAR = new Vector3f(-6f, 0.4f, 10f); 
    // Modifié : La pile est maintenant cachée dans le nouveau labyrinthe de la Salle 3
    private static final Vector3f POS_PILE    = new Vector3f(7.0f, 0.3f, 49.0f); 
    
    private static final String[] DIALOGUES_DANIEL = {
        "Hey ! Enfin quelqu'un ! Je suis bloque dans ce labo depuis des heures !",
        "La porte suivante est cassee. Il me faut une cle a molette pour la reparer.",
        "J'en ai vu une au fond a droite. Tu dois t'accroupir pour l'attraper !"
    };
    private Spatial spatialDaniel      = null;
    private Spatial spatialCle         = null;
    private boolean enigme2CleRamassee = false;
    private boolean enigme2Resolue     = false;
    private int     enigme2DiagIdx     = 0;
    private int     etatDaniel         = 0;     
    private String[] dialoguesCourants = DIALOGUES_DANIEL;

    // ── Enigme 4 — Blackout salle 4, generateur, alarme, QTE ─────────────────
    private static final float ROOM4_Z_MIN  = 65f;  
    private boolean enigme4Active     = false; 
    private boolean generateurVu      = false; 
    private boolean generateurRepare  = false;
    private boolean generateurDetruit = false;
    private boolean choixReparer      = false;
    private int     pilesEnigme4      = 0;     
    private boolean alarmeActive      = false;
    private float   tempsAlarme       = 0f;
    private boolean qteActif          = false;
    private final String[] sequenceQTE = {"QTE_Haut","QTE_Bas","QTE_Bas","QTE_Gauche"};
    private final String[] nomsQTE     = {"HAUT","BAS","BAS","GAUCHE"};
    private int     qteIndex          = 0;
    private boolean victoireEnCours   = false;
    private float   finTimer          = -1f;
    private boolean enigme1Resolue    = false;
    private boolean fuiteSalle5       = false; 
    private float   fuiteTimer        = 0f;
    private boolean porte004Ouverte   = false;
    private Spatial  robotFin    = null;   // robot lourd qui surgit a l'echec de la fuite
    private boolean  finRepare   = false;  // epilogue : true=preuves sauvees, false=labo detruit
    private boolean  archivePrete = false; // (repair) code fleche fait → le gardien attend
    private static final Vector3f POS_ARCHIVE = new Vector3f(0f, 0f, 131f); // gardien AU FOND de la salle 5
    private boolean  mortEnCours = false;
    private float    mortTimer   = 0f;
    private com.jme3.audio.AudioNode sirene = null;
    private com.jme3.font.BitmapFont fontJeu; // pour les messages ecrits sur les murs
    private Spatial spatialCrowbar    = null;
    private Spatial spatialPile       = null;
    private Geometry marqueurPile     = null;
    private Geometry marqueurCrowbar  = null;

    // ── PNJ Dr. W (survivant, salle 3) — narration + vision nocturne ─────────
    // Apres le premier virage du labyrinthe (passage gauche du mur 1, Z=46)
    private static final Vector3f POS_DOCTEUR = new Vector3f(-6.5f, 1.0f, 49.5f);
    private Spatial  spatialDocteur  = null;
    private Geometry marqueurDocteur = null;
    private int      etatDocteur     = 0; // 0 inconnu · 1 histoire+vision · 2 briefing enigme 4
    private String   speakerCourant  = "Daniel";

    private static final String CHARADE =                 
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

        ambiant = new AmbientLight();
        ambiant.setColor(ColorRGBA.White.mult(AMBIENT_NORMAL));
        app.getRootNode().addLight(ambiant);

        soleil = new DirectionalLight();
        soleil.setDirection(new Vector3f(0f, -1f, 0.2f).normalizeLocal());
        soleil.setColor(ColorRGBA.White.mult(0.35f));
        app.getRootNode().addLight(soleil);

        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f,  4.0f), new ColorRGBA(0.40f, 0.72f, 1.0f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 11.0f), new ColorRGBA(0.30f, 0.65f, 1.0f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 24.0f), new ColorRGBA(0.28f, 1.0f,  0.35f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 36.0f), new ColorRGBA(0.20f, 0.90f, 0.30f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 52.0f), new ColorRGBA(0.15f, 0.10f, 0.08f, 1f), 0.8f, 24f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 70.0f), new ColorRGBA(0.05f, 0.80f, 1.0f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 82.0f), new ColorRGBA(0.10f, 0.70f, 0.90f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f,  98.0f), new ColorRGBA(0.60f, 0.0f,  1.0f, 1f), 1.5f, 26f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 116.0f), new ColorRGBA(1.0f,  0.10f, 0.45f, 1f), 1.2f, 24f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 130.0f), new ColorRGBA(0.60f, 0.0f,  1.0f, 1f), 1.5f, 22f);

        fpp = new FilterPostProcessor(app.getAssetManager());
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(2.2f);    
        bloom.setBlurScale(1.5f);         
        fpp.addFilter(bloom);

        overlay = new ColorOverlayFilter(new ColorRGBA(1f, 1f, 1f, 1f));
        fpp.addFilter(overlay);

        app.getViewPort().addProcessor(fpp);

        try {
            sirene = new com.jme3.audio.AudioNode(app.getAssetManager(), "Sounds/alarm.ogg",
                    com.jme3.audio.AudioData.DataType.Buffer);
            sirene.setLooping(true);
            sirene.setPositional(false);
            sirene.setVolume(2.5f);
            app.getRootNode().attachChild(sirene);
        } catch (Exception e) {
            sirene = null;
        }

        doorManager = new Laboratory().construire(app.getAssetManager(), app.getRootNode(), bullet);

        Spatial lit = app.getAssetManager().loadModel("Models/props/old_bed.glb");
        lit.setName("Lit_Salle1");
        lit.setLocalScale(1.75f);
        lit.setLocalTranslation(0.91f, 0f, -7.45f);
        app.getRootNode().attachChild(lit);

        Spatial generator = app.getAssetManager().loadModel("Models/props/basic_generator.glb");
        generator.setName("Generateur_Interactif"); 
        generator.setLocalScale(1.5f);
        
        // Modifié : Le générateur est caché au fond à droite
        generator.setLocalTranslation(8.0f, 0.813f, 82.0f); 
        app.getRootNode().attachChild(generator);
        app.getRootNode().updateGeometricState();
        com.jme3.bullet.collision.shapes.CollisionShape formeGen =
                com.jme3.bullet.util.CollisionShapeFactory.createBoxShape(generator);
        com.jme3.bullet.control.RigidBodyControl corpsGen =
                new com.jme3.bullet.control.RigidBodyControl(formeGen, 0f);
        generator.addControl(corpsGen);
        bullet.getPhysicsSpace().add(corpsGen);

        handsRoot = new Node("HandsRoot");
        Node nodeCamera = new Node("nodeCamera");
        handsRoot.attachChild(nodeCamera);
        Spatial hands = app.getAssetManager().loadModel("Models/player/arms_throwing.glb");
        hands.rotate(0.2f, 0, 0);
        hands.setLocalScale(0.04f);
        nodeCamera.attachChild(hands);
        
        AmbientLight lumMains = new AmbientLight();
        lumMains.setColor(ColorRGBA.White.mult(1.0f));
        handsRoot.addLight(lumMains);
        DirectionalLight dirMains = new DirectionalLight();
        dirMains.setDirection(new Vector3f(-0.3f, -1f, 0.3f).normalizeLocal());
        dirMains.setColor(ColorRGBA.White.mult(0.8f));
        handsRoot.addLight(dirMains);
        handsView = app.getRenderManager().createMainView("MainsView", app.getCamera());
        handsView.setClearFlags(false, true, false); 
        handsView.attachScene(handsRoot);
        handsRoot.updateGeometricState();

        joueur   = new PlayerControl(bullet, app.getCamera(), nodeCamera, hands);
        batterie = new BatterieManager(600f);
        inventaire = new Inventory();

        app.getRootNode().attachChild(pilesNode);
        placerPile(new Vector3f( 2f, 1f,  8.0f)); // salle 1
        placerPile(new Vector3f(-3f, 1f, 52.0f)); // salle 3
        placerPile(new Vector3f( 3f, 1f, 78.0f)); // salle 4

        app.getRootNode().attachChild(objetsNode);

        hud = new HudManager(
            app.getAssetManager(),
            app.getGuiNode(),
            app.getCamera().getWidth(),
            app.getCamera().getHeight()
        );

        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setMoveSpeed(0f);
        app.getFlyByCamera().setRotationSpeed(2f);
        app.getInputManager().setCursorVisible(false);

        app.getCamera().lookAtDirection(new Vector3f(0f, 0f, 1f), Vector3f.UNIT_Y);

        doorManager.verrouiller("001"); 
        doorManager.verrouiller("002"); 
        doorManager.verrouiller("003"); 
        doorManager.verrouiller("004"); 

        spatialDaniel = app.getAssetManager().loadModel("Models/props/robot_daniel.glb");
        spatialDaniel.setName("Daniel_Robot");
        spatialDaniel.setLocalScale(2.4f);
        spatialDaniel.rotate(0f, -FastMath.HALF_PI, 0f);
        spatialDaniel.setLocalTranslation(POS_DANIEL.x, 0f, POS_DANIEL.z);
        app.getRootNode().attachChild(spatialDaniel);
        
        Node hitboxDaniel = new Node("Daniel_Hitbox");
        hitboxDaniel.setLocalTranslation(POS_DANIEL.x, 1.5f, POS_DANIEL.z);
        app.getRootNode().attachChild(hitboxDaniel);
        com.jme3.bullet.control.RigidBodyControl rbcDaniel =
                new com.jme3.bullet.control.RigidBodyControl(
                        new com.jme3.bullet.collision.shapes.BoxCollisionShape(
                                // hitbox elargie : on s'arrete avant que son modele
                                // ne soit coupe par la camera (near-plane)
                                new Vector3f(1.3f, 1.5f, 1.3f)), 0f);
        hitboxDaniel.addControl(rbcDaniel);
        bullet.getPhysicsSpace().add(rbcDaniel);

        spatialCle = app.getAssetManager().loadModel("Models/props/cle_molette.glb");
        spatialCle.setName("Cle_Molette");
        spatialCle.setLocalScale(0.02f);
        spatialCle.setLocalTranslation(POS_CLE.x, POS_CLE.y, POS_CLE.z);
        app.getRootNode().attachChild(spatialCle);

        // ── PNJ Dr. W : survivant terre dans le noir de la salle 3 ───────────
        spatialDocteur = app.getAssetManager().loadModel("Models/props/doctor_who.glb");
        spatialDocteur.setName("Docteur_W");
        spatialDocteur.rotate(0f, FastMath.HALF_PI, 0f); // 90° vers sa droite → face au joueur
        spatialDocteur.setLocalTranslation(POS_DOCTEUR.x, 0f, POS_DOCTEUR.z);
        app.getRootNode().attachChild(spatialDocteur);
        // Auto-calibrage : hauteur cible 2.9m (imposant), quelle que soit
        // l'echelle interne du modele.
        app.getRootNode().updateGeometricState();
        if (spatialDocteur.getWorldBound() instanceof com.jme3.bounding.BoundingBox) {
            float h = 2f * ((com.jme3.bounding.BoundingBox) spatialDocteur.getWorldBound()).getYExtent();
            if (h > 0.01f) spatialDocteur.setLocalScale(2.9f / h);
        }
        Node hitboxDocteur = new Node("Docteur_Hitbox");
        hitboxDocteur.setLocalTranslation(POS_DOCTEUR.x, 1.5f, POS_DOCTEUR.z);
        app.getRootNode().attachChild(hitboxDocteur);
        com.jme3.bullet.control.RigidBodyControl rbcDocteur =
                new com.jme3.bullet.control.RigidBodyControl(
                        new com.jme3.bullet.collision.shapes.BoxCollisionShape(
                                new Vector3f(1.1f, 1.5f, 1.1f)), 0f);
        hitboxDocteur.addControl(rbcDocteur);
        bullet.getPhysicsSpace().add(rbcDocteur);
        // Lueur blafarde au-dessus de lui : seul repere dans le noir total
        marqueurDocteur = anneauLumineux(POS_DOCTEUR, new ColorRGBA(0.75f, 0.95f, 1f, 1f));

        creerMeublesRoom2();

        // Modifié : Création du labyrinthe (Salle 3) et cachette du générateur (Salle 4)
        creerMeublesRoom3();
        creerMeublesRoom4();

        // Robot gardien de l'archive (salle 5) — la ou on arrache les preuves.
        // (charge aussi le modele en cache → pas de freeze quand un robot surgit)
        Spatial robotArchive = chargerProp("Models/props/heavy_robot.glb", "Robot_Archive",
                POS_ARCHIVE, 10.5f); // gardien de l'archive — agrandi *1.5, tres imposant
        robotArchive.rotate(0, FastMath.PI, 0); // face au joueur qui arrive

        // Histoire ECRITE SUR LES MURS (au fur et a mesure) — touche horreur
        creerTextesMuraux();

        enregistrerTouches();
    }

    private void placerObjet(String nom, Vector3f pos, ColorRGBA couleur) {
        Geometry g = new Geometry(nom, new Box(0.1f, 0.06f, 0.15f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        g.setMaterial(mat);
        g.setLocalTranslation(pos);
        g.setUserData("Ramassable", true);   
        objetsNode.attachChild(g);
    }

    private void placerPile(Vector3f pos) {
        // Vrai modele de batterie (au lieu du cube jaune)
        Spatial pile = app.getAssetManager().loadModel("Models/props/battery_pickup.glb");
        pile.setName("Pile_" + pilesNode.getQuantity());
        pilesNode.attachChild(pile);
        // auto-calibrage a ~0.55m sur le plus grand axe
        app.getRootNode().updateGeometricState();
        float maxDim = 1f;
        if (pile.getWorldBound() instanceof com.jme3.bounding.BoundingBox) {
            com.jme3.bounding.BoundingBox bb = (com.jme3.bounding.BoundingBox) pile.getWorldBound();
            maxDim = 2f * Math.max(bb.getXExtent(), Math.max(bb.getYExtent(), bb.getZExtent()));
        }
        if (maxDim < 1e-4f) maxDim = 1f;
        pile.setLocalScale(0.55f / maxDim);
        pile.setLocalTranslation(pos);
    }

    // ── Histoire ecrite sur les murs (touche horreur, "voix-off" visuelle) ───

    private void creerTextesMuraux() {
        fontJeu = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        // Uniquement salles 1 et 2 (en salle 3 le labyrinthe coupe la vue)
        texteMural("SUJET 27",                         0f, true,  2.4f); // R1 gauche
        texteMural("TU NE DEVAIS PAS\nTE REVEILLER",   8f, false, 2.4f); // R1 droite
        texteMural("TON CORPS DEPEND\nDE TA BATTERIE",23f, false, 2.4f); // R2 droite
        texteMural("LES PILES\nTE GARDENT EN VIE",    30f, true,  2.4f); // R2 gauche
        texteMural("ZERO BATTERIE\n= LA MORT",        37f, false, 2.4f); // R2 droite
    }

    /** Message "grave" sur un mur lateral (rouge sang), oriente vers la salle. */
    private void texteMural(String texte, float z, boolean murGauche, float y) {
        com.jme3.font.BitmapText t = new com.jme3.font.BitmapText(fontJeu);
        t.setSize(0.55f);
        t.setColor(new ColorRGBA(0.85f, 0.06f, 0.06f, 1f)); // rouge sang
        t.setText(texte);
        t.setLocalTranslation(-t.getLineWidth() / 2f, 0.6f, 0f); // centre horizontalement
        t.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        Node n = new Node("TexteMural");
        n.attachChild(t);
        n.setLocalTranslation(murGauche ? -9.3f : 9.3f, y, z);
        n.rotate(0, murGauche ? FastMath.HALF_PI : -FastMath.HALF_PI, 0);
        app.getRootNode().attachChild(n);
    }

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

        int[][] numKeys = {
            {KeyInput.KEY_0, KeyInput.KEY_NUMPAD0}, {KeyInput.KEY_1, KeyInput.KEY_NUMPAD1},
            {KeyInput.KEY_2, KeyInput.KEY_NUMPAD2}, {KeyInput.KEY_3, KeyInput.KEY_NUMPAD3},
            {KeyInput.KEY_4, KeyInput.KEY_NUMPAD4}, {KeyInput.KEY_5, KeyInput.KEY_NUMPAD5},
            {KeyInput.KEY_6, KeyInput.KEY_NUMPAD6}, {KeyInput.KEY_7, KeyInput.KEY_NUMPAD7},
            {KeyInput.KEY_8, KeyInput.KEY_NUMPAD8}, {KeyInput.KEY_9, KeyInput.KEY_NUMPAD9},
        };
        for (int i = 0; i <= 9; i++) {
            app.getInputManager().addMapping("Digi" + i,
                new KeyTrigger(numKeys[i][0]), new KeyTrigger(numKeys[i][1]));
        }
        app.getInputManager().addMapping("DigiEffacer", new KeyTrigger(KeyInput.KEY_BACK));
        app.getInputManager().addMapping("DigiValider",  new KeyTrigger(KeyInput.KEY_RETURN),
                                                         new KeyTrigger(KeyInput.KEY_NUMPADENTER));
        app.getInputManager().addMapping("DigiFermer",   new KeyTrigger(KeyInput.KEY_ESCAPE));

        app.getInputManager().addMapping("QTE_Haut",   new KeyTrigger(KeyInput.KEY_UP));
        app.getInputManager().addMapping("QTE_Bas",    new KeyTrigger(KeyInput.KEY_DOWN));
        app.getInputManager().addMapping("QTE_Gauche", new KeyTrigger(KeyInput.KEY_LEFT));
        app.getInputManager().addMapping("QTE_Droite", new KeyTrigger(KeyInput.KEY_RIGHT));

        app.getInputManager().addListener(actionListener,
                "Avancer","Reculer","Gauche","Droite",
                "Sprint","Accroupir","VisionNuit","Interagir","Sauter","VoirInventaire",
                "Digi0","Digi1","Digi2","Digi3","Digi4",
                "Digi5","Digi6","Digi7","Digi8","Digi9",
                "DigiEffacer","DigiValider","DigiFermer",
                "QTE_Haut","QTE_Bas","QTE_Gauche","QTE_Droite");
    }

    private void onAction(String name, boolean isPressed, float tpf) {
        if (mortEnCours || victoireEnCours) return; // plus d'input apres la fin
        if (hud.isDigicodeOuvert()) {
            boolean mouvement = name.equals("Avancer") || name.equals("Reculer")
                             || name.equals("Gauche")  || name.equals("Droite")
                             || name.equals("Sprint")  || name.equals("Sauter");
            if (mouvement) {
                hud.fermerDigicode();
                codeEntree = "";
            } else {
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
                        String chiffre = name.replace("Digi", "");
                        if (codeEntree.length() < 4) {
                            codeEntree += chiffre;
                            hud.updateDigicode(codeEntree);
                            if (codeEntree.length() == 4) validerCodeDigicode();
                        }
                    }
                }
                return; 
            }
        }

        if (hud.isDialogueOuvert()) {
            if (!isPressed) return;
            if (etatDaniel == 3 && "Daniel".equals(speakerCourant)) {
                if (name.equals("Digi1")) {
                    choixReparer = true; etatDaniel = 4; pilesEnigme4 = 1; 
                    apparaitrePile(); 
                    dialoguesCourants = new String[]{
                        "Tiens, ma derniere pile 9V. Il en faut DEUX pour le generateur.",
                        "La 2e est dans le labyrinthe (salle 3)... la ou ILS rodent.",
                        "Remets le courant. Et prie pour que la lumiere LES retienne."
                    };
                    enigme2DiagIdx = 0;
                    hud.setLigneDialogue("Daniel", dialoguesCourants[0]);
                    return;
                } else if (name.equals("Digi2")) {
                    choixReparer = false; etatDaniel = 4;
                    apparaitreCrowbar(); 
                    dialoguesCourants = new String[]{
                        "La methode forte... le Dr. W avait raison sur toi.",
                        "Le PIED DE BICHE est en salle 1. Sabote le generateur.",
                        "Le noir LES reveille, Sujet 27. Cours plus vite qu'eux."
                    };
                    enigme2DiagIdx = 0;
                    hud.setLigneDialogue("Daniel", dialoguesCourants[0]);
                    return;
                }
            }
            if (name.equals("Interagir")) avancerDialogueEnigme2();
            return;
        }

        if (qteActif) { // combinaison de fleches entree SUR LE ROBOT (REPARATION)
            if (!isPressed) return;
            joueur.setForward(false); joueur.setBackward(false);
            joueur.setLeft(false);    joueur.setRight(false);
            if (name.equals(sequenceQTE[qteIndex])) {
                qteIndex++;
                if (qteIndex >= sequenceQTE.length) {
                    qteActif = false;
                    archivePrete = true; // combinaison OK : le robot va te liberer (E)
                    hud.setMessageInteraction("");
                }
            } else if (name.startsWith("QTE_")) {
                qteIndex = 0; // mauvaise fleche → on recommence la sequence
            }
            return;
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
                    // Robot gardien (fin REPARATION) : entrer la combinaison SUR le
                    // robot, puis il te libere et te remet les preuves.
                    if (choixReparer && fuiteSalle5 && proche(POS_ARCHIVE, 6f)) {
                        if (archivePrete) {
                            inventaire.ajouter("Dossiers classifies (preuves)");
                            finRepare = true;
                            declencherVictoire("\"Combinaison acceptee. Tu es libre, Sujet 27.\n"
                                    + "Tu es humain... enfin. Prends les preuves et COURS.\"");
                        } else if (!qteActif) {
                            // ouvrir la saisie de la combinaison directement sur le robot
                            qteActif = true; qteIndex = 0;
                            joueur.setForward(false); joueur.setBackward(false);
                            joueur.setLeft(false);    joueur.setRight(false);
                            hud.setMessageInteraction("");
                        }
                        return;
                    }

                    CollisionResults resultats = new CollisionResults();
                    Ray rayon = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
                    app.getRootNode().collideWith(rayon, resultats);
                    if (resultats.size() > 0 && resultats.getClosestCollision().getDistance() < 3.0f) {
                        Geometry cible = resultats.getClosestCollision().getGeometry();
                        if (estGenerateur(cible)) { interagirGenerateur(); return; }
                    }

                    if (!enigme2CleRamassee && spatialCle != null && proche(POS_CLE, 2.5f)) {
                        if (joueur.isCrouch()) {
                            enigme2CleRamassee = true;
                            spatialCle.removeFromParent();
                            spatialCle = null;
                            inventaire.ajouter("Cle a molette");
                            hud.setMessageInteraction("Cle a molette trouvee ! Rapporte-la a Daniel.");
                        } else {
                            hud.setMessageInteraction("[C] S'accroupir pour attraper la cle !");
                        }
                        return;
                    }

                    if (spatialCrowbar != null && proche(POS_CROWBAR, 2.5f)) {
                        spatialCrowbar.removeFromParent();
                        spatialCrowbar = null;
                        if (marqueurCrowbar != null) { marqueurCrowbar.removeFromParent(); marqueurCrowbar = null; }
                        inventaire.ajouter("Pied de biche");
                        hud.setMessageInteraction("Pied de biche recupere ! Sabote le generateur.");
                        return;
                    }

                    if (spatialPile != null && proche(POS_PILE, 2.5f)) {
                        spatialPile.removeFromParent();
                        spatialPile = null;
                        if (marqueurPile != null) { marqueurPile.removeFromParent(); marqueurPile = null; }
                        pilesEnigme4++;
                        inventaire.ajouter("Pile 9V");
                        hud.setMessageInteraction("Pile 9V ajoutee a l'inventaire (" + pilesEnigme4 + "/2) !");
                        return;
                    }

                    if (spatialDocteur != null && proche(POS_DOCTEUR, 3.5f)) {
                        ouvrirDialogueDocteur();
                        return;
                    }

                    if (spatialDaniel != null && proche(POS_DANIEL, 3.5f)) {
                        ouvrirDialogueDaniel();
                        return;
                    }

                    if (doorManager.isPorteProcheVerrouillee()) {
                        String numPorte = doorManager.getNumeroPorteProche();
                        if ("002".equals(numPorte)) {
                            hud.setMessageInteraction(enigme2CleRamassee
                                ? "[!] Rapporte la cle a Daniel pour qu'il ouvre !"
                                : "[!] Porte cassee - trouve un outil pour Daniel");
                        } else if ("004".equals(numPorte)) {
                            if (generateurRepare || generateurDetruit) {
                                doorManager.deverrouiller("004");
                                doorManager.interagir();
                                porte004Ouverte = true;
                                demarrerFuite(); // la course de 10s commence !
                            } else {
                                hud.setMessageInteraction("[!] Verrouillee : repare ou sabote le generateur");
                            }
                        } else {
                            ouvrirDigicodePorte(numPorte);
                        }
                    } else {
                        if (!doorManager.interagir()) tenterDeRamasser();
                    }
                }
            }
        }
    }

    private void ouvrirDigicodePorte(String porte) {
        porteCibleDigicode = porte;
        codeEntree = "";
        hud.ouvrirDigicode();
        hud.updateDigicode("");
        hud.setIndiceDigicode("001".equals(porte)
            ? "Mot a decoder (tableau) :  A I D E"
            : "Indice : la charade (salle sombre)");
        joueur.setForward(false); joueur.setBackward(false);
        joueur.setLeft(false);    joueur.setRight(false);
        app.getInputManager().setCursorVisible(false);
    }

    private void validerCodeDigicode() {
        String bonCode = "001".equals(porteCibleDigicode) ? CODE_E1 : CODE_E3;

        if (codeEntree.equals(bonCode)) {
            hud.setFeedbackDigicode("CODE ACCEPTE !", ColorRGBA.Green);
            doorManager.deverrouiller(porteCibleDigicode);
            if ("001".equals(porteCibleDigicode)) enigme1Resolue = true;

            if ("003".equals(porteCibleDigicode)) {
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

    private void ouvrirDialogueDaniel() {
        if (etatDaniel == 0) {
            if (enigme2CleRamassee) {
                etatDaniel = 1; 
                dialoguesCourants = new String[]{
                    "Ah, la cle a molette ! Parfait, merci !",
                    "Je te deverrouille la porte. Vas-y, file !"
                };
            } else {
                dialoguesCourants = DIALOGUES_DANIEL;
            }
        } else if (etatDaniel == 2) {
            // Blackout en cours mais le choix n'est pas debloque : Daniel renvoie
            // vers le Dr. W (la narration passe par lui).
            dialoguesCourants = generateurVu
                ? new String[]{
                    "Le generateur ?! N'y touche pas sans savoir, Sujet 27...",
                    "Le Dr. W se terre en salle 3. Lui seul connait cette machine."}
                : new String[]{"La porte est ouverte, vas-y ! Fais attention a toi."};
        } else if (etatDaniel == 3) {
            dialoguesCourants = new String[]{
                "Le courant a saute ! Choisis :\n"
              + "  1 - REPARER le generateur (2 piles 9V)\n"
              + "  2 - SABOTER le labo (pied de biche)\n"
              + "  (Appuie sur 1 ou 2)"
            };
        } else { 
            dialoguesCourants = choixReparer
                ? new String[]{"Repare le generateur : il faut 2 piles 9V."}
                : new String[]{"Recupere le pied de biche (salle 1) et sabote le generateur."};
        }
        enigme2DiagIdx = 0;
        speakerCourant = "Daniel";
        hud.ouvrirDialogue("Daniel", dialoguesCourants[0]);
    }

    /**
     * PNJ Dr. W (salle 3) — la voix de l'histoire :
     *  1er contact  → lore horreur du labo + deblocage de la VISION NOCTURNE [N]
     *  apres le generateur → briefing des DEUX issues (reparer / saboter),
     *  puis renvoie vers Daniel pour le choix.
     */
    private void ouvrirDialogueDocteur() {
        if (etatDocteur == 0) {
            etatDocteur = 1; // la vision nocturne est desormais utilisable
            dialoguesCourants = new String[]{
                "...Chut. Pas si fort. ILS ecoutent.",
                "Dr. W. Chercheur de ce labo... enfin, ce qu'il en reste.",
                "27 sujets. Tu es le 27e. Aucun des autres n'est ressorti.",
                "Tes lunettes ont un mode VISION NOCTURNE : touche [N].",
                "Le code de la porte est ecrit sur les murs - INVISIBLE a l'oeil nu.",
                "Et quand ILS couperont la lumiere, plus loin... tu seras pret."
            };
        } else if (generateurVu && etatDocteur == 1) {
            etatDocteur = 2;
            etatDaniel  = 3; // Daniel proposera maintenant le choix 1/2
            dialoguesCourants = new String[]{
                "Le generateur est mort ? Alors c'est l'heure du choix, Sujet 27.",
                "REPARER : la lumiere revient, la porte s'ouvre... mais ILS sauront ou tu es.",
                "SABOTER : le labo meurt dans le noir. ILS seront aveugles. Toi aussi.",
                "Daniel garde la derniere pile ET le pied de biche. Va le voir (salle 2).",
                "Quel que soit ton choix... une fois la porte 004 ouverte, COURS."
            };
        } else if (etatDocteur >= 2) {
            dialoguesCourants = new String[]{"Va voir Daniel (salle 2). Et souviens-toi : COURS."};
        } else {
            dialoguesCourants = new String[]{"Trouve le code sur les murs. [N]... et ne reste pas dans le noir."};
        }
        enigme2DiagIdx = 0;
        speakerCourant = "Dr. W";
        hud.ouvrirDialogue("Dr. W", dialoguesCourants[0]);
    }

    private void avancerDialogueEnigme2() {
        enigme2DiagIdx++;
        if (enigme2DiagIdx < dialoguesCourants.length) {
            hud.setLigneDialogue(speakerCourant, dialoguesCourants[enigme2DiagIdx]);
        } else if (etatDaniel == 3 && "Daniel".equals(speakerCourant)) {
            enigme2DiagIdx = dialoguesCourants.length - 1;
        } else {
            enigme2DiagIdx = 0;
            hud.fermerDialogue();
            if (etatDaniel == 1) { 
                doorManager.deverrouiller("002");
                etatDaniel = 2;
                enigme2Resolue = true;
                hud.setMessageInteraction("Daniel a deverrouille la porte 002 !");
            }
        }
    }

    private void interagirGenerateur() {
        if (generateurRepare || generateurDetruit) return;
        
        if (!generateurVu) {
            generateurVu = true; // la suite passe d'abord par le Dr. W (salle 3)
            hud.setMessageInteraction("Le generateur est mort... Le Dr. W (salle 3) saura quoi faire.");
            return;
        }
        
        if (choixReparer) {
            if (pilesEnigme4 >= 2) {
                generateurRepare = true;
                hud.setMessageInteraction("Generateur REPARE ! Va a la porte 004 et PREPARE-TOI a courir.");
            } else {
                hud.setMessageInteraction("Il manque une pile 9V (" + pilesEnigme4 + "/2).");
            }
        } else {
            if (inventaire.contient("Pied de biche")) {
                generateurDetruit = true; 
                alarmeActive = true; 
                if (sirene != null) sirene.play();
                hud.setMessageInteraction("Generateur SABOTE ! Fonce a la porte 004 et FUIS !");
            } else {
                hud.setMessageInteraction("Il te faut le PIED DE BICHE (salle 1).");
            }
        }
    }

    private boolean estGenerateur(Spatial s) {
        while (s != null) {
            if ("Generateur_Interactif".equals(s.getName())) return true;
            s = s.getParent();
        }
        return false;
    }

    private boolean proche(Vector3f cible, float rayon) {
        return joueur.getCharacterControl().getPhysicsLocation().distanceSquared(cible) < rayon * rayon;
    }

    private void declencherVictoire(String message) {
        victoireEnCours = true;
        finTimer = 4f;
        alarmeActive = false;
        fuiteSalle5 = false;
        if (sirene != null) sirene.stop();
        if (overlay != null) overlay.setColor(new ColorRGBA(1f, 1f, 1f, 1f));
        hud.setMessageInteraction("");
        hud.setCharade("");
        hud.setObjectifEncadre(message);
    }

    private void demarrerFuite() {
        fuiteSalle5  = true;
        alarmeActive = true;
        fuiteTimer   = 10f; // 10 secondes pour s'echapper
        if (sirene != null) sirene.play();
        hud.setMessageInteraction("");
    }

    /** Echec de la fuite : un robot lourd surgit, raille le joueur, puis Game Over. */
    private void declencherMort() {
        mortEnCours = true;
        mortTimer   = 4f;
        qteActif    = false;
        fuiteSalle5 = false;
        if (sirene != null) sirene.stop();
        if (overlay != null) overlay.setColor(new ColorRGBA(0.35f, 0f, 0f, 1f)); // rouge sombre
        joueur.setForward(false); joueur.setBackward(false);
        joueur.setLeft(false);    joueur.setRight(false);
        // Le robot surgit DEVANT le joueur et lui fait face
        Vector3f pj  = joueur.getCharacterControl().getPhysicsLocation();
        Vector3f dir = app.getCamera().getDirection().clone(); dir.y = 0; dir.normalizeLocal();
        Vector3f devant = pj.add(dir.mult(3.5f)); devant.y = 0f;
        robotFin = chargerProp("Models/props/heavy_robot.glb", "Robot_Fin", devant, 7f);
        robotFin.lookAt(new Vector3f(pj.x, devant.y, pj.z), Vector3f.UNIT_Y);
        hud.setCharade("");
        hud.setMessageInteraction("");
        hud.setObjectifEncadre("\"Tu veux t'enfuir si vite ?\"");
    }

    private String calculerObjectif() {
        if (archivePrete)  return "Le robot s'incline... [E] pour partir avec les preuves, humain.";
        if (qteActif) {
            StringBuilder sb = new StringBuilder("COMBINAISON DU ROBOT >>  ");
            for (int i = 0; i < sequenceQTE.length; i++)
                sb.append(i < qteIndex ? "[ok] " : nomsQTE[i] + "  ");
            return sb.toString();
        }
        if (fuiteSalle5)   return (choixReparer ? "COURS vers le ROBOT gardien au fond ! " : "FUIS ! Atteins le fond ! ")
                                  + "(" + Math.max(0, (int) Math.ceil(fuiteTimer)) + "s)";
        if (generateurRepare) return "Porte 004 : entre la COMBINAISON, puis vois le gardien au fond.";
        if (generateurDetruit) return "AUTODESTRUCTION ! Fonce a la porte 004 et FUIS !";
        if (enigme4Active) {
            if (!generateurVu)   return "Coupure de courant ! Trouve le generateur (salle 4).";
            if (etatDocteur < 2) return "Le Dr. W (salle 3) sait quoi faire du generateur. Retourne le voir.";
            if (etatDaniel < 4)  return "Daniel (salle 2) a le materiel. Va faire ton CHOIX.";
            if (choixReparer)   return "Trouve 2 piles 9V (" + pilesEnigme4 + "/2) puis repare le generateur.";
            return inventaire.contient("Pied de biche")
                ? "Sabote le generateur (salle 4)."
                : "Recupere le pied de biche (salle 1).";
        }
        if (enigme3Resolue)     return "Avance vers la salle 4.";
        if (enigme2Resolue && etatDocteur == 0)
                                return "Salle 3 : quelqu'un se cache ici... va lui parler.";
        if (enigme2Resolue)     return "Lis le code cache sur les murs : [N] vision nocturne.";
        if (etatDaniel >= 1)    return "Daniel ouvre la porte... avance !";
        if (enigme2CleRamassee) return "Rapporte la cle a molette a Daniel (salle 2).";
        if (enigme1Resolue)     return "Salle 2 : trouve la cle (accroupi) et parle a Daniel.";
        return "Salle 1 : dechiffre le code (AIDE) grace a l'image au mur.";
    }

    private Geometry anneauLumineux(Vector3f pos, ColorRGBA couleur) {
        Geometry g = new Geometry("Anneau",
                new com.jme3.scene.shape.Torus(24, 8, 0.04f, 0.45f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        mat.setColor("GlowColor", couleur); 
        g.setMaterial(mat);
        g.rotate(FastMath.HALF_PI, 0, 0); 
        g.setLocalTranslation(pos.x, pos.y + 1.1f, pos.z);
        app.getRootNode().attachChild(g);
        return g;
    }

    private void apparaitrePile() {
        if (spatialPile != null) return;
        spatialPile  = chargerProp("Models/props/9v_battery.glb", "Pile_9V_2", POS_PILE, 0.4f);
        marqueurPile = anneauLumineux(POS_PILE, new ColorRGBA(0.7f, 1f, 0.1f, 1f)); 
    }

    private void apparaitreCrowbar() {
        if (spatialCrowbar != null) return;
        spatialCrowbar  = chargerProp("Models/props/crowbar.glb", "Pied_De_Biche", POS_CROWBAR, 1.0f);
        marqueurCrowbar = anneauLumineux(POS_CROWBAR, new ColorRGBA(1f, 0.5f, 0f, 1f)); 
    }

    private Spatial chargerProp(String chemin, String nom, Vector3f pos, float tailleCible) {
        Spatial s = app.getAssetManager().loadModel(chemin);
        s.setName(nom);
        app.getRootNode().attachChild(s);
        app.getRootNode().updateGeometricState();
        float maxDim = 1f;
        com.jme3.bounding.BoundingVolume bv = s.getWorldBound();
        if (bv instanceof com.jme3.bounding.BoundingBox) {
            com.jme3.bounding.BoundingBox bb = (com.jme3.bounding.BoundingBox) bv;
            maxDim = 2f * Math.max(bb.getXExtent(), Math.max(bb.getYExtent(), bb.getZExtent()));
        }
        if (maxDim < 1e-4f) maxDim = 1f;
        s.setLocalScale(tailleCible / maxDim);
        s.setLocalTranslation(pos);
        return s;
    }

    private void creerMeublesRoom2() {
        creerMeuble("Table_Labo1_R2",
            new Vector3f(-7.5f, 0.48f, 21.0f), new Vector3f(1.8f, 0.48f, 0.8f),  
            new ColorRGBA(0.22f, 0.25f, 0.28f, 1f));
        creerMeuble("Table_Labo2_R2",
            new Vector3f( 7.5f, 0.48f, 29.0f), new Vector3f(1.8f, 0.48f, 0.8f),  
            new ColorRGBA(0.22f, 0.25f, 0.28f, 1f));

        creerMeuble("Caisse1_R2",
            new Vector3f(-7.5f, 1.34f, 20.5f), new Vector3f(0.34f, 0.34f, 0.34f), 
            new ColorRGBA(0.40f, 0.34f, 0.20f, 1f));
        creerMeuble("Caisse2_R2",
            new Vector3f(-6.8f, 1.34f, 22.0f), new Vector3f(0.28f, 0.28f, 0.28f), 
            new ColorRGBA(0.36f, 0.31f, 0.18f, 1f));

        creerMeuble("Ecran_R2",
            new Vector3f( 7.5f, 1.52f, 28.0f), new Vector3f(0.48f, 0.38f, 0.05f), 
            new ColorRGBA(0.07f, 0.07f, 0.10f, 1f));

        creerRackMural(-9.0f, 37.0f); 

        creerMeuble("Caisse_Sol1_R2",
            new Vector3f(-2.1f, 0.42f, 30.5f), new Vector3f(0.55f, 0.42f, 0.55f), 
            new ColorRGBA(0.40f, 0.34f, 0.20f, 1f));
        creerMeuble("Caisse_Sol2_R2",
            new Vector3f(-2.9f, 0.42f, 30.5f), new Vector3f(0.55f, 0.42f, 0.55f), 
            new ColorRGBA(0.36f, 0.31f, 0.18f, 1f));

        creerMeuble("Etagere_R2",
            new Vector3f( 9.3f, 1.80f, 20.0f), new Vector3f(0.15f, 0.80f, 2.0f),  
            new ColorRGBA(0.20f, 0.22f, 0.26f, 1f));

        creerMeuble("Pilier1_R2",
            new Vector3f( 0.0f, 1.50f, 23.5f), new Vector3f(0.20f, 1.50f, 0.20f), 
            new ColorRGBA(0.30f, 0.32f, 0.36f, 1f));

        creerMeuble("Etabli_R2",
            new Vector3f( 4.0f, 0.48f, 38.5f), new Vector3f(1.5f, 0.48f, 0.6f),   
            new ColorRGBA(0.25f, 0.27f, 0.30f, 1f));
    }

    // Modifié : Le Labyrinthe de la Salle 3
    private void creerMeublesRoom3() {
        creerMeuble("Labyrinthe1_R3", 
            new Vector3f(3.0f, 1.5f, 46.0f), new Vector3f(7.0f, 1.5f, 1.0f), 
            new ColorRGBA(0.15f, 0.15f, 0.15f, 1f));
        creerMeuble("Labyrinthe2_R3", 
            new Vector3f(-4.0f, 1.5f, 53.0f), new Vector3f(6.0f, 1.5f, 1.0f), 
            new ColorRGBA(0.12f, 0.12f, 0.12f, 1f));
        creerMeuble("Labyrinthe3_R3", 
            new Vector3f(4.0f, 1.5f, 59.0f), new Vector3f(6.0f, 1.5f, 1.0f), 
            new ColorRGBA(0.14f, 0.14f, 0.14f, 1f));
    }

    // Modifié : Les caisses qui cachent le générateur en Salle 4
    private void creerMeublesRoom4() {
        creerMeuble("CacheGen1_R4", 
            new Vector3f(4.0f, 2.0f, 80.0f), new Vector3f(0.5f, 2.0f, 3.0f), 
            new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
        creerMeuble("CacheGen2_R4", 
            new Vector3f(8.0f, 2.0f, 77.5f), new Vector3f(4.0f, 2.0f, 0.5f), 
            new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
    }

    private void creerRackMural(float x, float z) {
        creerMeuble("Rack_Corps",
            new Vector3f(x, 2.2f, z),
            new Vector3f(0.60f, 1.25f, 0.18f),
            new ColorRGBA(0.11f, 0.13f, 0.15f, 1f));

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

        float ledX = x + 0.46f;
        float ledZ  = z - 0.21f;
        ColorRGBA[] ledCols = {
            new ColorRGBA(0.0f, 1.0f, 1.0f, 1f),   
            new ColorRGBA(1.0f, 0.1f, 0.1f, 1f),   
            new ColorRGBA(0.1f, 1.0f, 0.1f, 1f),   
            new ColorRGBA(1.0f, 0.5f, 0.0f, 1f)    
        };
        for (int i = 0; i < 4; i++) {
            creerMeuble("Rack_LED" + i,
                new Vector3f(ledX, baysY[i], ledZ),
                new Vector3f(0.022f, 0.022f, 0.01f),
                ledCols[i]);
        }

        creerMeuble("Rack_Label",
            new Vector3f(x, 3.36f, z - 0.19f),
            new Vector3f(0.54f, 0.055f, 0.01f),
            new ColorRGBA(0.52f, 0.57f, 0.62f, 1f));

        creerMeuble("Rack_Ventil",
            new Vector3f(x, 1.08f, z - 0.19f),
            new Vector3f(0.54f, 0.04f, 0.01f),
            new ColorRGBA(0.08f, 0.09f, 0.11f, 1f));

        creerMeuble("Rack_Eq_G",
            new Vector3f(x - 0.52f, 1.05f, z + 0.04f),
            new Vector3f(0.04f, 0.12f, 0.12f),
            new ColorRGBA(0.13f, 0.15f, 0.17f, 1f));
        creerMeuble("Rack_Eq_D",
            new Vector3f(x + 0.52f, 1.05f, z + 0.04f),
            new Vector3f(0.04f, 0.12f, 0.12f),
            new ColorRGBA(0.13f, 0.15f, 0.17f, 1f));
    }

    // Modifié : Ajout d'une hitbox solide sur tous les meubles pour ne pas tricher !
    private void creerMeuble(String nom, Vector3f pos, Vector3f demi, ColorRGBA couleur) {
        Geometry g = new Geometry(nom, new Box(demi.x, demi.y, demi.z));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        g.setMaterial(mat);
        g.setLocalTranslation(pos);
        
        com.jme3.bullet.collision.shapes.BoxCollisionShape shape = new com.jme3.bullet.collision.shapes.BoxCollisionShape(demi);
        com.jme3.bullet.control.RigidBodyControl rbc = new com.jme3.bullet.control.RigidBodyControl(shape, 0f);
        g.addControl(rbc);
        bullet.getPhysicsSpace().add(rbc);
        
        app.getRootNode().attachChild(g);
    }

    private void ajouterLumiereAmbiante(Vector3f pos, ColorRGBA couleur, float intensite, float rayon) {
        PointLight pl = new PointLight();
        pl.setPosition(pos);
        pl.setColor(couleur.mult(intensite));
        pl.setRadius(rayon);
        app.getRootNode().addLight(pl);
        pointLights.add(pl);
    }

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
                }
            }
        }
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) return;

        if (victoireEnCours) {
            finTimer -= tpf;
            if (finTimer <= 0) {
                app.getStateManager().detach(this);
                app.getRootNode().detachAllChildren();
                // Video de fin, puis epilogue "A SUIVRE" sur l'ile
                // (texte different selon le choix repare/sabote)
                app.getStateManager().attach(new FinVideoState(finRepare));
            }
            return;
        }

        if (mortEnCours) { // le robot raille le joueur, puis Game Over
            mortTimer -= tpf;
            joueur.setForward(false); joueur.setBackward(false);
            joueur.setLeft(false);    joueur.setRight(false);
            if (mortTimer <= 0) {
                app.getStateManager().detach(this);
                app.getRootNode().detachAllChildren();
                app.getStateManager().attach(new GameOverState());
            }
            return;
        }

        joueur.update(tpf);

        if (handsRoot != null) {
            handsRoot.updateLogicalState(tpf);
            handsRoot.updateGeometricState();
        }

        Vector3f pos = joueur.getCharacterControl().getPhysicsLocation();

        batterie.update(tpf);
        hud.updateBatterie(batterie.getPourcentage());

        if (batterie.isGameOver()) {
            app.getStateManager().detach(this);
            app.getRootNode().detachAllChildren();
            app.getStateManager().attach(new GameOverState());
        }

        for (int i = pilesNode.getQuantity() - 1; i >= 0; i--) {
            Spatial pile = pilesNode.getChild(i);
            pile.rotate(0, tpf * 1.5f, 0); // tourne lentement pour attirer l'oeil
            if (pos.distanceSquared(pile.getWorldTranslation()) < 2f * 2f) {
                batterie.rechargerAFond();
                pile.removeFromParent();
                hud.setMessageInteraction(">>> BATTERIE RECHARGEE A FOND <<<");
            }
        }

        if (digiCloseTimer > 0) {
            digiCloseTimer -= tpf;
            if (digiCloseTimer <= 0) {
                hud.fermerDigicode();
                codeEntree = "";
                digiCloseTimer = -1f;
                joueur.setForward(false);
                joueur.setBackward(false);
                joueur.setLeft(false);
                joueur.setRight(false);
                joueur.setSprint(false);
                batterie.setSprint(false);
            }
        }

        if (enigme3Resolue && !enigme4Active && pos.z > ROOM4_Z_MIN) {
            enigme4Active = true; 
        }
        // La salle 3 reste ECLAIREE : la vision nocturne sert uniquement a
        // reveler le code cache sur les murs. Le SEUL noir total est le
        // blackout global de l'enigme 4 (generateur, salle 4).
        boolean dansSalle3     = !enigme3Resolue && pos.z > ROOM3_Z_MIN && pos.z < 64f;
        boolean blackoutGlobal = enigme4Active && !generateurRepare;
        boolean dansLeNoir     = blackoutGlobal;

        if (alarmeActive) {
            tempsAlarme += tpf;
            // Alarme moins oppressante : l'ecran CLIGNOTE entre rouge et la
            // couleur normale de la salle (au lieu d'un rouge constant).
            float blink = 0.5f + 0.5f * FastMath.sin(tempsAlarme * 6f); // 0..1
            float vb = 1f - blink * 0.92f; // blink=1 → rouge (vb~0.08) · blink=0 → normal (vb=1)
            overlay.setColor(new ColorRGBA(1f, vb, vb, 1f));
        } else {
            majTeinteEcran(dansLeNoir);
        }

        if (fuiteSalle5 && !victoireEnCours && !mortEnCours) {
            // Le chrono se FIGE pendant le code fleche ET le dialogue du gardien
            boolean fige = qteActif || archivePrete;
            if (!fige) {
                fuiteTimer -= tpf;
                batterie.drainer(tpf * 12f); // la course pompe la batterie
            }
            if (choixReparer) {
                // REPARATION : la combinaison se fait directement SUR le robot
                // gardien (interaction E), qui te libere → victoire.
            } else {
                // SABOTAGE : juste courir au fond
                if (pos.z > 130f) {
                    finRepare = false;
                    declencherVictoire("Le labo s'autodetruit derriere toi...\nTu es libre. FUIS !");
                }
            }
            if (!fige && (fuiteTimer <= 0f || batterie.isGameOver())) declencherMort();
        }

        hud.setCharade(dansSalle3 && visionNocturne ? CHARADE : "");
        if (!victoireEnCours) hud.setObjectifEncadre(calculerObjectif());

        if (hud.isDigicodeOuvert() || hud.isDialogueOuvert()) hud.setMessageInteraction("");

        if (marqueurPile != null)    marqueurPile.rotate(0, 0, tpf * 2f);
        if (marqueurCrowbar != null) marqueurCrowbar.rotate(0, 0, tpf * 2f);
        if (marqueurDocteur != null) marqueurDocteur.rotate(0, 0, tpf * 1.2f);

        if (!hud.isDialogueOuvert() && !hud.isDigicodeOuvert() && !qteActif) {
            boolean porteProche = doorManager.update(pos);
            if (porteProche && doorManager.isPorteProcheVerrouillee()) {
                String np = doorManager.getNumeroPorteProche();
                if ("002".equals(np)) {
                    hud.setMessageInteraction(enigme2CleRamassee
                        ? "[!] Rapporte la cle a Daniel pour qu'il ouvre"
                        : "[!] Porte cassee - trouve un outil pour Daniel");
                } else if ("004".equals(np)) {
                    hud.setMessageInteraction((generateurRepare || generateurDetruit)
                        ? "[E] OUVRIR LA PORTE - prepare-toi a courir !"
                        : "[!] Verrouillee : generateur hors service");
                } else {
                    hud.setMessageInteraction("[E] Entrer le code Digicode");
                }
            } else if (porteProche) {
                hud.setMessageInteraction("[E] Ouvrir la porte");
            } else if (choixReparer && fuiteSalle5 && proche(POS_ARCHIVE, 6f)) {
                hud.setMessageInteraction(archivePrete
                    ? "[E] Le robot te libere..."
                    : "[E] Entrer la combinaison sur le robot");
            } else if (spatialDocteur != null && proche(POS_DOCTEUR, 3.5f)) {
                hud.setMessageInteraction("[E] Parler au Dr. W");
            } else if (spatialDaniel != null && proche(POS_DANIEL, 3.5f)) {
                hud.setMessageInteraction("[E] Parler a Daniel");
            } else if (!enigme2CleRamassee && spatialCle != null && proche(POS_CLE, 2.5f)) {
                hud.setMessageInteraction(joueur.isCrouch()
                    ? "[E] Ramasser la cle a molette"
                    : "[C] S'accroupir pour voir sous le rack...");
            } else if (spatialCrowbar != null && proche(POS_CROWBAR, 2.5f)) {
                hud.setMessageInteraction("[E] Ramasser le pied de biche");
            } else if (spatialPile != null && proche(POS_PILE, 2.5f)) {
                hud.setMessageInteraction("[E] Ramasser la pile 9V");
            } else {
                CollisionResults r = new CollisionResults();
                app.getRootNode().collideWith(
                    new Ray(app.getCamera().getLocation(), app.getCamera().getDirection()), r);
                Geometry cible = (r.size() > 0 && r.getClosestCollision().getDistance() < 3f)
                    ? r.getClosestCollision().getGeometry() : null;
                
                if (cible != null && estGenerateur(cible)) {
                    if (!generateurVu)          hud.setMessageInteraction("[E] Inspecter le generateur");
                    else if (generateurRepare || generateurDetruit)  hud.setMessageInteraction(""); 
                    else if (etatDaniel < 4)    hud.setMessageInteraction(""); 
                    else if (choixReparer)      hud.setMessageInteraction("[E] Reparer (" + pilesEnigme4 + "/2 piles)");
                    else hud.setMessageInteraction(inventaire.contient("Pied de biche")
                            ? "[E] SABOTER le generateur" : "[!] Il faut le pied de biche (salle 1)");
                } else if (cible != null && Boolean.TRUE.equals(cible.getUserData("Ramassable"))) {
                    hud.setMessageInteraction("[E] Ramasser : " + cible.getName());
                } else {
                    // Le hint [N] n'apparait qu'une fois la vision apprise (Dr. W)
                    hud.setMessageInteraction(((dansSalle3 || dansLeNoir) && !visionNocturne && etatDocteur >= 1)
                        ? "[N] Activer la vision nocturne" : "");
                }
            }
        }
    }

    private void majTeinteEcran(boolean dansLeNoir) {
        if (overlay == null) return;
        if (visionNocturne) {
            // Vision nocturne : teinte verte partout (revele le code en salle 3)
            overlay.setColor(new ColorRGBA(0.15f, 1.00f, 0.25f, 1f));
        } else if (dansLeNoir) {
            overlay.setColor(new ColorRGBA(0.02f, 0.02f, 0.03f, 1f)); // blackout
        } else {
            overlay.setColor(new ColorRGBA(1f, 1f, 1f, 1f));          // normal
        }
    }

    private void basculerVisionNuit() {
        // Verrouillee tant qu'on n'a pas vu le Dr. W — SAUF dans le blackout
        // (salle 4) ou il faut imperativement pouvoir voir pour survivre.
        if (etatDocteur < 1 && !enigme4Active) {
            hud.setMessageInteraction("Ces lunettes ont un mode etrange... quelqu'un doit savoir l'activer.");
            return;
        }
        visionNocturne = !visionNocturne;
        hud.setVisionNuit(visionNocturne);
    }

    @Override
    protected void cleanup(Application application) {
        app.getStateManager().detach(bullet);
        app.getRootNode().detachAllChildren();
        app.getRootNode().removeLight(ambiant);
        app.getRootNode().removeLight(soleil);
        for (PointLight pl : pointLights) app.getRootNode().removeLight(pl);
        pointLights.clear();
        if (fpp != null) { app.getViewPort().removeProcessor(fpp); fpp = null; }
        if (sirene != null) { sirene.stop(); sirene = null; }
        if (handsView != null) { app.getRenderManager().removeMainView(handsView); handsView = null; handsRoot = null; }
        app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(true);
        app.getInputManager().removeListener(actionListener);
        for (String m : new String[]{
            "Avancer","Reculer","Gauche","Droite",
            "Sprint","Accroupir","VisionNuit","Interagir","Sauter","VoirInventaire",
            "Digi0","Digi1","Digi2","Digi3","Digi4",
            "Digi5","Digi6","Digi7","Digi8","Digi9",
            "DigiEffacer","DigiValider","DigiFermer",
            "QTE_Haut","QTE_Bas","QTE_Gauche","QTE_Droite"
        }) {
            if (app.getInputManager().hasMapping(m))
                app.getInputManager().deleteMapping(m);
        }
        if (hud != null) hud.detacher();
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}