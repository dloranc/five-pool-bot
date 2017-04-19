package dloranc.fivepoolbot.debug;

import bwapi.*;
import bwta.*;
import bwta.Region;

import java.util.ArrayList;
import java.util.List;

public class DrawingDebug {
    private Game game = null;

    public DrawingDebug(Game game) {
        this.game = game;
    }

    public void draw(Player self, BaseLocation enemyBase) {

        drawChokepointLines();
        drawUnwalkablePolygons();
        drawMapRegions();

        for (Unit unit : self.getUnits()) {
            drawTargetLine(unit);
        }

        if (enemyBase != null) {
            game.drawCircleMap(enemyBase.getPosition(), 70, Color.Purple);
        }
    }

    private void drawChokepointLines() {
        List<Chokepoint> chokepoints = BWTA.getChokepoints();

        for (Chokepoint chokepoint : chokepoints) {
            Pair<Position, Position> sides = chokepoint.getSides();

            game.drawLineMap(sides.first, sides.second, Color.Yellow);
        }
    }

    private void drawMapRegions() {
        for (Region region : BWTA.getRegions()) {
            Polygon polygon = region.getPolygon();

            drawPolygon(polygon, Color.Teal, false);
        }
    }

    private void drawUnwalkablePolygons() {
        List<Polygon> polygons = BWTA.getUnwalkablePolygons();

        for (Polygon polygon : polygons) {
            drawPolygon(polygon, Color.Grey, true);
        }
    }

    /*
        scale parameter is for fixing bug, when user uses BWTA.getUnwalkablePolygons method
     */
    private void drawPolygon(Polygon polygon, Color color, boolean scale) {
        Position previousPosition = null;
        Position firstPosition = null;
        List<Position> positions = polygon.getPoints();

        List<Position> fixedPositions = new ArrayList<>();

        int scaleBy = 1;

        if (scale) {
            scaleBy = 8;
        }

        for (Position position : positions) {
            fixedPositions.add(new Position(position.getX() * scaleBy, position.getY() * scaleBy));
        }

        for (Position position : fixedPositions) {
            if (previousPosition == null) {
                previousPosition = position.getPoint();
                firstPosition = position.getPoint();
            } else {
                game.drawLineMap(previousPosition, position.getPoint(), color);
                previousPosition = position.getPoint();
            }
        }

        game.drawLineMap(firstPosition, fixedPositions.get(positions.size() - 1), Color.Grey);
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

        if (order == Order.Move) {
            color = Color.Yellow;
        }

        if (order == Order.MoveToGas || order == Order.WaitForGas || order == Order.HarvestGas || order == Order.ReturnGas) {
            color = Color.Green;
        }

        if (order == Order.MoveToMinerals || order == Order.MiningMinerals || order == Order.ReturnMinerals || order == Order.WaitForMinerals) {
            color = Color.Blue;
        }

        return color;
    }
}
