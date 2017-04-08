import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FivePoolBot extends DefaultBWListener {
    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    private boolean isSpawningPool;
    private boolean isScouting;
    private Unit scoutDrone;
    private Unit buildDrone;
    private Unit hatchery;
    private BaseLocation playerStartLocation;
    private List<BaseLocation> possibleEnemyBaseLocations;
    private BaseLocation baseToScout;
    private BaseLocation enemyBase;
    private int basesCount;
    private EnemyBuildings enemyBuildings;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        isScouting = false;
        isSpawningPool = false;
        scoutDrone = null;
        buildDrone = null;
        hatchery = null;
        playerStartLocation = null;
        possibleEnemyBaseLocations = null;
        baseToScout = null;
        enemyBase = null;
        enemyBuildings = new EnemyBuildings();

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        System.out.println(game.mapWidth());
        System.out.println(game.mapHeight());

        game.setLocalSpeed(0);

        playerStartLocation = BWTA.getStartLocation(self);
        possibleEnemyBaseLocations = BWTA.getStartLocations();

        basesCount = possibleEnemyBaseLocations.size();
        removePlayerBaseFromBaseList();

        for (Unit myUnit : self.getUnits()) {
            if (scoutDrone == null) {
                if (myUnit.getType() == UnitType.Zerg_Drone) {
                    scoutDrone = myUnit;
                }
            }

            if (buildDrone == null) {
                if (myUnit.getType() == UnitType.Zerg_Drone && !myUnit.equals(scoutDrone)) {
                    buildDrone = myUnit;
                }
            }

            if (hatchery == null) {
                if (myUnit.getType() == UnitType.Zerg_Hatchery) {
                    hatchery = myUnit;
                }
            }
        }
    }

    @Override
    public void onFrame() {
        float supplyTotal = self.supplyTotal() / 2;
        float supplyUsed = self.supplyUsed() / 2;

        enemyBuildings.update(game);

        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getRace());

        int enemyCount = game.enemy().getUnits().size();

        game.drawTextScreen(10, 25,
                game.mapFileName() + ", " + basesCount + " bases, enemy units: " + enemyCount
                        + ", bases to scout: " + possibleEnemyBaseLocations.size()
        );

        if (scoutDrone != null) {
            game.drawTextScreen(10, 45, "Scout Drone: " + scoutDrone.getPosition().toString());
        }

        if (buildDrone != null) {
            game.drawTextScreen(10, 65, "Build Drone: " + buildDrone.getPosition().toString());
        }

        int dronesCount = 0;

        for (Unit myUnit : self.getUnits()) {
            if (myUnit.getType() == UnitType.Zerg_Drone) {
                dronesCount++;
            }
        }

        for (Unit myUnit : self.getUnits()) {
            if (myUnit.getType() == UnitType.Zerg_Hatchery) {
                if (supplyTotal - supplyUsed <= 1) {
                    if (self.minerals() >= 100) {
                        myUnit.train(UnitType.Zerg_Overlord);
                    }
                } else {
                    if (supplyUsed < 5) {
                        if (self.minerals() >= 50) {
                            myUnit.train(UnitType.Zerg_Drone);
                        }
                    } else {
                        if (self.minerals() >= 50) {
                            myUnit.train(UnitType.Zerg_Zergling);
                        }
                    }
                }
            }

            if (dronesCount >= 5 && !isScouting) {
                baseToScout = selectBase();
                game.printf(baseToScout.getPosition().toString());
                scoutDrone.attack(baseToScout.getPosition());
                isScouting = true;
            }

            gatherMinerals(myUnit);

            if (myUnit.getType() == UnitType.Zerg_Zergling && myUnit.isIdle()) {
                HashSet<Position> enemyBuildingPositions = enemyBuildings.getBuildings();

                if (!enemyBuildingPositions.isEmpty()) {
                    for (Position enemyBuildingPosition : enemyBuildingPositions) {
                        myUnit.attack(enemyBuildingPosition);
                        System.out.println(enemyBuildingPosition);
                        break;
                    }
                } else {
                    if (enemyBase != null) {
                        myUnit.attack(enemyBase.getPosition());
                    } else {
                        ThreadLocalRandom random = ThreadLocalRandom.current();

                        Position randomPosition = new Position(
                                random.nextInt(game.mapWidth() * 32),
                                random.nextInt(game.mapHeight() * 32)
                        );

                        if (myUnit.canAttack(randomPosition)) {
                            myUnit.attack(randomPosition);
                        }
                    }
                }
            }
        }

        if (isScouting) {
            List<Unit> enemyUnits = game.enemy().getUnits();

            int buildingsCount = 0;

            for (Unit enemyUnit : enemyUnits) {
                if (enemyUnit.getType().isBuilding()) {
                    buildingsCount++;
                }
            }

            if (scoutDrone.isUnderAttack()) {
                enemyBase = baseToScout;
                backToBaseToGatherMinerals();
            }

            if (scoutDrone.isIdle() && buildingsCount == 0) {
                possibleEnemyBaseLocations.remove(baseToScout);

                baseToScout = selectBase();
                scoutDrone.attack(baseToScout.getPosition());
            }
        }

        if (dronesCount >= 5 && !isSpawningPool && self.minerals() >= 200) {
            TilePosition buildPosition = BuildingUtilities.getBuildTile(
                    game,
                    buildDrone,
                    UnitType.Zerg_Spawning_Pool,
                    playerStartLocation.getTilePosition()
            );

            if (buildPosition != null) {
                buildDrone.build(UnitType.Zerg_Spawning_Pool, buildPosition);
                isSpawningPool = true;
            }
        }
    }

    private void backToBaseToGatherMinerals() {
        Unit closestMineral = null;

        //find the closest mineral near main hatchery
        for (Unit neutralUnit : game.neutral().getUnits()) {
            if (neutralUnit.getType().isMineralField()) {
                if (closestMineral == null || hatchery.getDistance(neutralUnit) < hatchery.getDistance(closestMineral)) {
                    closestMineral = neutralUnit;
                }
            }
        }

        scoutDrone.gather(closestMineral);
    }

    private BaseLocation selectBase() {
        int baseIndex = ThreadLocalRandom.current().nextInt(possibleEnemyBaseLocations.size());

        return possibleEnemyBaseLocations.get(baseIndex);
    }

    private void removePlayerBaseFromBaseList() {
        List<BaseLocation> toRemove = new ArrayList<>();

        for (BaseLocation location : possibleEnemyBaseLocations) {
            if (location.getPosition().equals(playerStartLocation.getPosition())) {
                toRemove.add(location);
            }
        }

        possibleEnemyBaseLocations.removeAll(toRemove);
    }

    private void gatherMinerals(Unit myUnit) {
        if ((myUnit.getType().isWorker() && myUnit.isIdle())) {
            if (!(myUnit.equals(scoutDrone) && isScouting)) {
                Unit closestMineral = null;

                //find the closest mineral
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }

                //if a mineral patch was found, send the worker to gather it
                if (closestMineral != null) {
                    myUnit.gather(closestMineral, false);
                }
            }
        }
    }

    public static void main(String[] args) {
        new FivePoolBot().run();
    }
}