'use strict';

const API = 'http://localhost:8080/api/v1';

// ─── Состояние ───────────────────────────────────────────────────────────────
const state = {
  gameId:      null,
  playerName:  'Игрок 1',
  width:       15,
  height:      15,
  board:       [],       // board[y][x] = 'EMPTY' | 'PLAYER' | 'AI' | 'FIRE_PLACE' | 'WALL'
  playerScore: 0,
  aiScore:     0,
  isWaiting:   false,
  isOver:      false,
};

// ─── DOM ─────────────────────────────────────────────────────────────────────
const screenStart   = document.getElementById('screen-start');
const screenGame    = document.getElementById('screen-game');
const inputName     = document.getElementById('input-name');
const inputWidth    = document.getElementById('input-width');
const inputHeight   = document.getElementById('input-height');
const btnStart      = document.getElementById('btn-start');
const startError    = document.getElementById('start-error');
const hdrPlayerName = document.getElementById('hdr-player-name');
const hdrGameId     = document.getElementById('hdr-game-id');
const hdrPlayerScore= document.getElementById('hdr-player-score');
const hdrAiScore    = document.getElementById('hdr-ai-score');
const btnRestart    = document.getElementById('btn-restart');
const boardContainer= document.getElementById('board-container');
const statusMsg     = document.getElementById('status-msg');
const overlayWinner = document.getElementById('overlay-winner');
const overlayText   = document.getElementById('overlay-text');
const btnPlayAgain  = document.getElementById('btn-play-again');

// ─── API-вызовы ───────────────────────────────────────────────────────────────
async function apiPost(path, body) {
  const res = await fetch(`${API}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// ─── Инициализация доски ─────────────────────────────────────────────────────
function initBoard() {
  state.board = Array.from({ length: state.height }, () =>
    Array(state.width).fill('EMPTY')
  );
  state.playerScore = 0;
  state.aiScore     = 0;
  state.isWaiting   = false;
  state.isOver      = false;
}

// ─── Рендер поля ─────────────────────────────────────────────────────────────
function renderBoard() {
  boardContainer.style.gridTemplateColumns = `repeat(${state.width}, 48px)`;
  boardContainer.innerHTML = '';

  for (let y = 0; y < state.height; y++) {
    for (let x = 0; x < state.width; x++) {
      const cell = document.createElement('div');
      const type = state.board[y][x];
      cell.className = 'cell ' + type.toLowerCase().replace('_', '-');
      cell.dataset.x = x;
      cell.dataset.y = y;
      if (type === 'EMPTY' && !state.isWaiting && !state.isOver) {
        cell.addEventListener('click', onCellClick);
      }
      boardContainer.appendChild(cell);
    }
  }
}

// ─── Обновить шапку ──────────────────────────────────────────────────────────
function renderHeader() {
  hdrPlayerName.textContent  = state.playerName;
  hdrGameId.textContent      = `gameId: ${state.gameId}`;
  hdrPlayerScore.textContent = state.playerScore;
  hdrAiScore.textContent     = state.aiScore;
}

// ─── Клик по клетке ──────────────────────────────────────────────────────────
async function onCellClick(e) {
  if (state.isWaiting || state.isOver) return;

  const x = parseInt(e.currentTarget.dataset.x, 10);
  const y = parseInt(e.currentTarget.dataset.y, 10);

  if (state.board[y][x] !== 'EMPTY') return;

  // Ставим ход игрока
  state.board[y][x] = 'PLAYER';
  state.isWaiting = true;
  setStatus('Ход AI...');
  renderBoard();

  try {
    const res = await apiPost('/move', {
      gameId:     state.gameId,
      playerMove: { x, y, cellType: 'PLAYER' },
      aiMove:     null,
    });

    // Ход AI
    if (res.aiMove) {
      const ax = res.aiMove.x;
      const ay = res.aiMove.y;
      if (ay >= 0 && ay < state.height && ax >= 0 && ax < state.width) {
        state.board[ay][ax] = 'AI';
      }
    }

    // Горящие клетки
    if (res.burnedCells) {
      for (const c of res.burnedCells) {
        if (c.y >= 0 && c.y < state.height && c.x >= 0 && c.x < state.width) {
          state.board[c.y][c.x] = c.cellType;
        }
      }
    }

    state.playerScore = res.playerScore ?? state.playerScore;
    state.aiScore     = res.aiScore     ?? state.aiScore;

    renderHeader();

    if (res.winner) {
      state.isOver = true;
      renderBoard();
      showWinner(res.winner);
      return;
    }

    setStatus('Ваш ход');
  } catch (err) {
    setStatus(`Ошибка: ${err.message}`);
    console.error(err);
  } finally {
    state.isWaiting = false;
    renderBoard();
  }
}

// ─── Победитель ───────────────────────────────────────────────────────────────
function showWinner(winner) {
  overlayWinner.classList.remove('hidden');
  if (winner === 'PLAYER') {
    overlayText.textContent = '🎉 Вы победили!';
    overlayText.className = 'overlay-text win';
  } else {
    overlayText.textContent = '😞 Победил AI';
    overlayText.className = 'overlay-text lose';
  }
}

// ─── Статус-строка ────────────────────────────────────────────────────────────
function setStatus(msg) {
  statusMsg.textContent = msg;
}

// ─── Старт игры ───────────────────────────────────────────────────────────────
async function startGame(existingGameId = null) {
  const playerName = inputName.value.trim() || 'Игрок 1';
  const width      = Math.max(5, Math.min(30, parseInt(inputWidth.value,  10) || 15));
  const height     = Math.max(5, Math.min(30, parseInt(inputHeight.value, 10) || 15));

  state.playerName = playerName;
  state.width      = width;
  state.height     = height;

  try {
    const res = await apiPost('/start', {
      playerName,
      gameId:      existingGameId,
      fieldWidth:  width,
      fieldHeight: height,
    });

    state.gameId = res.gameId;
    initBoard();
    renderHeader();
    renderBoard();
    setStatus('Ваш ход');

    screenStart.classList.add('hidden');
    screenGame.classList.remove('hidden');
    overlayWinner.classList.add('hidden');

    startError.classList.add('hidden');
  } catch (err) {
    startError.textContent = `Не удалось подключиться к серверу: ${err.message}`;
    startError.classList.remove('hidden');
    console.error(err);
  }
}

// ─── Перезапуск ───────────────────────────────────────────────────────────────
async function restartGame() {
  overlayWinner.classList.add('hidden');
  setStatus('Перезапуск...');

  try {
    if (state.gameId) {
      await apiPost('/surrender', { gameId: state.gameId });
    }
    // Начинаем новую игру с теми же параметрами
    await startGame(null);
  } catch (err) {
    setStatus(`Ошибка перезапуска: ${err.message}`);
    console.error(err);
  }
}

// ─── Слушатели событий ────────────────────────────────────────────────────────
btnStart.addEventListener('click', () => startGame(null));

btnRestart.addEventListener('click', restartGame);
btnPlayAgain.addEventListener('click', restartGame);

// Enter на стартовом экране
[inputName, inputWidth, inputHeight].forEach(el => {
  el.addEventListener('keydown', e => { if (e.key === 'Enter') startGame(null); });
});
