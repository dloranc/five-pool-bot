package dloranc.fivepoolbot;

import bwapi.*;
import java.util.HashSet;
import java.util.List;

public class EnemyBuildings {
    private HashSet<Position> enemyBuildingMemory;

    public EnemyBuildings() {
        enemyBuildingMemory = new HashSet<>();
    }

    public void update(Game game) {
        List<Unit> enemyUnits = game.enemy().getUnits();

        for (Unit unit : enemyUnits) {
            if (unit.getType().isBuilding()) {
                if (!enemyBuildingMemory.contains(unit.getPosition())) {
                    enemyBuildingMemory.add(unit.getPosition());
                }
            }
        }

        for (Position position : enemyBuildingMemory) {
            //if that tile is currently visible to us...
            if (game.isVisible(position.getX() / 32, position.getY() / 32)) {

                //loop over all the visible enemy buildings and find out if at least
                //one of them is still at that remembered position
                boolean buildingStillThere = false;
                for (Unit unit : enemyUnits) {
                    if (unit.getType().isBuilding() && unit.getPosition().equals(position)) {
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
