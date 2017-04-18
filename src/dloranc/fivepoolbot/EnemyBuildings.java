package dloranc.fivepoolbot;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;

import java.util.HashSet;

public class EnemyBuildings {
    private HashSet<Position> enemyBuildingMemory;

    public EnemyBuildings() {
        enemyBuildingMemory = new HashSet<>();
    }

    public void update(Game game) {
        for (Unit unit : game.enemy().getUnits()) {
            if (unit.getType().isBuilding()) {
                if (!enemyBuildingMemory.contains(unit.getPosition())) {
                    enemyBuildingMemory.add(unit.getPosition());
                }
            }
        }

        for (Position position : enemyBuildingMemory) {
            // compute the TilePosition corresponding to our remembered Position p
            TilePosition tileCorrespondingToP = new TilePosition(position.getX() / 32, position.getY() / 32);

            //if that tile is currently visible to us...
            if (game.isVisible(tileCorrespondingToP)) {

                //loop over all the visible enemy buildings and find out if at least
                //one of them is still at that remembered position
                boolean buildingStillThere = false;
                for (Unit unit : game.enemy().getUnits()) {
                    if ((unit.getType().isBuilding()) && (unit.getPosition() == position)) {
                        buildingStillThere = true;
                        break;
                    }
                }

                //if there is no more any building, remove that position from our memory
                if (!buildingStillThere) {
                    enemyBuildingMemory.remove(position);
                    break;
                }
            }
        }
    }

    public HashSet<Position> getBuildings() {
        return enemyBuildingMemory;
    }
}
