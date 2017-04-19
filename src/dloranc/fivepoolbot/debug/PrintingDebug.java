package dloranc.fivepoolbot.debug;

import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwta.BaseLocation;

import java.util.List;

public class PrintingDebug {
    private Game game;

    public PrintingDebug(Game game) {
        this.game = game;
    }

    public void print(Player self, List<BaseLocation> possibleEnemyBaseLocations, int basesCount) {

        game.drawTextScreen(10, 10, "Playing as " + self.getRace() + ", FPS: " + game.getFPS() + ", frames: " + game.getFrameCount());

        int enemyCount = game.enemy().getUnits().size();

        game.drawTextScreen(10, 25,
                game.mapFileName() + ", " + basesCount + " bases, enemy units: " + enemyCount
                        + ", bases to scout: " + possibleEnemyBaseLocations.size()
        );
    }

    public void printScout(Unit scoutDrone, BaseLocation baseToScout) {
        if (scoutDrone != null && baseToScout != null) {
            game.drawTextScreen(10, 45, "Distance to base to scout: " + scoutDrone.getDistance(baseToScout.getPosition()));
        }
    }
}
