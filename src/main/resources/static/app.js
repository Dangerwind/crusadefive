'use strict';

const API = '/api/v1';

// ─── Состояние ───────────────────────────────────────────────────────────────
const state = {
  gameId:      null,
  playerId:    null,    // ID игрока для сохранения статистики
  playerName:  'Игрок 1',
  width:       15,
  height:      15,
  board:       [],    // board[y][x] = 'EMPTY' | 'PLAYER' | 'AI' | 'FIRE_PLACE' | 'WALL'
  tileMap:     [],    // tileMap[y][x] = 1..12, генерируется при старте
  rockMap:     [],    // rockMap[y][x] = 1..4, случайный камень для WALL
  levelName:   '',
  playerScore: 0,
  aiScore:     0,
  isWaiting:   false,
  isOver:      false,
  preserveScore: false, // Флаг для сохранения счета при смене уровня
};

// ─── Музыка ────────────────────────────────────────────────────────────────────
let startMusic = null;      // Музыка на стартовом экране
let gameMusic = null;       // Текущая музыка в игре

// Список игровых музыкальных файлов
const GAME_MUSIC_FILES = [
  'audio/music-01.mp3',
  'audio/music-02.mp3',
  'audio/music-03.mp3',
  'audio/music-04.mp3',
  'audio/music-05.mp3',
  'audio/music-06.mp3',
  'audio/music-07.mp3',
  'audio/music-08.mp3',
];

// Функция для получения случайного музыкального файла
function getRandomGameMusic() {
  const index = Math.floor(Math.random() * GAME_MUSIC_FILES.length);
  return GAME_MUSIC_FILES[index];
}

// Остановить текущую игровую музыку
function stopGameMusic() {
  if (gameMusic) {
    gameMusic.pause();
    gameMusic.currentTime = 0;
    gameMusic = null;
  }
}

// Остановить музыку стартового экрана
function stopStartMusic() {
  if (startMusic) {
    startMusic.pause();
    startMusic.currentTime = 0;
    startMusic = null;
  }
}

// Воспроизвести музыку стартового экрана
function playStartMusic() {
  stopGameMusic();
  stopStartMusic();
  startMusic = new Audio('audio/main-screen-01.mp3');
  startMusic.loop = true;
  startMusic.muted = musicMuted;
  startMusic.play().catch(() => {});
}

// Воспроизвести случайную игровую музыку
function playRandomGameMusic() {
  stopStartMusic();
  stopGameMusic();

  const musicFile = getRandomGameMusic();
  gameMusic = new Audio(musicFile);
  gameMusic.loop = false;
  gameMusic.muted = musicMuted;

  // Когда музыка заканчивается, запускаем следующую случайную
  gameMusic.addEventListener('ended', () => {
    playRandomGameMusic();
  });

  gameMusic.play().catch(() => {});
}

// ─── DOM ─────────────────────────────────────────────────────────────────────
const screenStart      = document.getElementById('screen-start');
const screenGame       = document.getElementById('screen-game');
const inputName        = document.getElementById('input-name');
const btnStart         = document.getElementById('btn-start');
const sectionReturning = document.getElementById('section-returning');
const sectionNew       = document.getElementById('section-new');
const welcomeName      = document.getElementById('welcome-name');
const welcomeScore     = document.getElementById('welcome-score');
const welcomeLevel     = document.getElementById('welcome-level');
const btnContinue      = document.getElementById('btn-continue');
const btnNewGame       = document.getElementById('btn-new-game');
const statsTbody       = document.getElementById('stats-tbody');
let selectedLevel      = 1;
const startError       = document.getElementById('start-error');
const hdrPlayerName  = document.getElementById('hdr-player-name');
const hdrLevelName   = document.getElementById('hdr-level-name');
const hdrPlayerScore = document.getElementById('hdr-player-score');
const hdrAiScore     = document.getElementById('hdr-ai-score');
const btnMenu        = document.getElementById('btn-menu');
const gameMenu       = document.getElementById('game-menu');
const menuSurrender  = document.getElementById('menu-surrender');
const menuSaveExit   = document.getElementById('menu-save-exit');
const menuRules      = document.getElementById('menu-rules');
const boardContainer = document.getElementById('board-container');
const overlayWinner  = document.getElementById('overlay-winner');
const overlayText    = document.getElementById('overlay-text');
const btnPlayAgain   = document.getElementById('btn-play-again');
const btnNextLevel   = document.getElementById('btn-next-level');
const btnMusicStart  = document.getElementById('btn-music-start');
const btnMusicGame   = document.getElementById('btn-music-game');

let musicMuted = false;

function applyMuteState() {
  const mute = musicMuted;
  if (startMusic) startMusic.muted = mute;
  if (gameMusic) gameMusic.muted = mute;

  btnMusicStart.classList.toggle('muted', mute);
  btnMusicGame.classList.toggle('muted', mute);

  const icon = mute ? 'none' : '';
  const mutedIcon = mute ? '' : 'none';
  btnMusicStart.querySelector('.speaker-icon').style.display = icon;
  btnMusicStart.querySelector('.speaker-muted').style.display = mutedIcon;
  btnMusicGame.querySelector('.speaker-icon').style.display = icon;
  btnMusicGame.querySelector('.speaker-muted').style.display = mutedIcon;
}

function toggleMusic() {
  musicMuted = !musicMuted;
  applyMuteState();
}

btnMusicStart.addEventListener('click', toggleMusic);
btnMusicGame.addEventListener('click', toggleMusic);

applyMuteState();

// ─── API-вызовы ───────────────────────────────────────────────────────────────
async function apiPost(path, body) {
  const res = await fetch(`${API}${path}`, {
    method:      'POST',
    credentials: 'include',
    headers:     { 'Content-Type': 'application/json' },
    body:        JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

async function apiGet(path) {
  const res = await fetch(`${API}${path}`, {
    method:      'GET',
    credentials: 'include',
  });
  return res;
}

// ─── Идентификация и статистика ──────────────────────────────────────────────
async function checkPlayer() {
  try {
    const res = await apiGet('/player/check');
    if (res.ok) {
      const player = await res.json();
      console.log('[checkPlayer] игрок найден:', player);
      state.playerName = (player.name ?? '').replace(/["']/g, '');
      state.playerId   = player.id;
      state.gameId      = player.gameId ?? null;
      state.continueLevel = player.level ?? 1;
      selectedLevel    = player.level ?? 1;
      console.log('[checkPlayer] установлено: gameId=', state.gameId, 'level=', selectedLevel);

      // Загружаем сохраненный счет из localStorage
      const savedScore = localStorage.getItem(`score_${state.playerId}`);
      if (savedScore !== null) {
        state.playerScore = parseInt(savedScore, 10);
      } else {
        state.playerScore = player.playerScore ?? 0;
      }

      // Загружаем сохраненный уровень из localStorage
      const savedLevel = localStorage.getItem(`level_${state.playerId}`);
      if (savedLevel !== null) {
        selectedLevel = parseInt(savedLevel, 10);
      }

      welcomeName.textContent  = (player.name ?? '').replace(/["']/g, '');
      welcomeScore.textContent = state.playerScore;

      sectionReturning.classList.remove('hidden');
      sectionNew.classList.add('hidden');
    } else {
      sectionNew.classList.remove('hidden');
      sectionReturning.classList.add('hidden');
    }
  } catch {
    sectionNew.classList.remove('hidden');
    sectionReturning.classList.add('hidden');
  }
}

async function loadLevels() {
  try {
    const res = await apiGet('/levels');
    if (!res.ok) return;
    const levels = await res.json();
    
    const container = document.getElementById('level-row-main');
    container.innerHTML = '';
    
    levels.forEach((levelName, index) => {
      const levelNum = index + 1;
      const btn = document.createElement('button');
      btn.className = 'btn-level-row';
      btn.dataset.level = levelNum;
      btn.innerHTML = `
        <span class="level-num">${levelNum}</span>
        <span class="level-name-text">${levelName}</span>
      `;
      container.appendChild(btn);
    });
    
    // Выделить сохранённый уровень
    const savedBtn = container.querySelector(`[data-level="${selectedLevel}"]`);
    if (savedBtn) {
      savedBtn.classList.add('active');
    } else if (container.firstChild) {
      container.firstChild.classList.add('active');
      selectedLevel = 1;
    }
  } catch (e) {
    console.error('Не удалось загрузить уровни', e);
  }
}

async function loadStatistics() {
  try {
    const res = await apiGet('/statistics');
    if (!res.ok) return;
    const players = await res.json();
    statsTbody.innerHTML = '';
    const top = players.slice(0, 10);
    top.forEach((p, index) => {
      const tr = document.createElement('tr');
      const date = p.updatedAt ? p.updatedAt.substring(0, 10) : '—';
      const cleanName = (p.name ?? '—').replace(/["']/g, '');
      tr.innerHTML = `
        <td>${index + 1}</td>
        <td>${cleanName}</td>
        <td>${p.playerScore ?? 0}</td>
        <td>${date}</td>
      `;
      statsTbody.appendChild(tr);
    });
    for (let i = top.length; i < 10; i++) {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${i + 1}</td><td></td><td></td><td></td>`;
      statsTbody.appendChild(tr);
    }
  } catch (e) {
    console.error('Не удалось загрузить статистику', e);
  }
}

// ─── Сохранение счета в localStorage ───────────────────────────────────────────
function saveScoreToLocalStorage() {
  if (state.playerId) {
    localStorage.setItem(`score_${state.playerId}`, state.playerScore.toString());
    localStorage.setItem(`level_${state.playerId}`, selectedLevel.toString());
  }
}

async function identifyPlayer(name) {
  const res = await fetch(`${API}/player/identify`, {
    method:      'POST',
    credentials: 'include',
    headers:     { 'Content-Type': 'application/json' },
    body:        JSON.stringify(name),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const player = await res.json();
  state.playerId = player.id; // Сохраняем ID игрока
  return player;
}

// ─── Сохранение статистики ───────────────────────────────────────────────────
// Вызывается ТОЛЬКО при нажатии "Сохранить и выйти"
async function saveStatistics() {
  if (!state.playerId) {
    console.warn('Нет ID игрока для сохранения статистики');
    return;
  }
  console.log('[saveStatistics] gameId:', state.gameId);
  console.log('[saveStatistics] playerName:', state.playerName);
  console.log('[saveStatistics] score:', state.playerScore);
  console.log('[saveStatistics] level:', selectedLevel);
  try {
    const res = await fetch(`${API}/statistics`, {
      method: 'PUT',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: state.playerId,
        name: state.playerName,
        playerScore: state.playerScore,
        level: selectedLevel,
        gameId: state.gameId
      }),
    });
    if (!res.ok) {
      console.error('Ошибка сохранения статистики:', res.status);
    } else {
      console.log('Статистика сохранена успешно');
      // Сохраняем в localStorage после успешной отправки на сервер
      localStorage.setItem(`score_${state.playerId}`, state.playerScore.toString());
      localStorage.setItem(`level_${state.playerId}`, selectedLevel.toString());
    }
  } catch (e) {
    console.error('Не удалось сохранить статистику', e);
  }
}

// ─── Инициализация доски ─────────────────────────────────────────────────────
function initBoard() {
  state.board = Array.from({ length: state.height }, () =>
    Array(state.width).fill('EMPTY')
  );
  // Случайная карта плиток — один раз при старте игры
  state.tileMap = Array.from({ length: state.height }, () =>
    Array.from({ length: state.width }, () => Math.floor(Math.random() * 12) + 1)
  );
  state.rockMap = Array.from({ length: state.height }, () =>
    Array.from({ length: state.width }, () => Math.floor(Math.random() * ROCK_COUNT) + 1)
  );
  // Сбрасываем счет только если не установлен флаг preserveScore
  if (!state.preserveScore) {
    state.playerScore = 0;
    state.aiScore     = 0;
  }
  state.preserveScore = false; // Сбрасываем флаг
  state.isWaiting   = false;
  state.isOver      = false;
}

const FIRE_FRAME_COUNT = 33;  // fire.png — горизонтальная полоса 33 кадра

// ─── Масштаб поля ────────────────────────────────────────────────────────────
const MAX_CELL   = 64;
const MIN_CELL   = 6;
const ROCK_COUNT = 10;
let currentCell  = 48;


/**
 * Пересчитывает размер клетки и обновляет CSS-переменную --cell-size.
 * Вызывается ТОЛЬКО при старте игры и при ресайзе окна.
 * НЕ вызывается при каждом ходе.
 */
function scaleBoard() {
  const hdr  = document.querySelector('.game-header');
  const hdrH = hdr ? hdr.getBoundingClientRect().height : 60;

  // Отступ сверху и снизу
  const PAD = 16;

  // Место по высоте (header + равные отступы сверху и снизу)
  const wrapperH = window.innerHeight - hdrH - PAD * 2;

  // Декоративные рыцари — 30vmin; резервируем место по горизонтали
  const decoSize = Math.round(0.30 * Math.min(window.innerWidth, window.innerHeight));
  const availW   = window.innerWidth - 2 * decoSize - PAD * 2;

  const cellByW = availW   / state.width;
  const cellByH = wrapperH / state.height;

  const cs = Math.max(MIN_CELL, Math.min(MAX_CELL, Math.floor(Math.min(cellByW, cellByH))));
  currentCell = cs;
  document.documentElement.style.setProperty('--cell-size', cs + 'px');

  // Пересчитываем keyframe рыцарей (CSS vars не работают в @keyframes)
  const ks     = Math.round(cs * 1.2);
  const stripW = 19 * ks;

  // Keyframes огня: горизонтальная полоса 33 кадра (как рыцари)
  const fs       = Math.round(cs * 1.2);
  const fireW    = FIRE_FRAME_COUNT * fs;

  let dynStyle = document.getElementById('dyn-knights');
  if (!dynStyle) {
    dynStyle    = document.createElement('style');
    dynStyle.id = 'dyn-knights';
    document.head.appendChild(dynStyle);
  }
  dynStyle.textContent = `
    @keyframes knight-idle {
      from { background-position: 0px 0px; }
      to   { background-position: ${-stripW}px 0px; }
    }
    @keyframes fire-idle {
      from { background-position: 0px 0px; }
      to   { background-position: ${-fireW}px 0px; }
    }
    .cell.fire-place::after { background-size: ${fireW}px ${fs}px; }
  `;

  if (boardContainer) {
    boardContainer.style.gridTemplateColumns = `repeat(${state.width}, ${cs}px)`;
  }
}

// ─── Рендер поля ─────────────────────────────────────────────────────────────
/**
 * Перерисовывает клетки поля.
 * НЕ пересчитывает масштаб — использует currentCell, выставленный scaleBoard().
 */
function renderBoard() {
  boardContainer.innerHTML = '';

  for (let y = 0; y < state.height; y++) {
    for (let x = 0; x < state.width; x++) {
      const cell = document.createElement('div');
      const type = state.board[y][x];
      cell.className  = 'cell ' + type.toLowerCase().replace('_', '-');
      cell.dataset.x  = x;
      cell.dataset.y  = y;

      // Трава: случайный тайл из tileMap
      const tileIdx = state.tileMap[y][x];
      cell.style.backgroundImage =
        `url('image/grass-${String(tileIdx).padStart(2, '0')}.png')`;

      // Рандомный стартовый кадр анимации рыцаря
      if (type === 'PLAYER' || type === 'AI') {
        cell.style.setProperty('--knight-delay', `-${(Math.random() * 3.8).toFixed(2)}s`);
      }

      if (type === 'FIRE_PLACE') {
        cell.style.setProperty('--fire-delay', `-${(Math.random() * 1.65).toFixed(2)}s`);
      }

      // Камень для WALL
      if (type === 'WALL') {
        const rockIdx = state.rockMap[y][x];
        cell.style.setProperty('--rock-url',
          `url('image/rock/rock-${String(rockIdx).padStart(2, '0')}.png')`);
      }

      if (type === 'EMPTY' && !state.isWaiting && !state.isOver) {
        cell.addEventListener('click', onCellClick);
      }
      boardContainer.appendChild(cell);
    }
  }
}

// ─── Обновить шапку ──────────────────────────────────────────────────────────
function renderHeader() {
  hdrPlayerName.textContent  = (state.playerName ?? '').replace(/["']/g, '');
  hdrLevelName.textContent   = state.levelName ? `Уровень: ${state.levelName}` : '';
  hdrPlayerScore.textContent = state.playerScore;
  hdrAiScore.textContent     = state.aiScore;

  // Сохраняем счет в localStorage при каждом обновлении
  saveScoreToLocalStorage();
}

// ─── Клик по клетке ──────────────────────────────────────────────────────────
async function onCellClick(e) {
  if (state.isWaiting || state.isOver) return;

  const x = parseInt(e.currentTarget.dataset.x, 10);
  const y = parseInt(e.currentTarget.dataset.y, 10);

  if (state.board[y][x] !== 'EMPTY') return;

  state.board[y][x] = 'PLAYER';
  state.isWaiting   = true;

  // Воспроизведение звука при установке рыцаря
  if (!musicMuted) {
    const insertSound = new Audio('audio/insert-01.mp3');
    insertSound.play().catch(() => {});
  }

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

    // Сохраняем счет в localStorage после каждого хода
    saveScoreToLocalStorage();

    renderHeader();

    if (res.winner) {
      state.isOver = true;
      renderBoard();
      showWinner(res.winner);
      return;
    }
  } catch (err) {
    console.error(err);
  } finally {
    state.isWaiting = false;
    renderBoard();
  }
}

// ─── Победитель ───────────────────────────────────────────────────────────────
function showWinner(winner) {
  if (winner === 'PLAYER') {
    overlayText.textContent = 'Ты выиграл,\nпоздравляю!';
    overlayText.className   = 'overlay-text win';
  } else {
    overlayText.textContent = 'Я выиграл!';
    overlayText.className   = 'overlay-text lose';
  }
  btnNextLevel.disabled = (selectedLevel >= 6);
  overlayWinner.classList.remove('hidden');

  // Сохраняем счет в localStorage при завершении игры
  if (state.playerId) {
    localStorage.setItem(`score_${state.playerId}`, state.playerScore.toString());
  }
}

// ─── Старт игры ───────────────────────────────────────────────────────────────
async function startGame(existingGameId = null, level = null) {
  const playerName = state.playerName || inputName.value.trim() || 'Игрок 1';

  state.playerName = playerName;

  console.log('[startGame] gameId:', existingGameId);
  console.log('[startGame] playerName:', playerName);
  console.log('[startGame] level:', selectedLevel);

  try {
    const res = await apiPost('/start', {
      playerName,
      gameId:    existingGameId,
      gameLevel: level ?? selectedLevel,
    });

    console.log('[startGame] ответ сервера:', res);

    state.gameId    = res.gameId;
    state.width     = res.fieldWidth;
    state.height    = res.fieldHeight;
    state.levelName = res.gameLevelName ?? '';
    
    // При продолжении игры не сбрасываем состояние
    const isContinuing = !!existingGameId;
    
    if (!isContinuing) {
      initBoard();
    } else {
      // При продолжении просто инициализируем пустое поле и tileMap
      state.board = Array.from({ length: state.height }, () =>
        Array(state.width).fill('EMPTY')
      );
      state.tileMap = Array.from({ length: state.height }, () =>
        Array.from({ length: state.width }, () => Math.floor(Math.random() * 12) + 1)
      );
      state.rockMap = Array.from({ length: state.height }, () =>
        Array.from({ length: state.width }, () => Math.floor(Math.random() * ROCK_COUNT) + 1)
      );
      state.isWaiting   = false;
      state.isOver      = false;
    }

    // применяем начальное состояние поля (WALL и другие не-EMPTY клетки)
    if (res.board) {
      for (const c of res.board) {
        if (c.y >= 0 && c.y < state.height && c.x >= 0 && c.x < state.width) {
          state.board[c.y][c.x] = c.cellType;
        }
      }
    }
    
    // Восстанавливаем счет из ответа сервера
    if (!state.preserveScore) {
      state.playerScore = res.playerScore ?? 0;
      state.aiScore     = res.aiScore ?? 0;
    } else {
      // При переходе на новый уровень сохраняем набранные очки
      state.aiScore = res.aiScore ?? 0;
      state.preserveScore = false;
    }

    renderHeader();

    screenStart.classList.add('hidden');
    screenGame.classList.remove('hidden');
    overlayWinner.classList.add('hidden');
    startError.classList.add('hidden');

    // Переключаем на игровую музыку
    playRandomGameMusic();

    // requestAnimationFrame гарантирует, что layout screenGame уже посчитан
    // (hdr.getBoundingClientRect() вернёт правильную высоту)
    requestAnimationFrame(() => {
      scaleBoard();
      renderBoard();
    });

  } catch (err) {
    startError.textContent = `Не удалось подключиться к серверу: ${err.message}`;
    startError.classList.remove('hidden');
    console.error(err);
  }
}

// ─── Перезапуск ───────────────────────────────────────────────────────────────
async function restartGame() {
  overlayWinner.classList.add('hidden');
  state.preserveScore = false;
  state.playerScore = 0;
  state.aiScore = 0;
  try {
    await startGame(null);
  } catch (err) {
    console.error(err);
  }
}

async function restartNextLevel() {
  overlayWinner.classList.add('hidden');

  // Сохраняем текущий счет перед переходом на следующий уровень
  state.preserveScore = true;

  // Сохраняем в localStorage перед переходом
  saveScoreToLocalStorage();

  selectedLevel = Math.min(6, selectedLevel + 1);
  // обновляем подсветку кнопок уровня
  document.querySelectorAll('.btn-level-row').forEach(b => {
    b.classList.toggle('active', parseInt(b.dataset.level, 10) === selectedLevel);
  });
  try {
    await startGame(state.gameId);
  } catch (err) {
    console.error(err);
  }
}

// ─── Слушатели событий ────────────────────────────────────────────────────────

// Новый игрок — создаём через /player/identify, потом стартуем
btnStart.addEventListener('click', async () => {
  const name = inputName.value.trim() || 'Игрок 1';
  try {
    const player = await identifyPlayer(name);
    state.playerName = (player.name ?? '').replace(/["']/g, '');
    state.playerId   = player.id;

    // Для нового игрока сбрасываем счет
    state.playerScore = 0;
    localStorage.setItem(`score_${state.playerId}`, '0');

    await startGame(null);
  } catch (err) {
    startError.textContent = `Ошибка: ${err.message}`;
    startError.classList.remove('hidden');
  }
});

// Возвращающийся — продолжить с тем же gameId
btnContinue.addEventListener('click', () => {
  const existingGameId = state.gameId;
  console.log('[Продолжить] gameId:', existingGameId);
  console.log('[Продолжить] playerName:', state.playerName);
  console.log('[Продолжить] level:', selectedLevel);
  startGame(existingGameId, state.continueLevel);
});

// Возвращающийся — начать заново
btnNewGame.addEventListener('click', () => {
  // Сбрасываем счет при начале новой игры
  state.preserveScore = false;
  state.playerScore = 0;
  state.aiScore = 0;
  if (state.playerId) {
    localStorage.setItem(`score_${state.playerId}`, '0');
  }
  startGame(null);
});

btnPlayAgain.addEventListener('click', restartGame);
btnNextLevel.addEventListener('click', restartNextLevel);

// Меню
btnMenu.addEventListener('click', e => {
  e.stopPropagation();
  gameMenu.classList.toggle('hidden');
});
document.addEventListener('click', () => gameMenu.classList.add('hidden'));

menuSurrender.addEventListener('click', async () => {
  gameMenu.classList.add('hidden');

  restartGame();
});

menuSaveExit.addEventListener('click', async () => {
  gameMenu.classList.add('hidden');

  await saveStatistics();

  screenGame.classList.add('hidden');
  screenStart.classList.remove('hidden');
  
  await checkPlayer();
  loadStatistics();

  // Переключаем на музыку стартового экрана
  playStartMusic();
});

menuRules.addEventListener('click', () => {
  gameMenu.classList.add('hidden');
  alert('Цель игры — выстроить 5 рыцарей подряд по горизонтали, вертикали или диагонали. Побеждает тот, кто захватил больше клеток после заполнения поля.');
});

// Кнопки уровней — по одному обработчику через делегирование
document.addEventListener('click', e => {
  const btn = e.target.closest('.btn-level-row');
  if (!btn) return;
  selectedLevel = parseInt(btn.dataset.level, 10);
  document.querySelectorAll('.btn-level-row').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
});

// Обновление активного уровня после загрузки игрока (когда уровни уже загружены)
const originalCheckPlayer = checkPlayer;
checkPlayer = async function() {
  await originalCheckPlayer();
  // После загрузки игрока и уровней, обновляем выделение
  setTimeout(() => {
    document.querySelectorAll('.btn-level-row').forEach(b => {
      b.classList.toggle('active', parseInt(b.dataset.level, 10) === selectedLevel);
    });
  }, 100);
};

// Resize: пересчитываем масштаб и перерисовываем поле
window.addEventListener('resize', () => {
  scaleBoard();
  renderBoard();
});

// Enter на стартовом экране
inputName.addEventListener('keydown', e => {
  if (e.key === 'Enter') btnStart.click();
});

// ─── Инициализация ────────────────────────────────────────────────────────────
checkPlayer();
loadStatistics();
loadLevels();

// Пытаемся запустить музыку стартового экрана (браузеры могут блокировать до первого взаимодействия)
playStartMusic();

// Запускаем музыку при взаимодействии пользователя (браузерная политика автовоспроизведения)
function startMusicOnInteraction() {
  // Если мы на стартовом экране и музыка не играет - запускаем
  if (screenStart && !screenStart.classList.contains('hidden')) {
    if (!startMusic || startMusic.paused) {
      playStartMusic();
    }
  }
}
// Слушаем все клики и нажатия клавиш для запуска музыки
document.addEventListener('click', startMusicOnInteraction);
document.addEventListener('keydown', startMusicOnInteraction);
