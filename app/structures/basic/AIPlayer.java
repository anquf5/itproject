package structures.basic;
import commands.BasicCommands;
import structures.GameState;
import utils.ToolBox;

import java.util.*;

public class AIPlayer extends Player{

    // record the units that the AI player can option
    Set<Tile> optionalTiles = new HashSet<>();
    // Record the Tile that the AI players can move or summon
    Set<Tile> whiteTileGroup = new HashSet<>();
    // Record the Tile that the AI players can attack
    Set<Tile> redTileGroup = new HashSet<>();

    @Override
    public void cardSelected(int handPosition){
        Card cardSelected = this.cardsOnHand[handPosition];

        //if the player have selected a card, reset the card highlight firstly
        if ( GameState.getInstance().getCurrentState().equals(GameState.CurrentState.CARD_SELECT) ){
            clearSelected();
        }
        //set backend
        GameState.getInstance().setCardSelected(cardSelected);
        //highlight valid tiles
        showValidRange(cardSelected);
    }

    public AIPlayer(int health, int mana){
        super(health,mana);
    }

    public void addToOptionalTile(Tile tile){ optionalTiles.add(tile);}

    public void addToWhiteGroup(Tile tile){ whiteTileGroup.add(tile);}

    public void addToRedGroup(Tile tile){ redTileGroup.add(tile);}

    /**
     * start AI player
     */
    public void startUpAIMode(){
        Map<String, Object> parameters;

        // unit move and attack
        // 1. find all of unit
        parameters = new HashMap<>();
        parameters.put("type","searchUnit");
        parameters.put("range","all_friends");
        GameState.getInstance().broadcastEvent(Tile.class, parameters);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 2. store optional unit
        Iterator findUnit = optionalTiles.iterator();
        while(findUnit.hasNext()){
            Tile tileClicked = (Tile) findUnit.next();

            // 3. first click
            if(GameState.getInstance().getCurrentState().equals(GameState.CurrentState.READY)){
                parameters = new HashMap<>();
                parameters.put("type","firstClickTile");
                parameters.put("tilex",tileClicked.getTilex());
                parameters.put("tiley",tileClicked.getTiley());
                GameState.getInstance().broadcastEvent(Tile.class,parameters);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

                // get all of the tile that the AI can click
                parameters = new HashMap<>();
                parameters.put("type","AI_FindOperateTile");
                GameState.getInstance().broadcastEvent(Tile.class,parameters);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

                // 4. operate Unit
                if(GameState.getInstance().getCurrentState().equals(GameState.CurrentState.UNIT_SELECT)){
                    if(!redTileGroup.isEmpty()) {
                        // 4.1 if the unit can attack, attack firstly
                        Iterator searchAttack = this.redTileGroup.iterator();
                        while (searchAttack.hasNext()) {
                            // choose a tile
                            Tile y = (Tile) searchAttack.next();
                            parameters = new HashMap<>();
                            parameters.put("type", "operateUnit");
                            parameters.put("tilex", y.getTilex());
                            parameters.put("tiley", y.getTiley());
                            parameters.put("originTileSelected", tileClicked);
                            GameState.getInstance().broadcastEvent(Tile.class, parameters);
                            try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
                            clearTileRecord();
                            break;
                        }
                    }
                    else{
                        // 4.2 only move
                        Iterator searchMove = this.whiteTileGroup.iterator();
                        while(searchMove.hasNext()){
                            Tile y = (Tile) searchMove.next();
                            parameters = new HashMap<>();
                            parameters.put("type","operateUnit");
                            parameters.put("tilex",y.getTilex());
                            parameters.put("tiley",y.getTiley());
                            parameters.put("originTileSelected", tileClicked);
                            GameState.getInstance().broadcastEvent(Tile.class,parameters);
                            try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
                            clearTileRecord();
                            break;
                        }
                    }
                }
            }
        }
        clearTileRecord();
        this.optionalTiles.clear();
        try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

        // AI plays a card
        AIplay_a_card:
        for(int i = 0; i < 6; i++){
            if(this.cardsOnHand[i] != null){
                // 1. chose a card
                if(this.cardsOnHand[i].getManacost() <= this.mana){
                    this.cardSelected(i);
                }
                else {continue AIplay_a_card; }

                // 2. find the placeable tile
                parameters = new HashMap<>();
                parameters.put("type","AI_FindOperateTile");
                GameState.getInstance().broadcastEvent(Tile.class,parameters);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

                Iterator searchSummon = whiteTileGroup.iterator();
                while(searchSummon.hasNext()){
                    // select a tile
                    Tile y = (Tile) searchSummon.next();
                    if(GameState.getInstance().getCurrentState().equals(GameState.CurrentState.CARD_SELECT)){
                        parameters = new HashMap<>();

                        //get card selected
                        Card cardSelected = cardsOnHand[i];

                        //if it is a creature
                        if (cardSelected.isCreatureOrSpell() == 1){

                            ToolBox.logNotification(ToolBox.currentPlayerName() + " play a card: " + cardSelected.getCardname());

                            cardSelected.creatureCardUsed(y.tilex,y.tiley);}

                        //if it is a spell
                        else {
                            parameters.put("type", "spell");
                            parameters.put("tilex",y.getTilex());
                            parameters.put("tiley",y.getTiley());
                            GameState.getInstance().broadcastEvent(Tile.class,parameters);

                        }
                        this.clearTileRecord();
                        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
                        break;
                    }
                }
            }
        }
        this.clearTileRecord();

        GameState.getInstance().switchPlayer();
    }

    /**
     * refresh the tile state
     */
    private void clearTileRecord(){
        this.whiteTileGroup.clear();
        this.redTileGroup.clear();
    }

    /**
     * getter and setter
     */
    @Override
    public void setMana(int mana) {
        this.mana = mana;
        BasicCommands.setPlayer2Mana(GameState.getInstance().getOut(),this);
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
        BasicCommands.setPlayer2Health(GameState.getInstance().getOut(),this);
    }

}
