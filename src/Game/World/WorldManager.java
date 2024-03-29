package Game.World;

import Game.Entities.Dynamic.Player;
import Game.Entities.Static.LillyPad;
import Game.Entities.Static.Log;
import Game.Entities.Static.StaticBase;
import Game.Entities.Static.Tree;
import Game.Entities.Static.Turtle;
import Main.Handler;
import UI.UIManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

/**
 * Literally the world. This class is very important to understand.
 * Here we spawn our hazards (StaticBase), and our tiles (BaseArea)
 * 
 * We move the screen, the player, and some hazards. 
 * 				How? Figure it out.
 */
public class WorldManager {

	private ArrayList<BaseArea> AreasAvailables;			// Lake, empty and grass area (NOTE: The empty tile is just the "sand" tile. Ik, weird name.)
	private ArrayList<StaticBase> StaticEntitiesAvailables;	// Has the hazards: LillyPad, Log, Tree, and Turtle.

	private ArrayList<BaseArea> SpawnedAreas;				// Areas currently on world
	private ArrayList<StaticBase> SpawnedHazards;			// Hazards currently on world.

	Long time;
	Boolean reset = true;

	Handler handler;


	public Player player;									// How do we find the frog coordinates? How do we find the Collisions? This bad boy.

	UIManager object = new UIManager(handler);
	UI.UIManager.Vector object2 = object.new Vector();


	private ID[][] grid;									
	private int gridWidth,gridHeight;						// Size of the grid. 
	private int movementSpeed;								// Movement of the tiles going downwards.
	private boolean previousSpawnedY = false;               // Prevents two Lily Pads from spawning two Y levels consecutively.

	public WorldManager(Handler handler) {
		this.handler = handler;

		AreasAvailables = new ArrayList<>();				// Here we add the Tiles to be utilized.
		StaticEntitiesAvailables = new ArrayList<>();		// Here we add the Hazards to be utilized.

		AreasAvailables.add(new GrassArea(handler, 0));		
		AreasAvailables.add(new WaterArea(handler, 0));
		AreasAvailables.add(new EmptyArea(handler, 0));

		StaticEntitiesAvailables.add(new LillyPad(handler, 0, 0));
		StaticEntitiesAvailables.add(new Log(handler, 0, 0));
		StaticEntitiesAvailables.add(new Tree(handler, 0, 0));
		StaticEntitiesAvailables.add(new Turtle(handler, 0, 0));

		SpawnedAreas = new ArrayList<>();
		SpawnedHazards = new ArrayList<>();

		player = new Player(handler);       

		gridWidth = handler.getWidth()/64;
		gridHeight = handler.getHeight()/64;
		movementSpeed = 1;
		// movementSpeed = 20; I dare you.

		/* 
		 * 	Spawn Areas in Map (2 extra areas spawned off screen)
		 *  To understand this, go down to randomArea(int yPosition) 
		 */
		for(int i=0; i<gridHeight+2; i++) {
			SpawnedAreas.add(randomArea2((-2+i)*64));
		}

		player.setX((gridWidth/2)*64);
		player.setY((gridHeight-3)*64);

		// Not used atm.
		grid = new ID[gridWidth][gridHeight];
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				grid[x][y]=ID.EMPTY;
			}
		}
	}

	public void tick() {

		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[2])) {
			this.object2.word = this.object2.word + this.handler.getKeyManager().str[1];
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[0])) {
			this.object2.word = this.object2.word + this.handler.getKeyManager().str[2];
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[1])) {
			this.object2.word = this.object2.word + this.handler.getKeyManager().str[0];
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[3])) {
			this.object2.addVectors();
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[4]) && this.object2.isUIInstance) {
			this.object2.scalarProduct(handler);
		}

		if(this.reset) {
			time = System.currentTimeMillis();
			this.reset = false;
		}

		if(this.object2.isSorted) {

			if(System.currentTimeMillis() - this.time >= 2000) {		
				this.object2.setOnScreen(true);	
				this.reset = true;
			}

		}

		for (BaseArea area : SpawnedAreas) {
			area.tick();
		}
		for (StaticBase hazard : SpawnedHazards) {
			hazard.tick();
		}



		for (int i = 0; i < SpawnedAreas.size(); i++) {
			SpawnedAreas.get(i).setYPosition(SpawnedAreas.get(i).getYPosition() + movementSpeed);

			// Check if Area (thus a hazard as well) passed the screen.
			if (SpawnedAreas.get(i).getYPosition() > handler.getHeight()) {
				// Replace with a new random area and position it on top
				SpawnedAreas.set(i, randomArea(-2 * 64));
			}
			// Make sure players position is synchronized with area's movement
			if (SpawnedAreas.get(i).getYPosition() < player.getY()
					&& player.getY() - SpawnedAreas.get(i).getYPosition() < 3) {
				player.setY(SpawnedAreas.get(i).getYPosition());
			}
		}

		HazardMovement();

		player.tick();
		// Make player move the same as the areas.
		player.setY(player.getY()+movementSpeed); 

		object2.tick();

	}

	private void HazardMovement() {

		for (int i = 0; i < SpawnedHazards.size(); i++) {

			// Moves hazard down
			SpawnedHazards.get(i).setY(SpawnedHazards.get(i).getY() + movementSpeed);

			// Moves Log to the right
			if (SpawnedHazards.get(i) instanceof Log) {
				SpawnedHazards.get(i).setX(SpawnedHazards.get(i).getX() + 1);

				// Verifies the hazards Rectangles aren't null and
				// If the player Rectangle intersects with the Log Rectangle,then
				// Moves player to the right.
				if (SpawnedHazards.get(i).GetCollision() != null
						&& player.getPlayerCollision().intersects(SpawnedHazards.get(i).GetCollision())) {
					player.setX(player.getX() + 1);
					hazardBoundaries();
				}

				// Make logs loop the screen
				if(SpawnedHazards.get(i).getX() > 576) {

					SpawnedHazards.get(i).setX(-128);
				}
			}
			// Moves Turtles to the left and make them loop the screen
			if (SpawnedHazards.get(i) instanceof Turtle) {
				SpawnedHazards.get(i).setX(SpawnedHazards.get(i).getX() - 1);

				if (SpawnedHazards.get(i).GetCollision() != null
						&& player.getPlayerCollision().intersects(SpawnedHazards.get(i).GetCollision()) ) {
					player.setX(player.getX() - 1);
					hazardBoundaries();
				}
				// Make Turtles loop the screen
				if (SpawnedHazards.get(i).getX() + 80 < 0) {
					SpawnedHazards.get(i).setX(this.handler.getWidth());
				}
			}

			// Prevents the frog to go through the trees.
			if (SpawnedHazards.get(i) instanceof Tree && SpawnedHazards.get(i).GetCollision() != null
					&& player.getPlayerCollision().intersects(SpawnedHazards.get(i).GetCollision())) {
				if(player.getFacing().equals("UP")) {
					player.setY(player.getY() + 10);
				}
				else if (player.getFacing().equals("DOWN")) {
					player.setY(player.getY() - 10);
				}
				else if (player.getFacing().equals("RIGHT")) {
					player.setX(player.getX() - 10);
				}
				else if (player.getFacing().equals("LEFT")) {
					player.setX(player.getX() + 10);	
				}
			}
			// If hazard has passed the screen height, then remove this hazard.
			if (SpawnedHazards.get(i).getY() > handler.getHeight()) {
				SpawnedHazards.remove(i);
			}
			// Kill player when the lower screen gets him.
			if (player.getY() - player.getHeight() > handler.getHeight()) {
				player.kill();
			}
            // Kill player when he touches the water
			boolean deadFroggy = false;
			
			for (int j = 0; j < SpawnedAreas.size();j++) {
				if(SpawnedAreas.get(j) instanceof WaterArea && SpawnedAreas.get(j).getYPosition() <= player.getPlayerCollision().getY()
						&& (SpawnedAreas.get(j).getYPosition() + 66) >= (player.getPlayerCollision().getY() + player.getPlayerCollision().getHeight())) {
					deadFroggy = true;
					
					if(notInDanger(player.getPlayerCollision())) {
						deadFroggy = false;
					}
				}
			}
			
			if(deadFroggy) {
				player.kill();
			}
		}
	}
	// Added boundaries when the player is on top of Turtle or Log 
	public void hazardBoundaries() {
		if (player.getX() < 0 && player.getFacing().equals("RIGHT")
				|| player.getX() < 0 && player.getFacing().equals("UP")
				|| player.getX() < 0 && player.getFacing().equals("DOWN")
				|| player.getX() < 0 && player.getFacing().equals("LEFT")) {
			player.setX(player.getX() + 5);
		    }
		if (player.getX() > 576 && player.getFacing().equals("RIGHT")
				|| player.getX() + 64 > 576 && player.getFacing().equals("UP")
				|| player.getX() + 64 > 576 && player.getFacing().equals("DOWN")
				|| player.getX() + 64 > 576 && player.getFacing().equals("LEFT")) {
			player.setX(player.getX() - 5);
		}
		    
	}

	public void render(Graphics g){

		for(BaseArea area : SpawnedAreas) {
			area.render(g);
		}

		for (StaticBase hazards : SpawnedHazards) {
			hazards.render(g);

		}

		player.render(g);       
		this.object2.render(g);      

	}

	/*
	 * Given a yPosition, this method will return a random Area out of the Available ones.)
	 * It is also in charge of spawning hazards at a specific condition.
	 */
	private BaseArea randomArea(int yPosition) {
		Random rand = new Random();

		// From the AreasAvailable, get me any random one.
		BaseArea randomArea = AreasAvailables.get(rand.nextInt(AreasAvailables.size())); 
		// Added trees only on grass area.
		if(randomArea instanceof GrassArea) {
			randomArea = new GrassArea(handler, yPosition);
			SpawnHazard2(yPosition);
		}
		else if(randomArea instanceof WaterArea) {
			randomArea = new WaterArea(handler, yPosition);
			SpawnHazard(yPosition);
		}
		else {
			randomArea = new EmptyArea(handler, yPosition);
		}
		return randomArea;
	}
	
	// Method for Water No-Spawn
	private BaseArea randomArea2(int yPosition) {
		Random rand = new Random();

		// From the AreasAvailable, get me any random one.
		BaseArea randomArea2 = AreasAvailables.get(rand.nextInt(AreasAvailables.size())); 

		if(randomArea2 instanceof GrassArea) {
			randomArea2 = new GrassArea(handler, yPosition);
			SpawnHazard2(yPosition);
		}
		else {
			randomArea2 = new EmptyArea(handler, yPosition);
		}
		return randomArea2;
	}
	
	/*
	 * Given a yPositionm this method will add a new hazard to the SpawnedHazards ArrayList
	 */
	private void SpawnHazard(int yPosition) {
		Random rand = new Random();
		int randInt;
		int choice = rand.nextInt(9);
		int counterLillyPad = rand.nextInt(9);
		int counterLog = rand.nextInt(4);
		// Chooses between Log or Lillypad
		if (choice <=2) {
			randInt = 128* rand.nextInt(4);
			previousSpawnedY = false;
			while(counterLog >= 0) {
				SpawnedHazards.add(new Log(handler, randInt, yPosition));
				randInt -= 160 + rand.nextInt(32);
				counterLog--;
			}
			previousSpawnedY = false;
		}

		// Add LillyPads randomly and add more than 1 in a row.
		else if (choice >=5 && previousSpawnedY != true) {
			randInt = 64 * rand.nextInt(9);
			while (counterLillyPad >= 0) {
				SpawnedHazards.add(new LillyPad(handler, randInt, yPosition));
				previousSpawnedY = true;
				randInt = 64 * rand.nextInt(7);
				counterLillyPad--;
			}
			previousSpawnedY = true;
		}
		else {
			randInt = this.handler.getWidth();
			SpawnedHazards.add(new Turtle(handler, randInt, yPosition));
			previousSpawnedY = false;
		}

	}
	// Method to add trees only in the grass area
	private void SpawnHazard2(int yPosition) {
		Random rand = new Random();
		int randInt;
		randInt = 64 * rand.nextInt(9);
		SpawnedHazards.add(new Tree(handler, randInt, yPosition));
	}
	// Checks if the frog is on top of a hazard in the water area
	public boolean notInDanger(Rectangle player) {
		for (int i = 0; i < SpawnedHazards.size(); i++) {
			if (SpawnedHazards.get(i).GetCollision() != null
					&& player.intersects(SpawnedHazards.get(i).GetCollision())) {
				return true;
			}
		}
		return false;
	}
		
}