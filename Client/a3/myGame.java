package Client.a3;
//Samantha Trevino and Sean Hobson
//Spring '22, CSC 165, Final Project
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;
import tage.networking.IGameConnection;
import tage.physics.PhysicsEngineFactory;
import tage.shapes.*;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;

import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.nodeControllers.BounceController;
import tage.nodeControllers.RotationController;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Random;

//Script imports
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import javax.script.Invocable;
//Physics imports
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;

import static tage.Utils.toDoubleArray;

public class myGame extends VariableFrameRateGame
{
    private static Engine engine;
    private Camera cam;
    private CameraOrbit3D cam3D;
    public Bullet bul;

    public static Engine getEngine() { return engine; }

    private int score;
    private double startTime;

    private GameObject player, x, y, z;
    private GameObject prize, prize2, prize3, ground;
    private ObjShape playerS, linxS, linyS, linzS, groundS, ghostS, prizeS1, prizeS2, prizeS3, modelGhost;
    private TextureImage prizeT1, prizeT2, prizeT3, spikeT, doltx, groundT, ghostT, hills, ghostModelT, carTexture;
    private Light ambLight, dirLight;
    private NodeController rc, bc;
    private double deltaTime, prevTime, elapsedTime, amt; //variables for speed movement based on time

    private boolean onDolphin, axesOn;
    private Vector3f dolFwd, dolLoc;
    private Matrix4f currentT;

    private InputManager im;
    public Random rand = new Random();

    private int skyboxTexture; // skyboxes

    //Networking variables.
    private GhostManager gm;
    private String serverAddress;
    private int serverPort;
    private IGameConnection.ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private boolean isClientConnected = false;
    //Script variables.
    private File scriptFile2, scriptFile3;
    private long fileLastModifiedTime = 0;
    ScriptEngine jsEngine;
    //Variables for physics
    private GameObject ball1, ball2;
    private PhysicsEngine physicsEngine;
    private JBulletPhysicsEngine jBulletPE;
    private PhysicsObject ball1P, ball2P, planeP, playerP;
    private boolean running = false;
    private float vals[] = new float[16];
    //NPC variables
    private ObjShape npcShape;
    private TextureImage npcTex;


    public myGame(String serverAddress, int serverPort, String protocol)
    {
        //String serverAddress, int serverPort, String protocol, add or subtract this statement to add or remove networking
        super();
        gm = new GhostManager(this);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
       // String protocol = "TCP"; this was for testing
        if (protocol.toUpperCase().compareTo("TCP") == 0)
            this.serverProtocol = IGameConnection.ProtocolType.TCP;
        else
            this.serverProtocol = IGameConnection.ProtocolType.UDP;
    }

    public static void main(String[] args)
    {
        myGame game = new myGame(args[0], Integer.parseInt(args[1]), args[2]);
        //myGame game = new myGame();
        engine = new Engine(game);
        //Script code
        ScriptEngineManager factory = new ScriptEngineManager();
        String scriptFileName = "assets/scripts/hello.js";

        // get the JavaScript engine
        ScriptEngine jsEngine = factory.getEngineByName("js");

        // run the script
        game.executeScript(jsEngine, scriptFileName);

        game.initializeSystem();
        game.game_loop();
    }
    
    public GameObject getPlayerModel() {return player; }
    public boolean getDolphinStatus() { return onDolphin; }
    public Camera getCam() { return cam; }
    public ObjShape getNPCshape() { return npcShape; }
    public TextureImage getNPCtexture() { return npcTex; }

    @Override
    public void loadShapes()
    {
        playerS = new ImportedModel("triangleCar.obj");
        prizeS1 = new Torus();
        prizeS2 = new Torus();
        prizeS3 = new Torus();
        linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
        linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
        linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));

        //For terrain requirement.
        groundS = new TerrainPlane(1000);
        //For model for extra players (for multi-player)
        ghostS = new ImportedModel("triangleGhost.obj");
        //Shape for imported model
        modelGhost = new ImportedModel("triangleGhost.obj");

    }

    @Override
    public void loadTextures()
    {
        doltx = new TextureImage("Dolphin_HighPolyUV.png");
        //For terrain
        hills = new TextureImage("hills.jpg"); 
        groundT = new TextureImage("grass.jpg");
        //For model for extra players (for multi-player)
        ghostT = new TextureImage("redGhostTexture.png");
        //Texture for imported model
        ghostModelT = new TextureImage("ghostTexture.png");
        //Car model texture
        carTexture = new TextureImage("carUVText.png");
    }

    @Override
    public void buildObjects()
    {	Matrix4f initialTranslation, initialScale;
        deltaTime = 0.0f;
       // prevTime = 0.0f;
        // build dolphin in the center of the window
        player = new GameObject(GameObject.root(), playerS, carTexture);
        initialTranslation = (new Matrix4f()).translation(0,-0.5f,0);
        initialScale = (new Matrix4f()).scaling(0.2f);
        player.setLocalTranslation(initialTranslation);
        player.setLocalScale(initialScale);

        //Build enemy
        prize = new GameObject(GameObject.root(),modelGhost,ghostModelT);
        prize.setLocalTranslation((new Matrix4f()).translation(3,0.5f,0));
        prize.setLocalScale((new Matrix4f()).scaling(0.3f));

        //second enemy
        prize2 = new GameObject(GameObject.root(), modelGhost, ghostModelT);
        prize2.setLocalTranslation((new Matrix4f()).translation(5,0.5f,3));
        prize2.setLocalScale((new Matrix4f()).scaling(0.3f));

        //third enemy
        prize3 = new GameObject(GameObject.root(), modelGhost, ghostModelT);
        prize3.setLocalTranslation((new Matrix4f()).translation(-4,0.5f,-4));
        prize3.setLocalScale((new Matrix4f()).scaling(0.3f));


        //-------------Ground object-------------------------
        ground = new GameObject(GameObject.root(), groundS, groundT);
        ground.setLocalTranslation((new Matrix4f()).translation(0,0,0));
        ground.setLocalScale((new Matrix4f()).scaling(15.0f));
        ground.setHeightMap(hills);

        //physics test ball 1
        ball1 = new GameObject(GameObject.root(), new Sphere(), doltx);
        ball1.setLocalTranslation((new Matrix4f()).translation(0.0f, 4.0f, 0.0f));
        ball1.setLocalScale((new Matrix4f()).scaling(0.50f));
        //physics test ball 2
        ball2 = new GameObject(GameObject.root(), new Sphere(), doltx);
        ball2.setLocalTranslation((new Matrix4f()).translation(-0.5f, 1.0f, 0.0f));
        ball2.setLocalScale((new Matrix4f()).scaling(0.50f));

        //Imported model - Ghost
       // ghostMO = new GameObject(GameObject.root(), modelGhost, ghostModelT);

        //Axes lines
        x = new GameObject(GameObject.root(), linxS);
        y = new GameObject(GameObject.root(), linyS);
        z = new GameObject(GameObject.root(), linzS);
        (x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
        (y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
        (z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));

    }
    @Override
    public void createViewports()
    {
        (engine.getRenderSystem()).addViewport("MAIN",0,0,1f,1f);
        (engine.getRenderSystem()).addViewport("MAP",.75f,0,.25f,.25f);
        Camera camMap = (engine.getRenderSystem()).getViewport("MAP").getCamera();

         Viewport mapVP = (engine.getRenderSystem()).getViewport("MAP");

        mapVP.setHasBorder(true);
        mapVP.setBorderWidth(3);
        mapVP.setBorderColor(0.0f, 1.0f, 1.0f);

        camMap.setLocation(new Vector3f(0,6,0));
        camMap.setU(new Vector3f(1,0,0));
        camMap.setV(new Vector3f(0,0,1));
        camMap.setN(new Vector3f(0,-1,0));
    }

    @Override
    public void loadSkyBoxes()
    {
        skyboxTexture = (engine.getSceneGraph()).loadCubeMap("nightSky","png");
        (engine.getSceneGraph()).setActiveSkyBoxTexture(skyboxTexture);
        (engine.getSceneGraph()).setSkyBoxEnabled(true);
    }

    @Override
    public void initializeGame()
    {
        prevTime = System.currentTimeMillis();
        startTime = System.currentTimeMillis();
        (engine.getRenderSystem()).setWindowDimensions(1900,1000);
        score = 0;
        onDolphin = true;

        // -------------- adding node controllers -----------
        bc = new BounceController(engine, 1.0f);
        (engine.getSceneGraph()).addNodeController(bc);
        bc.addTarget(prize);
        bc.addTarget(prize2);
        bc.addTarget(prize3);
        bc.enable();

        //----------------- adding lights -----------------
        Light.setGlobalAmbient(0.3f, 0.3f, 0.3f);
        ambLight = new Light();
        dirLight = new Light();
        ambLight.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
        //posLight.setLocation(new Vector3f(7.0f, 4.0f, 1.0f));

        player.getLocalForwardVector();
        dirLight.setType(Light.LightType.SPOTLIGHT);
        dirLight.setLocation(player.getWorldLocation());
        dirLight.setDirection(player.getLocalForwardVector());
        (engine.getSceneGraph()).addLight(ambLight);
        engine.getSceneGraph().addLight(dirLight);

        //--------------Initialize camera -------------------
        im = engine.getInputManager();
        Camera cam = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
        cam3D = new CameraOrbit3D (cam, player, im.getFirstGamepadName(), engine, this);

        //------------- Other Inputs Section -----------------
        //setupNetworking();
        FwdAction fwdAction = new FwdAction(this, protClient);
        TurnAction turnAction = new TurnAction(this);

        // attach the action objects to gamepad components
        String gpName = im.getFirstGamepadName();
        im.associateAction(gpName,
                net.java.games.input.Component.Identifier.Axis.Y,
                fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gpName,
                net.java.games.input.Component.Identifier.Axis.X,
                turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        //-------------Script section ---------------------
        // initialize the scripting engine
        ScriptEngineManager factory = new ScriptEngineManager();
        java.util.List<ScriptEngineFactory> list = factory.getEngineFactories();
        jsEngine = factory.getEngineByName("js");

        // add the light specified in the script to the game world
        scriptFile2 = new File("assets/scripts/CreateLight.js");
        this.runScript(scriptFile2);
        (engine.getSceneGraph()).addLight((Light)jsEngine.get("light"));

        // set up the script that associates the light color with the space bar
        scriptFile3 = new File("assets/scripts/UpdateLightColor.js");
        this.runScript(scriptFile3);


        //------------- PHYSICS --------------
        // --- initialize physics system ---
        String engine = "tage.physics.JBullet.JBulletPhysicsEngine";
        float[] gravity = {0f, -10, 0f};
        physicsEngine = PhysicsEngineFactory.createPhysicsEngine(engine);
        physicsEngine.initSystem();
        physicsEngine.setGravity(gravity);

        //  --- create physics world ---
        float mass = 1.0f;
        float up[] = {0,1,0};
        double[] tempTransform;

        Matrix4f translation = new Matrix4f(ball1.getLocalTranslation());
        tempTransform = toDoubleArray(translation.get(vals));
        ball1P = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
        ball1P.setBounciness(1.0f);
        ball1.setPhysicsObject(ball1P);

        translation = new Matrix4f(ball2.getLocalTranslation());
        tempTransform = toDoubleArray(translation.get(vals));
        ball2P = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
        ball2P.setBounciness(1.0f);
        ball2.setPhysicsObject(ball2P);

        translation = new Matrix4f(ground.getLocalTranslation());
        tempTransform = toDoubleArray(translation.get(vals));
        planeP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), tempTransform, up, 0.0f);
        planeP.setBounciness(1.0f);
        ground.setPhysicsObject(planeP);


    } //-----End of initializeGame -----


    @Override //Things you want to happen constantly / updates with every frame
    public void update()
    {

        double totalTime = System.currentTimeMillis() - startTime;
        elapsedTime = System.currentTimeMillis() - prevTime;
        prevTime = System.currentTimeMillis();
        amt = elapsedTime * 0.03;
        double amtt = totalTime * 0.001;

        // build and set HUD
        cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
        hudManagement((int) totalTime);

        //Terrain height stuff.
         Vector3f loc = player.getWorldLocation();
         float height = ground.getHeight(loc.x(), loc.z()); 
         player.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));
         updateLight();
        // update physics
        if (running) {
            Matrix4f mat = new Matrix4f();
            Matrix4f mat2 = new Matrix4f().identity();
            checkForCollisions();
            physicsEngine.update((float)elapsedTime);
            for (GameObject go:engine.getSceneGraph().getGameObjects())
            {
                if (go.getPhysicsObject() != null)
                {	mat.set(Utils.toFloatArray(go.getPhysicsObject().getTransform()));

                    mat2.set(3,0,mat.m30()); mat2.set(3,1,mat.m31()); mat2.set(3,2,mat.m32());
                    go.setLocalTranslation(mat2);
                }
            }
        }
        // update inputs and camera
        im.update((float)elapsedTime);
        collectPrize();
        
        processNetworking((float)elapsedTime);
    }//End of update

    public void updateLight()
    {
        dirLight.setLocation(player.getWorldLocation());
        dirLight.setDirection(player.getWorldForwardVector());
    }
    //-----------NETWORKING METHODS------------
    public ObjShape getGhostShape() { return ghostS; }
    public TextureImage getGhostTexture() { return ghostT; }
    public GhostManager getGhostManager() { return gm; }

    private void setupNetworking()
    {	isClientConnected = false;
        try {
            protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (protClient == null) {
            System.out.println("missing protocol host");
        }
        else {	// Send the initial join message with a unique identifier for this client
            System.out.println("sending join message to protocol host");
            protClient.sendJoinMessage();
        }
    }
    protected void processNetworking(float elapsTime)
    {	// Process packets received by the client from the server
        if (protClient != null)
            protClient.processPackets();
    }
    public Vector3f getPlayerPosition() { return player.getWorldLocation(); }
    public void setIsConnected(boolean value) { this.isClientConnected = value; }
    private class SendCloseConnectionPacketAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event evt)
        {
            if(protClient != null && isClientConnected == true) {
                protClient.sendByeMessage();
            }
        }
    }
    //-----------End of Networking Methods ---------------------

    public void toggleAxes()
    {
        if(axesOn)
        {
            axesOn = false;
            x.getRenderStates().disableRendering();
            y.getRenderStates().disableRendering();
            z.getRenderStates().disableRendering();
        } else {
            axesOn = true;
            x.getRenderStates().enableRendering();
            y.getRenderStates().enableRendering();
            z.getRenderStates().enableRendering();
        }
    }

    public void hudManagement(int sec)
    {

        String elapsTimeStr = Integer.toString(sec);
        String scoreStr = Integer.toString(score);
        String dispStr1 = "Time = " + elapsTimeStr;
        String dispStr2 = "Score = " + scoreStr;
        String dolLoc = "Dolphin position = X: "+ (int) player.getWorldLocation().x
                                           + ", Y: " + (int)player.getWorldLocation().y
                                           + ", Z: " + (int) player.getWorldLocation().z;

        Vector3f hud1Color = new Vector3f(0,1,0);
        Vector3f hud2Color = new Vector3f(0,0,1);
        Vector3f hudHealthColor = new Vector3f(1, 0, 0);
        Vector3f hud4Color = new Vector3f(1,0,1);

        int w = (int) engine.getRenderSystem().getViewport("MAIN").getActualWidth();
        int mapWidth = (int) engine.getRenderSystem().getViewport("MAP").getActualWidth();
        int miniMap = w - mapWidth;

        (engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 0, 15);
        (engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 175, 15);
       // (engine.getHUDmanager()).setHUD3(disHealth, hudHealthColor, 350, 15);

        (engine.getHUDmanager()).setHUD4(dolLoc, hudHealthColor, miniMap+10, 15);
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
        //Requirement 2.1
        case KeyEvent.VK_UP:
            engine.getRenderSystem().getViewport("MAP").getCamera().cameraPanVert(1.5f);
            break;
        case KeyEvent.VK_DOWN:
            engine.getRenderSystem().getViewport("MAP").getCamera().cameraPanVert(-1.5f);
            break;
        case KeyEvent.VK_LEFT:
            engine.getRenderSystem().getViewport("MAP").getCamera().cameraPanSide(-1.5f);
            break;
        case KeyEvent.VK_RIGHT:
            engine.getRenderSystem().getViewport("MAP").getCamera().cameraPanSide(1.5f);
            break;
        //Requirement 2.2
        case KeyEvent.VK_1:
            engine.getRenderSystem().getViewport("MAP").getCamera().moveFrontBack(1f);
            break;
        case KeyEvent.VK_2:
            engine.getRenderSystem().getViewport("MAP").getCamera().moveFrontBack(-1f);
            break;


        //Move Dolphin forward
        case KeyEvent.VK_W:
                dolFwd = player.getWorldForwardVector();
                dolLoc = player.getWorldLocation();
                player.setLocalLocation(dolLoc.add(dolFwd.mul(0.4f)));
            break;

        case KeyEvent.VK_A:
                  //Key turns dolphin
                player.objectYaw(player, 0.03f)
                ;break;

        case KeyEvent.VK_S: //Move dolphin backwards, same as forward but negative
                dolFwd = player.getWorldForwardVector();
                dolLoc = player.getWorldLocation();
                player.setLocalLocation(dolLoc.add(dolFwd.mul(-0.4f)));
             break;

        case KeyEvent.VK_D:
                player.objectYaw(player, -0.06f);
                break;

        case KeyEvent.VK_SPACE:
                toggleAxes();
                //--------------Script Light stuff
                Invocable invocableEngine = (Invocable) jsEngine;
                //get the light to be updated
                Light lgt = engine.getLightManager().getLight(0);

                // invoke the script function
                try { invocableEngine.invokeFunction("updateAmbientColor", lgt); }
                catch (ScriptException e1) {System.out.println("ScriptException in " + scriptFile3 + e1); }
                catch (NoSuchMethodException e2) {System.out.println("No such method exception in " + scriptFile3 + e2); }
                catch (NullPointerException e3) {System.out.println ("Null ptr exception reading " + scriptFile3 + e3); }
            break;

        //Test for gravity
        case KeyEvent.VK_ENTER:
        {
            System.out.println("starting physics");
            running = true;
        } break;

        default:
            System.out.println("That key doesn't do anything");
            throw new IllegalStateException("Unexpected value: " + e.getKeyCode());
        }
        super.keyPressed(e);
    }

    public void collectPrize()
    {
        Vector3f dolLoc = player.getWorldLocation();
        float d = Math.abs(dolLoc.distance(prize.getWorldLocation()));
        float d2 = Math.abs(dolLoc.distance(prize2.getWorldLocation()));
        float d3 = Math.abs(dolLoc.distance(prize3.getWorldLocation()));
        float randX = -10.0f + rand.nextFloat() * (10.0f-(-10.0f));
        float randZ = -10.0f + rand.nextFloat() * (10.0f-(-10.0f));
        if(d <= 1.0f)
        {
            score +=1;
            prize.setLocalLocation(new Vector3f(randX, 0.5f, randZ));
        }
        else if(d2 <= 1.0f)
        {
            score +=1;
            prize2.setLocalLocation(new Vector3f(randX, 0.5f, randZ));
        }
        else if (d3 <= 1.0f)
        {
            score +=1;
            prize3.setLocalLocation(new Vector3f(randX, 0.5f, randZ));
        }
    }
    //Script method for 7a
    private void executeScript(ScriptEngine engine, String scriptFileName)
    {
        try
        {
            FileReader fileReader = new FileReader(scriptFileName);
            engine.eval(fileReader);    //execute the script statements in the file
            fileReader.close();
        }
        catch (FileNotFoundException e1) { System.out.println(scriptFileName + " not found " + e1); }
        catch (IOException e2)          { System.out.println("IO problem with " + scriptFileName + e2); }
        catch (ScriptException e3)      { System.out.println("ScriptException in " + scriptFileName + e3); }
        catch (NullPointerException e4) { System.out.println ("Null ptr exception in " + scriptFileName + e4); }
    }
    //Script method for 7b
    private void runScript(File scriptFile)
    {
        try
        {
            FileReader fileReader = new FileReader(scriptFile);
            jsEngine.eval(fileReader);
            fileReader.close();
        }
        catch (FileNotFoundException e1) { System.out.println(scriptFile + " not found " + e1); }
        catch (IOException e2)          { System.out.println("IO problem with " + scriptFile + e2); }
        catch (ScriptException e3)      { System.out.println("ScriptException in " + scriptFile + e3); }
        catch (NullPointerException e4) { System.out.println ("Null ptr exception reading " + scriptFile + e4); }
    }
    //--------------------Physics Section-----------------------------------------
    private void checkForCollisions()
    {
        com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
        com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
        com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
        com.bulletphysics.dynamics.RigidBody object1, object2;
        com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

        dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
        dispatcher = dynamicsWorld.getDispatcher();
        int manifoldCount = dispatcher.getNumManifolds();
        for (int i=0; i<manifoldCount; i++)
        {
            manifold = dispatcher.getManifoldByIndexInternal(i);
            object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
            object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
            JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
            JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
            for (int j = 0; j < manifold.getNumContacts(); j++)
            {
                contactPoint = manifold.getContactPoint(j);
                if (contactPoint.getDistance() < 0.0f)
                {
                    System.out.println("---- hit between " + obj1 + " and " + obj2);
                    break;
                }
            }
        }
    }

}