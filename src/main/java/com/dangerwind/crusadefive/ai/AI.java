package com.dangerwind.crusadefive.ai;

import com.dangerwind.crusadefive.dto.Cell;
import com.dangerwind.crusadefive.dto.CellType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AI {

    private static final int[][] WEIGHTS = {
//          1 конец     2 конца  открыто
            { 0,        0       },  // [0] - пустая клетка
            { 5,        10      },  // [1] - один
            { 50,       200     },  // [2] - двойка
            { 500,      5000    },  // [3] - тройка
            { 100000,   100000  },  // [4] - четвёрка
            { 1000000,  1000000 },  // [5] - пять в ряд
    };

    private static final int[][] DIRECTION = new int[][]{
            {0, 1},   // вертикаль
            {1, 0},   // горизонталь
            {1, 1},   // диагональ \
            {1, -1}   // диагональ /
    };

    private static final double INDEX_PLAYER = 1.0;
    private static final double INDEX_AI = 1.0;


    private static boolean isInBoard(CellType[][] board, int x, int y) {
        return (x >= 0 && x < board.length &&
                y >= 0 && y < board[0].length);
    }

    // Считает длину цепочки и открытые концы в одном направлении (dx, dy),
    // возвращает взвешенную оценку для данной клетки.
    private static double evaluateDirection(
            CellType[][] board,
            int x, int y,
            int dx, int dy,
            CellType type) {

        int count = 1;      // 1 — текущая клетка куда ставим фишку
        int openEnds = -1;  // -1: закрыто, 0: один конец открыт, 1: оба конца открыты

        // Прямой проход
        int nx = x + dx;
        int ny = y + dy;
        while (isInBoard(board, nx, ny) && board[nx][ny] == type) {
            count++;
            nx += dx;
            ny += dy;
        }
        if (isInBoard(board, nx, ny) && board[nx][ny] == CellType.EMPTY) openEnds++;

        // Обратный проход
        nx = x - dx;
        ny = y - dy;
        while (isInBoard(board, nx, ny) && board[nx][ny] == type) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        if (isInBoard(board, nx, ny) && board[nx][ny] == CellType.EMPTY) openEnds++;

        if (count >= 5) return WEIGHTS[WEIGHTS.length - 1][1];

        if (openEnds >= 0) {
            int capped = Math.min(count, WEIGHTS.length);
            return WEIGHTS[capped - 1][openEnds];
        }

        return 0.0;
    }

    // считает сколько фишек стоит в ряд по одному направлению (dx, dy)
    private static int countDirection(
            CellType[][] board,
            int x, int y,
            int dx, int dy,
            CellType type) {

        int count = 1;      // 1 — текущая клетка куда ставим фишку


        // Прямой проход
        int nx = x + dx;
        int ny = y + dy;

        while (isInBoard(board, nx, ny) && board[nx][ny] == type) {
            count++;
            nx += dx;
            ny += dy;
        }

        // Обратный проход
        nx = x - dx;
        ny = y - dy;
        while (isInBoard(board, nx, ny) && board[nx][ny] == type) {
            count++;
            nx -= dx;
            ny -= dy;
        }

        return count;
    }

    public static boolean isWinner(CellType[][] board, int x, int y, CellType type) {
        for (int[] direction : DIRECTION) {
            if (countDirection(board, x, y, direction[0], direction[1], type) >= 5) {
                return true;
            }
        }
        return false;
    }


    public static boolean isBoardFull(CellType[][] board) {
        for (int y = 0; y < board[0].length; y++) {
            for (int x = 0; x < board.length; x++) {
                if (board[x][y] == CellType.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Cell calculateBestMove(CellType[][] board) {

        int[] maxXY = new int[2];
        double maxWeight = 0;

        boolean haveBestMove = false;

        for (int y = 0; y < board[0].length; y++) {
            for (int x = 0; x < board.length; x++) {

                if (board[x][y] != CellType.EMPTY) continue;

                double totalWeightPlayer = 0;
                double totalWeightAi = 0;

                for (int[] direction : DIRECTION) {

                    double playerScore = evaluateDirection(board, x, y, direction[0], direction[1],
                            CellType.PLAYER) * INDEX_PLAYER;
                    if (totalWeightPlayer < playerScore) totalWeightPlayer = playerScore;

                    double aiScore = evaluateDirection(board, x, y, direction[0], direction[1],
                            CellType.AI) * INDEX_AI;
                    if (totalWeightAi < aiScore) totalWeightAi = aiScore;
                }

                double best = Math.max(totalWeightPlayer, totalWeightAi);
                if (maxWeight < best) {
                    maxWeight = best;
                    maxXY[0] = x;
                    maxXY[1] = y;
                    haveBestMove = true;
                }
            }
        }

        // если нет лучшего хода, поставить в центр, если он свободен, иначе в первую попавшуюся пустую клетку
        if  (!haveBestMove) {
            int centerX = board.length / 2;
            int centerY = board[0].length / 2;

            if (board[centerX][centerY] == CellType.EMPTY) {
                return new Cell(centerX, centerY, CellType.AI);
            }

            centerX = (int) (Math.random() * board.length);
            centerY = (int) (Math.random() * board[0].length);

            // перебираем клетки по кругу от центра, пока не найдём пустую
            while (board[centerX][centerY] != CellType.EMPTY) {
                centerX++;
                if (centerX >= board.length) { centerX = 0; centerY++; }
                if (centerY >= board[0].length) { centerY = 0; centerX = 0; }
            }
            return new Cell(centerX, centerY, CellType.AI);
        }

        return new Cell(maxXY[0], maxXY[1], CellType.AI);
    }

// чем закрашиваем клетки
public static List<Cell> burnCells(CellType[][] board, int startX, int startY, CellType type) {
    List<Cell> burned = new ArrayList<>();
    Queue<int[]> queue = new LinkedList<>();



    board[startX][startY] = type;
    queue.add(new int[]{startX, startY});
    burned.add(new Cell(startX, startY, type)); // первая клетка сгорела

    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}; // все 8 направлений

    while (!queue. isEmpty()) {
        int[] cell = queue.poll();
        int x = cell[0];
        int y = cell[1];

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (isInBoard(board, nx, ny) && ( board[nx][ny] == CellType.AI || board[nx][ny] == CellType.PLAYER) ) {
                board[nx][ny] = type;
                queue.add(new int[]{nx, ny});
                burned.add(new Cell(nx, ny, type));
                System.out.println("  Сгорела клетка: (" + nx + ", " + ny + ") всего=" + queue.size());
            }
        }
    }

    return burned;
}
}
