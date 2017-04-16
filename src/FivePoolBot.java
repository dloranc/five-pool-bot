import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class FivePoolBot extends DefaultBWListener {
    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    private boolean isSpawningPool;
    private boolean isScoutingIdle;
    private Unit scoutDrone;
    private Unit hatchery;
    private BaseLocation playerStartLocation;
    private List<BaseLocation> possibleEnemyBaseLocations;
    private BaseLocation baseToScout;
    private BaseLocation enemyBase;
    private boolean isEnemyBaseDestroyed;
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

        isSpawningPool = false;
        isScoutingIdle = false;
        scoutDrone = null;
        hatchery = null;
        playerStartLocation = null;
        possibleEnemyBaseLocations = null;
        baseToScout = null;
        enemyBase = null;
        isEnemyBaseDestroyed = false;
        enemyBuildings = new EnemyBuildings();

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        BWTA.readMap();
        BWTA.analyze();

        game.setLocalSpeed(0);

        playerStartLocation = BWTA.getStartLocation(self);
        possibleEnemyBaseLocations = BWTA.getStartLocations();

        basesCount = possibleEnemyBaseLocations.size();
        removePlayerBaseFromPossibleEnemyBaseList();

        for (Unit myUnit : self.getUnits()) {
            if (hatchery == null) {
                if (myUnit.getType() == UnitType.Zerg_Hatchery) {
                    hatchery = myUnit;
                }
            }
        }
    }

    @Override
    public void onFrame() {
        float supplyUsed = self.supplyUsed() / 2;
        float supplyTotal = self.supplyTotal() / 2;
        int dronesCount = getDronesCount();

        enemyBuildings.update(game);

        printDebug();
        drawDebug();

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

            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                gatherMinerals(myUnit);
            }

            if (myUnit.getType() == UnitType.Zerg_Zergling && myUnit.isIdle()) {
                attack(myUnit);
            }
        }

        scouting();

        if (dronesCount >= 5 && !isSpawningPool && self.minerals() >= 200) {
            buildSpawningPool();
        }
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if (scoutDrone == null && getDronesCount() >= 5) {
            if (unit.getType() == UnitType.Zerg_Drone) {
                scoutDrone = unit;
            }
        }
    }

    @Override
    public void onEnd(boolean b) {
        if (self.isVictorious()) {
            System.out.println("Victory!");
        } else {
            System.out.println("Lose...");
        }
    }

    private void printDebug() {
        game.drawTextScreen(10, 10, "Playing as " + self.getRace() + ", FPS: " + game.getFPS() + ", frames: " + game.getFrameCount());

        int enemyCount = game.enemy().getUnits().size();

        game.drawTextScreen(10, 25,
                game.mapFileName() + ", " + basesCount + " bases, enemy units: " + enemyCount
                        + ", bases to scout: " + possibleEnemyBaseLocations.size()
        );

        if (scoutDrone != null) {
            game.drawTextScreen(10, 45, "Scout Drone: " + scoutDrone.getPosition().toString());
        }
    }

    private void drawDebug() {
        if (scoutDrone != null) {
            drawTargetLine(scoutDrone);
        }

        for (Unit unit : self.getUnits()) {
            if (unit.getType() == UnitType.Zerg_Zergling) {
                drawTargetLine(unit);
            }
        }

        if (enemyBase != null) {
            game.drawCircleMap(enemyBase.getPosition(), 70, Color.Purple);
        }
    }

    private void drawTargetLine(Unit unit) {
        Position position = unit.getPosition();
        Position destination = unit.getOrderTargetPosition();

        Order order = unit.getOrder();

        Color color = assignOrderColor(order);

        if (destination.isValid()) {
            if (!destination.equals(new Position(0, 0))) {
                game.drawLineMap(position, destination, color);
            }
        }
    }

    private Color assignOrderColor(Order order) {
        Color color = Color.White;

        if (order == Order.AttackMove || order == Order.AttackUnit) {
            color = Color.Red;
        }

        if (order == Order.MoveToMinerals || order == Order.ReturnMinerals) {
            color = Color.Blue;
        }

        return color;
    }

    private int getDronesCount() {
        int dronesCount = 0;

        for (Unit myUnit : self.getUnits()) {
            if (myUnit.getType() == UnitType.Zerg_Drone) {
                dronesCount++;
            }
        }
        return dronesCount;
    }

    private void scouting() {
        if (scoutDrone == null) {
            return;
        }

        if (scoutDrone.isUnderAttack()) {
            enemyBase = baseToScout;
            backToBaseToGatherMinerals();
        }
        // add isScoutingIdle because scouting drone can be idle for more
        // than one frame and this behavior causes that drone can't scout
        // last base when map has four starting locations
        if (scoutDrone.isIdle() && !isScoutingIdle) {
            isScoutingIdle = true;

            if (enemyBuildings.getBuildings().isEmpty()) {
                possibleEnemyBaseLocations.remove(baseToScout);

                baseToScout = selectBase();
                scoutDrone.attack(baseToScout.getPosition());

                if (possibleEnemyBaseLocations.size() == 1) {
                    enemyBase = baseToScout;
                }
            }
        } else {
            isScoutingIdle = false;
        }

    }

    private void buildSpawningPool() {
        Unit buildDrone = null;

        for (Unit myUnit : self.getUnits()) {
            if (myUnit.getType() == UnitType.Zerg_Drone) {
                if (!myUnit.isCarryingMinerals() && myUnit.getID() != scoutDrone.getID()) {
                    buildDrone = myUnit;
                    break;
                }
            }
        }

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

    private void attack(Unit myUnit) {
        HashSet<Position> enemyBuildingPositions = enemyBuildings.getBuildings();

        if (!enemyBuildingPositions.isEmpty()) {
            Position enemyBuildingPosition = enemyBuildingPositions.iterator().next();
            myUnit.attack(enemyBuildingPosition);
        } else {
            if (enemyBase != null) {
                if (!isEnemyBaseDestroyed) {
                    myUnit.attack(enemyBase.getPosition());
                } else {
                    scoutAndAttack(myUnit);
                }
            } else {
                scoutAndAttack(myUnit);
            }
        }
    }

    @Override
    public void onUnitDestroy(Unit unit) {
        UnitType unitType = unit.getType();

        if (!Objects.equals(unit.getPlayer().getName(), self.getName())) {
            if (isBase(unitType)) {
                isEnemyBaseDestroyed = true;
            }
        }
    }

    private boolean isBase(UnitType unitType) {
        return unitType == UnitType.Protoss_Nexus ||
                unitType == UnitType.Zerg_Hatchery ||
                unitType == UnitType.Zerg_Lair ||
                unitType == UnitType.Zerg_Hive ||
                unitType == UnitType.Terran_Command_Center;
    }

    private void scoutAndAttack(Unit myUnit) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Position randomPosition = new Position(
                random.nextInt(game.mapWidth() * 32),
                random.nextInt(game.mapHeight() * 32)
        );

        if (myUnit.canAttack(randomPosition)) {
            myUnit.attack(randomPosition);
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
        BaseLocation nearestBaseLocation = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (BaseLocation baseLocation : possibleEnemyBaseLocations) {
            int distance = scoutDrone.getDistance(baseLocation.getPosition());

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestBaseLocation = baseLocation;
            }
        }

        return nearestBaseLocation;
    }

    private void removePlayerBaseFromPossibleEnemyBaseList() {
        List<BaseLocation> toRemove = new ArrayList<>();

        for (BaseLocation location : possibleEnemyBaseLocations) {
            if (location.getPosition().equals(playerStartLocation.getPosition())) {
                toRemove.add(location);
            }
        }

        possibleEnemyBaseLocations.removeAll(toRemove);
    }

    private void gatherMinerals(Unit myUnit) {
        if (scoutDrone != null) {
            if (myUnit.getID() == scoutDrone.getID()) {
                return;
            }
        }

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

    public static void main(String[] args) {
        new FivePoolBot().run();
    }
}