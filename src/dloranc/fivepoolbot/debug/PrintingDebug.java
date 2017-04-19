package dloranc.fivepoolbot.debug;

import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwta.BaseLocation;

import java.util.List;

public class PrintingDebug {
    private Game game;
    private Player self;
    private List<BaseLocation> possibleEnemyBaseLocations;
    private Unit scoutDrone;
    private int basesCount;

    public PrintingDebug(Game game, Player self, List<BaseLocation> possibleEnemyBaseLocations, Unit scoutDrone, int basesCount) {
        this.game = game;
        this.self = self;
        this.possibleEnemyBaseLocations = possibleEnemyBaseLocations;
        this.scoutDrone = scoutDrone;
        this.basesCount = basesCount;
    }

    public void print() {

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
}
