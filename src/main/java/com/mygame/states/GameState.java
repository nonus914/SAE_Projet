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

    private static final float AMBIENT_NORMAL = 0.15f; // props PBR visibles — le labo est Unshaded (ignore cet ambient)
    private final List<PointLight> pointLights = new ArrayList<>();
    private FilterPostProcessor fpp; // post-traitement : glow neon (Bloom)
    private ColorOverlayFilter overlay; // coupe/restaure la lumiere du labo (Unshaded)
    private Node handsRoot; // scene separee des mains (rendue par-dessus le monde)
    private com.jme3.renderer.ViewPort handsView;

    // ── Piles ramassables (auto-contact) ─────────────────────────────────────
    private final Node pilesNode = new Node("Piles");

    // ── Objets ramassables (raycasting E key — système Noah) ─────────────────
    private final Node objetsNode = new Node("Objets");

    // ── Enigmes — Digicodes ───────────────────────────────────────────────────
    private static final String CODE_E1       = "1945"; // enigme 1 : mot "AIDE" via tableau (A1Z26)
    private static final String CODE_E3       = "2703"; // enigme 3 (charade)
    private String porteCibleDigicode         = "";     // "001" ou "003"
    private static final float  ROOM3_Z_MIN   = 43f;  // R3 demarre a Z=40 — decalage de 3m
    private static final float  ROOM3_Z_MAX   = 88f;  // sombre jusqu'a la fin de R4 (R4 exit = Z=88)
    private boolean enigme3Resolue    = false;
    private String  codeEntree        = "";
    private float   digiCloseTimer    = -1f; // >0 = compte a rebours avant fermeture

    // ── Enigme 2 — Robot Daniel + Clé à molette (salle 2) ───────────────────
    private static final Vector3f POS_DANIEL = new Vector3f( 4.0f, 1.0f, 25.0f); // R2, decolle du mur
    // Cle a molette : au sol devant le rack mural (Y=0.2), dans l'empreinte X du rack
    // Visible & ramassable SEULEMENT en position accroupie (camera ~1.85m vs ~2.55m debout)
    private static final Vector3f POS_CLE    = new Vector3f(-8.6f, 0.2f, 36.0f); // R2 elargie : Z=21.5 → 36
    private static final Vector3f POS_CROWBAR = new Vector3f(-6f, 0.4f, 10f); // pied de biche — salle 1 (sabotage)
    private static final Vector3f POS_PILE    = new Vector3f(-9f, 0.3f, 50f); // 2e pile 9V — coin salle 3 (un peu cachee)
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
    private int     etatDaniel         = 0;     // 0 intro · 1 va ouvrir · 2 ouverte · 3 blackout-choix · 4 choix fait
    private String[] dialoguesCourants = DIALOGUES_DANIEL;

    // ── Enigme 4 — Blackout salle 4, generateur, alarme, QTE ─────────────────
    private static final float ROOM4_Z_MIN  = 65f;  // entree salle 4 (porte 003 a Z=64)
    private boolean enigme4Active     = false; // blackout declenche en salle 4
    private boolean generateurVu      = false; // generateur inspecte (→ va voir Daniel)
    private boolean generateurRepare  = false;
    private boolean generateurDetruit = false;
    private boolean choixReparer      = false;
    private int     pilesEnigme4      = 0;     // piles 9V (besoin de 2)
    private boolean alarmeActive      = false;
    private float   tempsAlarme       = 0f;
    private boolean qteActif          = false;
    private final String[] sequenceQTE = {"QTE_Haut","QTE_Haut","QTE_Droite","QTE_Bas"};
    private final String[] nomsQTE     = {"HAUT","HAUT","DROITE","BAS"};
    private int     qteIndex          = 0;
    private boolean victoireEnCours   = false;
    private float   finTimer          = -1f;
    private boolean enigme1Resolue    = false;
    private boolean fuiteSalle5       = false; // course finale salle 5
    private float   fuiteTimer        = 0f;
    private boolean porte004Ouverte   = false;
    private com.jme3.audio.AudioNode sirene = null;
    private Spatial spatialCrowbar    = null;
    private Spatial spatialPile       = null;
    private Geometry marqueurPile     = null;
    private Geometry marqueurCrowbar  = null;

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

        // Lumiere directionnelle douce pour les props (Daniel, lit, generateur...)
        // Le labo est Unshaded — cette lumiere n'affecte QUE les props GLB PBR
        soleil = new DirectionalLight();
        soleil.setDirection(new Vector3f(0f, -1f, 0.2f).normalizeLocal());
        soleil.setColor(ColorRGBA.White.mult(0.35f));
        app.getRootNode().addLight(soleil);

        // ── Point lights colores (eclairent uniquement les props GLB PBR) ────
        // Intensite moderee (x1.2) : props visibles sans blowout.
        // Le labo Unshaded ne reagit pas a ces lights (couleur fixe par code).
        //
        // R1 : bleu-cyan
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f,  4.0f), new ColorRGBA(0.40f, 0.72f, 1.0f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 11.0f), new ColorRGBA(0.30f, 0.65f, 1.0f, 1f), 1.2f, 22f);
        // R2 : vert
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 24.0f), new ColorRGBA(0.28f, 1.0f,  0.35f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 36.0f), new ColorRGBA(0.20f, 0.90f, 0.30f, 1f), 1.2f, 22f);
        // R3 → lumiere neutre tres douce (salle sombre mais props minimalement visibles)
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 52.0f), new ColorRGBA(0.15f, 0.10f, 0.08f, 1f), 0.8f, 24f);
        // R4 : cyan
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 70.0f), new ColorRGBA(0.05f, 0.80f, 1.0f, 1f), 1.2f, 22f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 82.0f), new ColorRGBA(0.10f, 0.70f, 0.90f, 1f), 1.2f, 22f);
        // R5 : violet/magenta
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f,  98.0f), new ColorRGBA(0.60f, 0.0f,  1.0f, 1f), 1.5f, 26f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 116.0f), new ColorRGBA(1.0f,  0.10f, 0.45f, 1f), 1.2f, 24f);
        ajouterLumiereAmbiante(new Vector3f( 0f, 2.5f, 130.0f), new ColorRGBA(0.60f, 0.0f,  1.0f, 1f), 1.5f, 22f);

        // ── Glow neon SELECTIF (BloomFilter, GlowMode.Objects) ───────────────
        // Seuls les materiaux qui definissent un GlowColor brillent (les neons,
        // marques dans Laboratory). Le tableau d'enigme, les murs et le sol ne
        // bavent plus. Cout GPU tres faible.
        fpp = new FilterPostProcessor(app.getAssetManager());
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(2.2f);    // force du halo neon
        bloom.setBlurScale(1.5f);         // etalement du halo
        fpp.addFilter(bloom);

        // Overlay plein ecran : coupe/restaure la lumiere du labo (Unshaded =
        // insensible aux lumieres 3D) et applique la vision nocturne. White = neutre.
        overlay = new ColorOverlayFilter(new ColorRGBA(1f, 1f, 1f, 1f));
        fpp.addFilter(overlay);

        app.getViewPort().addProcessor(fpp);

        // Sirene d'alarme (optionnelle : depose un fichier assets/Sounds/alarm.ogg)
        try {
            sirene = new com.jme3.audio.AudioNode(app.getAssetManager(), "Sounds/alarm.ogg",
                    com.jme3.audio.AudioData.DataType.Buffer);
            sirene.setLooping(true);
            sirene.setPositional(false);
            sirene.setVolume(2.5f);
            app.getRootNode().attachChild(sirene);
        } catch (Exception e) {
            sirene = null;
            System.out.println("[Audio] Sirene absente (Sounds/alarm.ogg) - son ignore.");
        }

        // Labo + portes
        doorManager = new Laboratory().construire(app.getAssetManager(), app.getRootNode(), bullet);

        // ── Lit salle 1 : agrandi (x1.75) et cale contre le MUR DU FOND (Z=-8),
        //    en face de la porte d'entree (Door_001 au centre, Z=16). Tete au mur,
        //    le lit s'etend vers la porte ; le joueur spawn dessus (cf PlayerControl).
        // GLB bbox : X -1.07→+0.03  Y 0→1.21  Z -0.03→+2.14 (longueur le long de Z).
        // Centrage X=0 : translateX = 0.91 (compense le centre local -0.52*1.75).
        // Tete au mur : face interieure du fond a Z=-7.5 → translateZ = -7.45.
        Spatial lit = app.getAssetManager().loadModel("Models/props/old_bed.glb");
        lit.setName("Lit_Salle1");
        lit.setLocalScale(1.75f);
        lit.setLocalTranslation(0.91f, 0f, -7.45f);
        app.getRootNode().attachChild(lit);
        System.out.println("[Salle1] Lit place. Bounds : " + lit.getWorldBound());

        // Générateur — room 4, mur gauche, juste après porte 003
        // Dimensions JME : 0.855m (X) × 1.084m (Y=hauteur) × 0.732m (Z) — origine au centre
        // Scale 1.5 → 1.28m × 1.63m × 1.10m — posé sur le sol (Y = demi-hauteur × scale)
        Spatial generator = app.getAssetManager().loadModel("Models/props/basic_generator.glb");
        generator.setName("Generateur_Interactif"); // interaction enigme 4 (etape suivante)
        generator.setLocalScale(1.5f);
        generator.setLocalTranslation(-9.0f, 0.813f, 68.5f); // R4 elargie : Z=43 → 68.5
        app.getRootNode().attachChild(generator);
        // Hitbox solide : on ne peut plus rentrer dans le generateur
        app.getRootNode().updateGeometricState();
        com.jme3.bullet.collision.shapes.CollisionShape formeGen =
                com.jme3.bullet.util.CollisionShapeFactory.createBoxShape(generator);
        com.jme3.bullet.control.RigidBodyControl corpsGen =
                new com.jme3.bullet.control.RigidBodyControl(formeGen, 0f);
        generator.addControl(corpsGen);
        bullet.getPhysicsSpace().add(corpsGen);

        // ── Mains du joueur — SCENE SEPAREE rendue par-dessus le monde ───────
        // Les bras s'etendent ~2m devant la camera : dans la scene principale ils
        // s'enfoncaient dans les murs/portes/Daniel des qu'on s'approchait. Ici ils
        // sont rendus dans un viewport dedie qui ne nettoie QUE le depth buffer →
        // toujours dessines DEVANT le monde, plus aucune penetration visuelle.
        handsRoot = new Node("HandsRoot");
        Node nodeCamera = new Node("nodeCamera");
        handsRoot.attachChild(nodeCamera);
        Spatial hands = app.getAssetManager().loadModel("Models/player/arms_throwing.glb");
        hands.rotate(0.2f, 0, 0);
        hands.setLocalScale(0.04f);
        nodeCamera.attachChild(hands);
        // Eclairage propre a la scene des mains (les lights du monde ne s'y appliquent pas)
        AmbientLight lumMains = new AmbientLight();
        lumMains.setColor(ColorRGBA.White.mult(1.0f));
        handsRoot.addLight(lumMains);
        DirectionalLight dirMains = new DirectionalLight();
        dirMains.setDirection(new Vector3f(-0.3f, -1f, 0.3f).normalizeLocal());
        dirMains.setColor(ColorRGBA.White.mult(0.8f));
        handsRoot.addLight(dirMains);
        handsView = app.getRenderManager().createMainView("MainsView", app.getCamera());
        handsView.setClearFlags(false, true, false); // depth uniquement
        handsView.attachScene(handsRoot);
        handsRoot.updateGeometricState();

        joueur   = new PlayerControl(bullet, app.getCamera(), nodeCamera, hands);
        batterie = new BatterieManager(600f);
        inventaire = new Inventory();

        // Piles (auto-contact)
        app.getRootNode().attachChild(pilesNode);
        placerPile(new Vector3f( 2f, 1f, 11.5f)); // R1 elargie : Z=5 → 11.5
        placerPile(new Vector3f(-2f, 1f, 34.0f)); // R2 elargie : Z=20 → 34
        placerPile(new Vector3f( 1f, 1f, 58.0f)); // R3 elargie : Z=36 → 58

        // Node objets ramassables conserve (blocs de test rouge/jaune retires)
        app.getRootNode().attachChild(objetsNode);

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

        // Orientation initiale : le joueur (sur le lit, au fond) regarde la porte
        // d'entree, droit devant vers +Z (Door_001 a Z=16). FlyByCamera applique
        // ensuite les mouvements souris a partir de cette direction.
        app.getCamera().lookAtDirection(new Vector3f(0f, 0f, 1f), Vector3f.UNIT_Y);

        // Verrouiller les portes à code
        doorManager.verrouiller("001"); // enigme 1 — Noah
        doorManager.verrouiller("002"); // enigme 2 — clé à molette
        doorManager.verrouiller("003"); // enigme 3 — charade
        doorManager.verrouiller("004"); // enigme 4 — generateur (etape suivante)

        // ── Enigme 2 — Robot Daniel (PNJ salle 2) ────────────────────────────
        spatialDaniel = app.getAssetManager().loadModel("Models/props/robot_daniel.glb");
        spatialDaniel.setName("Daniel_Robot");
        // Plus grand que le joueur (yeux a 1.7m) → sa tete reste au-dessus de la
        // camera, elle ne peut plus etre "coupee" par le near-plane en s'approchant.
        spatialDaniel.setLocalScale(2.4f);
        // Sur le cote de la salle 2 — rotation 180° par rapport a avant
        spatialDaniel.rotate(0f, -FastMath.HALF_PI, 0f);
        spatialDaniel.setLocalTranslation(POS_DANIEL.x, 0f, POS_DANIEL.z);
        app.getRootNode().attachChild(spatialDaniel);
        // Hitbox solide : noeud invisible + RigidBodyControl — EXACTEMENT le
        // pattern des portes (DoorManager), qui bloque le joueur de facon fiable.
        Node hitboxDaniel = new Node("Daniel_Hitbox");
        hitboxDaniel.setLocalTranslation(POS_DANIEL.x, 1.5f, POS_DANIEL.z);
        app.getRootNode().attachChild(hitboxDaniel);
        com.jme3.bullet.control.RigidBodyControl rbcDaniel =
                new com.jme3.bullet.control.RigidBodyControl(
                        new com.jme3.bullet.collision.shapes.BoxCollisionShape(
                                new Vector3f(0.7f, 1.5f, 0.7f)), 0f);
        hitboxDaniel.addControl(rbcDaniel);
        bullet.getPhysicsSpace().add(rbcDaniel);
        System.out.println("[Enigme2] Daniel charge + hitbox (pattern portes).");

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

        // ── Enigme 4 — pied de biche (salle 1) + 2e pile 9V (salle 3) ─────────
        // NB : le pied de biche et la pile 9V n'apparaissent qu'au moment du
        // choix chez Daniel (enigme 4) — pas visibles avant (cf apparaitre*()).

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

        // QTE (forcage porte 004) — fleches directionnelles
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
        // Digicode intercept — si le panneau est ouvert, bloquer les autres actions
        if (hud.isDigicodeOuvert()) {
            // Bouger ferme le digicode → on peut s'eloigner et relire le tableau
            boolean mouvement = name.equals("Avancer") || name.equals("Reculer")
                             || name.equals("Gauche")  || name.equals("Droite")
                             || name.equals("Sprint")  || name.equals("Sauter");
            if (mouvement) {
                hud.fermerDigicode();
                codeEntree = "";
                // pas de return : le mouvement est traite normalement ci-dessous
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
                        // Chiffre 0-9
                        String chiffre = name.replace("Digi", "");
                        if (codeEntree.length() < 4) {
                            codeEntree += chiffre;
                            hud.updateDigicode(codeEntree);
                            if (codeEntree.length() == 4) validerCodeDigicode();
                        }
                    }
                }
                return; // bloquer le reste tant que le panneau est ouvert
            }
        }

        // ── Dialogue Daniel ouvert — choix [1]/[2] + avancer ─────────────────
        if (hud.isDialogueOuvert()) {
            if (!isPressed) return;
            if (etatDaniel == 3) { // blackout : choix reparer / saboter
                if (name.equals("Digi1")) {
                    choixReparer = true; etatDaniel = 4; pilesEnigme4 = 1; // Daniel donne 1 pile
                    apparaitrePile(); // la 2e pile apparait en salle 3 (anneau lumineux)
                    dialoguesCourants = new String[]{
                        "Tiens, ma derniere pile 9V. Il en faut DEUX pour le generateur.",
                        "Trouve la 2e pile (salle 3), puis remets le courant !"
                    };
                    enigme2DiagIdx = 0;
                    hud.setLigneDialogue("Daniel", dialoguesCourants[0]);
                    return;
                } else if (name.equals("Digi2")) {
                    choixReparer = false; etatDaniel = 4;
                    apparaitreCrowbar(); // le pied de biche apparait en salle 1 (anneau lumineux)
                    dialoguesCourants = new String[]{
                        "La methode forte, hein ? Va chercher le PIED DE BICHE en salle 1.",
                        "Sabote le generateur, puis FORCE la porte de sortie (004)."
                    };
                    enigme2DiagIdx = 0;
                    hud.setLigneDialogue("Daniel", dialoguesCourants[0]);
                    return;
                }
            }
            if (name.equals("Interagir")) avancerDialogueEnigme2();
            return;
        }

        // ── QTE — forcage porte 004 (le prompt est affiche par update) ───────
        if (qteActif) {
            if (!isPressed) return;
            joueur.setForward(false); joueur.setBackward(false);
            joueur.setLeft(false);    joueur.setRight(false);
            if (name.equals(sequenceQTE[qteIndex])) {
                qteIndex++;
                if (qteIndex >= sequenceQTE.length) {
                    qteActif = false;
                    doorManager.deverrouiller("004");
                    doorManager.interagir();
                    porte004Ouverte = true;
                    demarrerFuite();
                }
            } else if (name.startsWith("QTE_") || name.equals("Interagir")) {
                qteIndex = 0; // mauvaise touche → on recommence la sequence
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
                    // 1. Raycast — digicode salle 1 + generateur salle 4
                    CollisionResults resultats = new CollisionResults();
                    Ray rayon = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
                    app.getRootNode().collideWith(rayon, resultats);
                    if (resultats.size() > 0 && resultats.getClosestCollision().getDistance() < 3.0f) {
                        Geometry cible = resultats.getClosestCollision().getGeometry();
                        if (estGenerateur(cible)) { interagirGenerateur(); return; }
                    }

                    // 2. Enigme 2 — Ramasser la cle a molette (accroupi obligatoire)
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

                    // 2b. Enigme 4 — Ramasser le pied de biche (salle 1, sabotage)
                    if (spatialCrowbar != null && proche(POS_CROWBAR, 2.5f)) {
                        spatialCrowbar.removeFromParent();
                        spatialCrowbar = null;
                        if (marqueurCrowbar != null) { marqueurCrowbar.removeFromParent(); marqueurCrowbar = null; }
                        inventaire.ajouter("Pied de biche");
                        hud.setMessageInteraction("Pied de biche recupere ! Sabote le generateur.");
                        return;
                    }

                    // 2c. Enigme 4 — Ramasser la 2e pile 9V (reparation)
                    if (spatialPile != null && proche(POS_PILE, 2.5f)) {
                        spatialPile.removeFromParent();
                        spatialPile = null;
                        if (marqueurPile != null) { marqueurPile.removeFromParent(); marqueurPile = null; }
                        pilesEnigme4++;
                        inventaire.ajouter("Pile 9V");
                        hud.setMessageInteraction("Pile 9V ajoutee a l'inventaire (" + pilesEnigme4 + "/2) !");
                        return;
                    }

                    // 3. Enigme 2/4 — Parler a Daniel (proximite)
                    if (spatialDaniel != null && proche(POS_DANIEL, 3.5f)) {
                        ouvrirDialogueDaniel();
                        return;
                    }

                    // 4. Porte verrouillee par proximite
                    if (doorManager.isPorteProcheVerrouillee()) {
                        String numPorte = doorManager.getNumeroPorteProche();
                        if ("002".equals(numPorte)) {
                            // La porte 002 ne s'ouvre QUE via Daniel (la cle seule ne suffit pas).
                            hud.setMessageInteraction(enigme2CleRamassee
                                ? "[!] Rapporte la cle a Daniel pour qu'il ouvre !"
                                : "[!] Porte cassee - trouve un outil pour Daniel");
                        } else if ("004".equals(numPorte)) {
                            if ((generateurRepare || generateurDetruit) && !qteActif) {
                                qteActif = true; // combinaison pour ouvrir (les 2 voies)
                                qteIndex = 0;
                                hud.setMessageInteraction("");
                            } else if (!generateurRepare && !generateurDetruit) {
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

    /** Ouvre le digicode pour une porte et affiche l'indice adapte. */
    private void ouvrirDigicodePorte(String porte) {
        porteCibleDigicode = porte;
        codeEntree = "";
        hud.ouvrirDigicode();
        hud.updateDigicode("");
        hud.setIndiceDigicode("001".equals(porte)
            ? "Mot a decoder (tableau) :  A I D E"
            : "Indice : la charade (salle sombre)");
        // Stopper le mouvement en cours (sinon le joueur reste colle a la porte)
        joueur.setForward(false); joueur.setBackward(false);
        joueur.setLeft(false);    joueur.setRight(false);
        app.getInputManager().setCursorVisible(false);
    }

    private void validerCodeDigicode() {
        // Choisir le bon code selon la porte ciblée
        String bonCode = "001".equals(porteCibleDigicode) ? CODE_E1 : CODE_E3;

        if (codeEntree.equals(bonCode)) {
            hud.setFeedbackDigicode("CODE ACCEPTE !", ColorRGBA.Green);
            doorManager.deverrouiller(porteCibleDigicode);
            if ("001".equals(porteCibleDigicode)) enigme1Resolue = true;

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

    private void ouvrirDialogueDaniel() {
        if (etatDaniel == 0) {
            if (enigme2CleRamassee) {
                etatDaniel = 1; // a la fin du dialogue → Daniel ouvre la porte 002
                dialoguesCourants = new String[]{
                    "Ah, la cle a molette ! Parfait, merci !",
                    "Je te deverrouille la porte. Vas-y, file !"
                };
            } else {
                dialoguesCourants = DIALOGUES_DANIEL;
            }
        } else if (etatDaniel == 2) {
            dialoguesCourants = new String[]{"La porte est ouverte, vas-y ! Fais attention a toi."};
        } else if (etatDaniel == 3) {
            dialoguesCourants = new String[]{
                "Le courant a saute ! Choisis :\n"
              + "  1 - REPARER le generateur (2 piles 9V)\n"
              + "  2 - SABOTER le labo (pied de biche)\n"
              + "  (Appuie sur 1 ou 2)"
            };
        } else { // etatDaniel == 4 : rappel de la consigne
            dialoguesCourants = choixReparer
                ? new String[]{"Repare le generateur : il faut 2 piles 9V."}
                : new String[]{"Recupere le pied de biche (salle 1) et sabote le generateur."};
        }
        enigme2DiagIdx = 0;
        hud.ouvrirDialogue("Daniel", dialoguesCourants[0]);
    }

    private void avancerDialogueEnigme2() {
        enigme2DiagIdx++;
        if (enigme2DiagIdx < dialoguesCourants.length) {
            hud.setLigneDialogue("Daniel", dialoguesCourants[enigme2DiagIdx]);
        } else if (etatDaniel == 3) {
            enigme2DiagIdx = dialoguesCourants.length - 1; // reste sur le choix (1/2)
        } else {
            enigme2DiagIdx = 0;
            hud.fermerDialogue();
            if (etatDaniel == 1) { // Daniel deverrouille la porte 002
                doorManager.deverrouiller("002");
                etatDaniel = 2;
                enigme2Resolue = true;
                hud.setMessageInteraction("Daniel a deverrouille la porte 002 !");
            }
        }
    }

    /** Interaction avec le generateur (salle 4). */
    private void interagirGenerateur() {
        if (generateurRepare || generateurDetruit) return;
        if (!generateurVu) {
            generateurVu = true;
            etatDaniel = 3; // Daniel propose desormais le choix reparer/saboter
            hud.setMessageInteraction(""); // la directive s'affiche dans le cadre (cf update)
            return;
        }
        if (choixReparer) {
            if (pilesEnigme4 >= 2) {
                generateurRepare = true; // la lumiere revient (blackout off)
                hud.setMessageInteraction("Generateur REPARE ! Va a la porte 004 et entre la combinaison.");
            } else {
                hud.setMessageInteraction("Il manque une pile 9V (" + pilesEnigme4 + "/2).");
            }
        } else {
            if (inventaire.contient("Pied de biche")) {
                generateurDetruit = true; // on reste dans le noir
                hud.setMessageInteraction("Generateur SABOTE ! Va a la porte 004 et entre la combinaison.");
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

    /** Demarre la course de fuite (salle 5) : alarme rouge + sirene + chrono. */
    private void demarrerFuite() {
        fuiteSalle5  = true;
        alarmeActive = true;
        fuiteTimer   = 10f;
        if (sirene != null) sirene.play();
        hud.setMessageInteraction("");
    }

    /** Objectif courant (affiche dans le cadre HUD), selon l'avancee du jeu. */
    private String calculerObjectif() {
        if (fuiteSalle5)   return "FUIS ! Atteins le fond du labo (" + Math.max(0, (int) Math.ceil(fuiteTimer)) + "s)";
        if (qteActif)      return "Combinaison : appuie sur " + nomsQTE[qteIndex];
        if (generateurRepare || generateurDetruit)
                           return "Porte de sortie (004) : entre la combinaison.";
        if (enigme4Active) {
            if (!generateurVu)  return "Coupure de courant ! Inspecte le generateur (salle 4).";
            if (etatDaniel < 4) return "Retourne parler a Daniel (salle 2).";
            if (choixReparer)   return "Trouve 2 piles 9V (" + pilesEnigme4 + "/2) puis repare le generateur.";
            return inventaire.contient("Pied de biche")
                ? "Sabote le generateur (salle 4)."
                : "Recupere le pied de biche (salle 1).";
        }
        if (enigme3Resolue)     return "Avance vers la salle 4.";
        if (enigme2Resolue)     return "Salle sombre : resous la charade. [N] vision nocturne.";
        if (etatDaniel >= 1)    return "Daniel ouvre la porte... avance !";
        if (enigme2CleRamassee) return "Rapporte la cle a molette a Daniel (salle 2).";
        if (enigme1Resolue)     return "Salle 2 : trouve la cle (accroupi) et parle a Daniel.";
        return "Salle 1 : ouvre le digicode de la porte et decode le mot avec le tableau.";
    }

    /** Anneau lumineux flottant AU-DESSUS d'un objet (pas dessus) — repere visuel. */
    private Geometry anneauLumineux(Vector3f pos, ColorRGBA couleur) {
        Geometry g = new Geometry("Anneau",
                new com.jme3.scene.shape.Torus(24, 8, 0.04f, 0.45f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", couleur);
        mat.setColor("GlowColor", couleur); // halo via bloom → visible dans le noir
        g.setMaterial(mat);
        g.rotate(FastMath.HALF_PI, 0, 0); // a plat (horizontal)
        g.setLocalTranslation(pos.x, pos.y + 1.1f, pos.z);
        app.getRootNode().attachChild(g);
        return g;
    }

    /** Fait apparaitre la 2e pile 9V (choix REPARER chez Daniel). */
    private void apparaitrePile() {
        if (spatialPile != null) return;
        spatialPile  = chargerProp("Models/props/9v_battery.glb", "Pile_9V_2", POS_PILE, 0.4f);
        marqueurPile = anneauLumineux(POS_PILE, new ColorRGBA(0.7f, 1f, 0.1f, 1f)); // vert-jaune
    }

    /** Fait apparaitre le pied de biche (choix SABOTER chez Daniel). */
    private void apparaitreCrowbar() {
        if (spatialCrowbar != null) return;
        spatialCrowbar  = chargerProp("Models/props/crowbar.glb", "Pied_De_Biche", POS_CROWBAR, 1.0f);
        marqueurCrowbar = anneauLumineux(POS_CROWBAR, new ColorRGBA(1f, 0.5f, 0f, 1f)); // orange
    }

    /** Charge un prop GLB et le redimensionne a une taille cible (m), quelle que
     *  soit son echelle native. Le ramassage reste gere par proximite. */
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

        // ── Fin de partie (victoire) — fige le jeu puis bascule sur WinState ──
        if (victoireEnCours) {
            finTimer -= tpf;
            if (finTimer <= 0) {
                app.getStateManager().detach(this);
                app.getRootNode().detachAllChildren();
                app.getStateManager().attach(new WinState());
            }
            return;
        }

        joueur.update(tpf);

        // Scene des mains (viewport separe) : a mettre a jour manuellement
        if (handsRoot != null) {
            handsRoot.updateLogicalState(tpf);
            handsRoot.updateGeometricState();
        }

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

        // ── Blackout GLOBAL / alarme (rendu via overlay, labo Unshaded) ──────
        if (enigme3Resolue && !enigme4Active && pos.z > ROOM4_Z_MIN) {
            enigme4Active = true; // une fois entre en salle 4 → coupure de courant
        }
        boolean room3Noir      = !enigme3Resolue && pos.z > ROOM3_Z_MIN && pos.z < 64f;
        boolean blackoutGlobal = enigme4Active && !generateurRepare; // TOUT le labo, persistant
        boolean dansLeNoir     = room3Noir || blackoutGlobal;

        if (alarmeActive) {
            tempsAlarme += tpf;
            float pulse = FastMath.pow(FastMath.sin(tempsAlarme * 6f), 2f); // 0..1
            overlay.setColor(new ColorRGBA(0.45f + 0.55f * pulse, 0.03f, 0.03f, 1f)); // rouge clignotant
        } else {
            majTeinteEcran(dansLeNoir);
        }

        // ── Course de fuite (salle 5) : alarme rouge + chrono + drain batterie ─
        if (fuiteSalle5 && !victoireEnCours) {
            fuiteTimer -= tpf;
            batterie.drainer(tpf * 20f); // la course pompe la batterie
            if (pos.z > 130f) {                       // atteint le fond du labo (mur Z=136)
                declencherVictoire("VOUS VOUS ECHAPPEZ DU LABORATOIRE !\nVICTOIRE !");
            } else if (fuiteTimer <= 0f || batterie.isGameOver()) {
                if (sirene != null) sirene.stop();
                app.getStateManager().detach(this);
                app.getRootNode().detachAllChildren();
                app.getStateManager().attach(new GameOverState());
                return;
            }
        }

        // ── Charade salle 3 (texte vert) + banniere OBJECTIF dynamique ───────
        hud.setCharade(room3Noir && visionNocturne ? CHARADE : "");
        if (!victoireEnCours) hud.setObjectifEncadre(calculerObjectif());

        // ── Hints interaction (porte / NPC / objet) ───────────────────────────
        // Ne pas écraser le message si dialogue ou digicode est ouvert
        // Quand un panneau est ouvert, on n'affiche pas le hint central (evite la superposition)
        if (hud.isDigicodeOuvert() || hud.isDialogueOuvert()) hud.setMessageInteraction("");

        // Anneaux lumineux : rotation lente (repere visuel au-dessus des objets)
        if (marqueurPile != null)    marqueurPile.rotate(0, 0, tpf * 2f);
        if (marqueurCrowbar != null) marqueurCrowbar.rotate(0, 0, tpf * 2f);

        if (!hud.isDialogueOuvert() && !hud.isDigicodeOuvert() && !qteActif) {
            boolean porteProche = doorManager.update(pos);
            if (porteProche && doorManager.isPorteProcheVerrouillee()) {
                String np = doorManager.getNumeroPorteProche();
                if ("002".equals(np)) {
                    hud.setMessageInteraction(enigme2CleRamassee
                        ? "[!] Rapporte la cle a Daniel pour qu'il ouvre"
                        : "[!] Porte cassee - trouve un outil pour Daniel");
                } else if ("004".equals(np)) {
                    hud.setMessageInteraction(
                        (generateurDetruit && inventaire.contient("Pied de biche"))
                            ? "[E] FORCER la porte (pied de biche)"
                            : "[!] Verrouillee : generateur hors service");
                } else {
                    hud.setMessageInteraction("[E] Entrer le code Digicode");
                }
            } else if (porteProche) {
                hud.setMessageInteraction("[E] Ouvrir la porte");
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
                    else if (generateurRepare)  hud.setMessageInteraction("");
                    else if (etatDaniel < 4)    hud.setMessageInteraction(""); // doit d'abord voir Daniel
                    else if (choixReparer)      hud.setMessageInteraction("[E] Reparer (" + pilesEnigme4 + "/2 piles)");
                    else hud.setMessageInteraction(inventaire.contient("Pied de biche")
                            ? "[E] SABOTER le generateur" : "[!] Il faut le pied de biche (salle 1)");
                } else if (cible != null && Boolean.TRUE.equals(cible.getUserData("Ramassable"))) {
                    hud.setMessageInteraction("[E] Ramasser : " + cible.getName());
                } else {
                    hud.setMessageInteraction((dansLeNoir && !visionNocturne) ? "[N] Activer la vision nocturne" : "");
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Coupe ou restaure la lumiere du labo via l'overlay plein ecran (le labo est
     * Unshaded → insensible aux lumieres 3D, donc on teinte l'image finale).
     *  • noir sans vision nuit → quasi-noir
     *  • noir + vision nuit    → teinte verte lisible
     *  • lumiere normale       → blanc (aucun effet)
     */
    private void majTeinteEcran(boolean dansLeNoir) {
        if (overlay == null) return;
        if (dansLeNoir && !visionNocturne) {
            overlay.setColor(new ColorRGBA(0.02f, 0.02f, 0.03f, 1f)); // quasi-noir
        } else if (dansLeNoir && visionNocturne) {
            overlay.setColor(new ColorRGBA(0.15f, 1.00f, 0.25f, 1f)); // vision nocturne
        } else {
            overlay.setColor(new ColorRGBA(1f, 1f, 1f, 1f));          // lumiere normale
        }
    }

    private void basculerVisionNuit() {
        visionNocturne = !visionNocturne;
        hud.setVisionNuit(visionNocturne);
        // L'effet (vert / noir) est applique chaque frame par majTeinteEcran(),
        // via le filtre overlay — compatible avec le labo Unshaded.
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
