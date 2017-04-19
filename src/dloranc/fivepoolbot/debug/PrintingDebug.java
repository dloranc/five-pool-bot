package dloranc.fivepoolbot.debug;

import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwta.BaseLocation;

import java.util.List;

public class PrintingDebug {
    public void print(Game game, Player self, List<BaseLocation> possibleEnemyBaseLocations, Unit scoutDrone, int basesCount) {

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
