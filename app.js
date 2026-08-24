// Fast Master Progressive Web App - Core Controller

// --- Data Constants ---
const PLANS = {
    PLAN_16_8: { key: 'PLAN_16_8', name: '16:8', fastingHours: 16, eatingHours: 8, emoji: '🟢', difficulty: 'Débutant', desc: '16h de jeûne et 8h de repas. Le protocole le plus populaire et accessible.' },
    PLAN_18_6: { key: 'PLAN_18_6', name: '18:6', fastingHours: 18, eatingHours: 6, emoji: '🟡', difficulty: 'Intermédiaire', desc: '18h de jeûne. Accélère la combustion des graisses et l\'autophagie.' },
    PLAN_20_4: { key: 'PLAN_20_4', name: '20:4', fastingHours: 20, eatingHours: 4, emoji: '🟠', difficulty: 'Avancé (Warrior)', desc: '20h de jeûne, 4h pour manger. Résultats métaboliques décuplés.' },
    PLAN_OMAD: { key: 'PLAN_OMAD', name: 'OMAD (23:1)', fastingHours: 23, eatingHours: 1, emoji: '🔴', difficulty: 'Expert', desc: 'Un seul repas complet par jour. Autophagie profonde et clarté maximale.' },
    PLAN_FLEXIBLE_16_8: { key: 'PLAN_FLEXIBLE_16_8', name: '16:8 Flexible', fastingHours: 16, eatingHours: 8, emoji: '🔄', difficulty: 'Adaptatif', desc: '16:8 du Lundi au Vendredi, repos et liberté le week-end.' }
};

const STAGES = [
    {
        id: 1, name: 'Digestion', startHour: 0, endHour: 4, emoji: '🍽️', color: '#9E9E9E',
        shortDesc: 'Votre corps digère le dernier repas',
        fullDesc: 'L\'estomac et l\'intestin absorbent les nutriments. Le pancréas sécrète de l\'insuline pour acheminer le glucose dans vos cellules.',
        effects: ['📈 Insuline élevée', '⚡ Le glucose est la source d\'énergie principale', '🧘 Satiété naturelle'],
        tip: 'Buvez de l\'eau tempérée pour faciliter la digestion. Évitez tout grignotage.'
    },
    {
        id: 2, name: 'Baisse de l\'insuline', startHour: 4, endHour: 8, emoji: '📉', color: '#42A5F5',
        shortDesc: 'L\'insuline diminue, le stockage s\'arrête',
        fullDesc: 'La glycémie redescend à son niveau de base. Le foie commence à décomposer le glycogène stocké pour fournir de l\'énergie.',
        effects: ['📉 L\'insuline chute — fin du stockage de graisse', '🏦 Déstockage du glycogène hépatique', '💧 Élimination de la rétention d\'eau'],
        tip: 'Si la faim se fait sentir, buvez un thé vert ou café noir sans sucre.'
    },
    {
        id: 3, name: 'Combustion des graisses', startHour: 8, endHour: 12, emoji: '🔥', color: '#FF9100',
        shortDesc: 'La lipolyse s\'active à plein régime',
        fullDesc: 'Le glycogène s\'épuise. Votre corps commence à décomposer les cellules adipeuses en acides gras libres pour carburant.',
        effects: ['🔥 Lipolyse active (perte de graisse)', '⬆️ Hormone de croissance (HGH) en hausse (+500%)', '🏋️ Muscles protégés'],
        tip: 'Moment idéal pour une marche légère ou une séance de sport douce.'
    },
    {
        id: 4, name: 'Cétose & Clarté', startHour: 12, endHour: 18, emoji: '🧬', color: '#7C4DFF',
        shortDesc: 'Production de cétones, clarté mentale',
        fullDesc: 'Le foie convertit les graisses en corps cétoniques (BHB). Le cerveau utilise ce super-carburant propre : énergie stable et focus accru.',
        effects: ['🧪 Le foie produit des cétones (BHB)', '🧠 Énergie cérébrale constante sans coup de barre', '🔬 Réduction de l\'inflammation'],
        tip: 'Profitez de cette clarté pour travailler sur des tâches complexes ou créer.'
    },
    {
        id: 5, name: 'Autophagie', startHour: 18, endHour: 24, emoji: '🔄', color: '#00BFA5',
        shortDesc: 'Les cellules se nettoient et se régénèrent',
        fullDesc: 'Le recyclage cellulaire interne s\'enclenche : les cellules éliminent leurs composants défectueux et leurs protéines toxiques.',
        effects: ['🧹 Élimination des déchets cellulaires', '🧬 Réparation accélérée de l\'ADN', '⏳ Effet anti-âge profond'],
        tip: 'Restez parfaitement hydraté avec de l\'eau pure ou des infusions sans calorie.'
    },
    {
        id: 6, name: 'Régénération profonde', startHour: 24, endHour: 72, emoji: '⚡', color: '#FF1744',
        shortDesc: 'Renouvellement des cellules souches',
        fullDesc: 'Le jeûne prolongé déclenche un reset complet du système immunitaire et une poussée d\'hormone de croissance jusqu\'à +2000%.',
        effects: ['🌱 Activation des cellules souches', '🔄 Réinitialisation du système immunitaire', '💉 Sensibilité à l\'insuline maximale'],
        tip: 'Réservé aux jeûneurs expérimentés. Assurez-vous d\'avoir des électrolytes.'
    }
];

// --- State Management ---
class AppState {
    constructor() {
        this.load();
    }

    load() {
        const savedUser = localStorage.getItem('fastmaster_user');
        this.user = savedUser ? JSON.parse(savedUser) : {
            name: 'Jeûneur',
            weight: 70,
            height: 175,
            darkMode: true,
            sound: true,
            autoStart: false,
            scheduleHour: 20,
            scheduleMinute: 0
        };

        const savedFasting = localStorage.getItem('fastmaster_active');
        this.fasting = savedFasting ? JSON.parse(savedFasting) : {
            isActive: false,
            startTime: 0,
            planKey: 'PLAN_16_8'
        };

        const savedSessions = localStorage.getItem('fastmaster_sessions');
        this.sessions = savedSessions ? JSON.parse(savedSessions) : [];

        const savedWeights = localStorage.getItem('fastmaster_weights');
        this.weights = savedWeights ? JSON.parse(savedWeights) : [];
    }

    save() {
        localStorage.setItem('fastmaster_user', JSON.stringify(this.user));
        localStorage.setItem('fastmaster_active', JSON.stringify(this.fasting));
        localStorage.setItem('fastmaster_sessions', JSON.stringify(this.sessions));
        localStorage.setItem('fastmaster_weights', JSON.stringify(this.weights));
        if (typeof triggerAutoSync === 'function') {
            triggerAutoSync();
        }
    }
}

const state = new AppState();
let deferredPrompt = null;
let audioCtx = null;

// --- Sound Synthesizer ---
function playChime(success = true) {
    if (!state.user.sound) return;
    try {
        if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        if (audioCtx.state === 'suspended') audioCtx.resume();
        
        const now = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        
        if (success) {
            osc.frequency.setValueAtTime(523.25, now);
            osc.frequency.exponentialRampToValueAtTime(659.25, now + 0.15);
            osc.frequency.exponentialRampToValueAtTime(783.99, now + 0.3);
            gain.gain.setValueAtTime(0.3, now);
            gain.gain.exponentialRampToValueAtTime(0.01, now + 0.6);
            osc.start(now);
            osc.stop(now + 0.6);
        } else {
            osc.frequency.setValueAtTime(440, now);
            osc.frequency.exponentialRampToValueAtTime(349.23, now + 0.2);
            gain.gain.setValueAtTime(0.3, now);
            gain.gain.exponentialRampToValueAtTime(0.01, now + 0.4);
            osc.start(now);
            osc.stop(now + 0.4);
        }
    } catch (e) {
        console.warn('Audio not allowed yet', e);
    }
}

// --- Notification Dispatcher ---
function sendBrowserNotification(title, body) {
    if ('Notification' in window && Notification.permission === 'granted') {
        navigator.serviceWorker.ready.then(reg => {
            reg.showNotification(title, {
                body: body,
                icon: './icons/icon-192.png',
                badge: './icons/icon-192.png',
                vibrate: [200, 100, 200]
            });
        }).catch(() => {
            new Notification(title, { body: body, icon: './icons/icon-192.png' });
        });
    }
}

// --- UI Navigation ---
function switchTab(tabId) {
    document.querySelectorAll('.tab-view').forEach(view => view.classList.remove('active'));
    document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));

    const activeView = document.getElementById('view-' + tabId);
    const activeBtn = document.querySelector('.nav-tab[data-tab="' + tabId + '"]');

    if (activeView) activeView.classList.add('active');
    if (activeBtn) activeBtn.classList.add('active');

    if (tabId === 'weight') {
        renderWeightView();
    } else if (tabId === 'history') {
        renderHistoryView();
    }
}

// --- Fasting Timer Logic ---
function updateTimer() {
    const plan = PLANS[state.fasting.planKey] || PLANS.PLAN_16_8;
    const circleRing = document.getElementById('timerProgressRing');
    const badge = document.getElementById('timerBadge');
    const digits = document.getElementById('timerDigits');
    const subtext = document.getElementById('timerSubtext');
    const percentEl = document.getElementById('timerPercent');
    const mainBtn = document.getElementById('btnMainAction');
    const stageCard = document.getElementById('stageCard');

    const circumference = 785;

    if (state.fasting.isActive) {
        const elapsedMs = Date.now() - state.fasting.startTime;
        const targetMs = plan.fastingHours * 3600000;
        const elapsedHours = elapsedMs / 3600000;
        const progress = Math.min(elapsedMs / targetMs, 1);

        const totalSeconds = Math.floor(elapsedMs / 1000);
        const hrs = Math.floor(totalSeconds / 3600);
        const mins = Math.floor((totalSeconds % 3600) / 60);
        const secs = totalSeconds % 60;
        digits.textContent = String(hrs).padStart(2, '0') + ':' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');

        const offset = circumference - (progress * circumference);
        circleRing.style.strokeDashoffset = offset;

        badge.className = 'timer-status-badge';
        badge.textContent = '🔒 JEÛNE EN COURS • PLAN ' + plan.name;
        
        const remainingMs = targetMs - elapsedMs;
        if (remainingMs > 0) {
            const remHrs = Math.floor(remainingMs / 3600000);
            const remMins = Math.floor((remainingMs % 3600000) / 60000);
            subtext.textContent = 'Objectif dans ' + remHrs + 'h ' + remMins + 'm';
        } else {
            subtext.textContent = '🎉 Objectif de ' + plan.fastingHours + 'h atteint !';
        }

        percentEl.textContent = Math.floor(progress * 100) + '%';

        mainBtn.innerHTML = '<span>⏹</span> Arrêter le jeûne';
        mainBtn.className = 'btn-main-action stop-mode';

        let currentStage = STAGES[0];
        for (let i = STAGES.length - 1; i >= 0; i--) {
            if (elapsedHours >= STAGES[i].startHour) {
                currentStage = STAGES[i];
                break;
            }
        }

        const stageDuration = currentStage.endHour - currentStage.startHour;
        const stageElapsed = Math.max(0, Math.min(elapsedHours - currentStage.startHour, stageDuration));
        const stageProgress = (stageElapsed / stageDuration) * 100;

        document.getElementById('stageBadge').textContent = 'Étape ' + currentStage.id + '/6 • ' + currentStage.startHour + 'h-' + currentStage.endHour + 'h';
        document.getElementById('stageTitle').innerHTML = currentStage.emoji + ' ' + currentStage.name;
        document.getElementById('stageDesc').textContent = currentStage.shortDesc;
        document.getElementById('stageProgressFill').style.width = stageProgress + '%';
        document.getElementById('stageProgressFill').style.background = currentStage.color;
        stageCard.style.display = 'block';

    } else {
        circleRing.style.strokeDashoffset = circumference;
        badge.className = 'timer-status-badge idle';
        badge.textContent = '⏸️ PRÊT • PLAN ' + plan.name;
        digits.textContent = String(plan.fastingHours).padStart(2, '0') + ':00:00';
        subtext.textContent = 'Plan sélectionné : ' + plan.name + ' (' + plan.fastingHours + 'h jeûne)';
        percentEl.textContent = '0%';

        mainBtn.innerHTML = '<span>▶️</span> Commencer le jeûne';
        mainBtn.className = 'btn-main-action';

        document.getElementById('stageBadge').textContent = 'Étape 1/6 • Préparation';
        document.getElementById('stageTitle').innerHTML = '🍽️ 1. Digestion';
        document.getElementById('stageDesc').textContent = 'Prêt pour votre prochaine étape biologique.';
        document.getElementById('stageProgressFill').style.width = '0%';
        stageCard.style.display = 'block';
    }

    updateQuickStats();
}

function toggleFasting() {
    if (state.fasting.isActive) {
        const now = Date.now();
        const durationMs = now - state.fasting.startTime;
        const plan = PLANS[state.fasting.planKey] || PLANS.PLAN_16_8;
        const isCompleted = durationMs >= (plan.fastingHours * 3600000);

        state.sessions.unshift({
            id: Date.now().toString(),
            planKey: state.fasting.planKey,
            planName: plan.name,
            startTime: state.fasting.startTime,
            endTime: now,
            durationMillis: durationMs,
            completed: isCompleted
        });

        state.fasting.isActive = false;
        state.fasting.startTime = 0;
        state.save();

        playChime(isCompleted);
        showToast(isCompleted ? '🎉 Bravo ! Jeûne complété et enregistré.' : '⏹ Jeûne arrêté et enregistré.');
        sendBrowserNotification('Jeûne terminé', 'Session de ' + Math.floor(durationMs / 3600000) + 'h enregistrée.');
    } else {
        state.fasting.isActive = true;
        state.fasting.startTime = Date.now();
        state.save();

        playChime(true);
        const plan = PLANS[state.fasting.planKey] || PLANS.PLAN_16_8;
        showToast('▶️ C\'est parti pour ' + plan.fastingHours + 'h de jeûne !');
        sendBrowserNotification('Jeûne commencé ⏳', 'Votre jeûne de ' + plan.fastingHours + 'h vient de débuter.');
    }

    updateTimer();
}

function updateQuickStats() {
    const totalFasts = state.sessions.length;
    const completedFasts = state.sessions.filter(s => s.completed).length;
    let totalMs = 0;
    state.sessions.forEach(s => totalMs += (s.durationMillis || 0));
    const totalHours = Math.floor(totalMs / 3600000);

    const totalEl = document.getElementById('statTotalFasts');
    const compEl = document.getElementById('statCompleted');
    const hrsEl = document.getElementById('statTotalHours');
    if (totalEl) totalEl.textContent = totalFasts;
    if (compEl) compEl.textContent = completedFasts;
    if (hrsEl) hrsEl.textContent = totalHours + 'h';
}

function renderPlans() {
    const list = document.getElementById('plansList');
    if (!list) return;
    list.innerHTML = '';

    Object.values(PLANS).forEach(p => {
        const isSelected = state.fasting.planKey === p.key;
        const card = document.createElement('div');
        card.className = 'plan-card ' + (isSelected ? 'selected' : '');
        card.innerHTML = '<div class="plan-card-left"><span class="plan-emoji">' + p.emoji + '</span><div><div class="plan-name">Plan ' + p.name + '</div><div class="plan-desc">' + p.desc + '</div></div></div><div class="plan-tag">' + (isSelected ? 'Actif ✓' : p.difficulty) + '</div>';
        card.onclick = () => {
            state.fasting.planKey = p.key;
            state.save();
            renderPlans();
            updateTimer();
            showToast('Plan ' + p.name + ' activé !');
        };
        list.appendChild(card);
    });
}

function calculateBMI(weightKg, heightCm) {
    if (!weightKg || !heightCm) return { bmi: 0, label: 'Non renseigné', color: '#9BA3B5' };
    const hM = heightCm / 100;
    const bmi = +(weightKg / (hM * hM)).toFixed(1);

    if (bmi < 18.5) return { bmi, label: 'Sous-poids', color: '#3B82F6' };
    if (bmi < 25.0) return { bmi, label: 'Poids normal ✓', color: '#10B981' };
    if (bmi < 30.0) return { bmi, label: 'Surpoids', color: '#F59E0B' };
    return { bmi, label: 'Obésité', color: '#EF4444' };
}

function renderWeightView() {
    const currentWeight = state.user.weight || 70;
    const currentHeight = state.user.height || 175;
    const bmiData = calculateBMI(currentWeight, currentHeight);

    const bmiEl = document.getElementById('bmiVal');
    const badge = document.getElementById('bmiBadge');
    if (bmiEl) bmiEl.textContent = bmiData.bmi || '--';
    if (badge) {
        badge.textContent = bmiData.label;
        badge.style.color = bmiData.color;
        badge.style.background = bmiData.color + '22';
    }

    const list = document.getElementById('weightList');
    if (!list) return;
    list.innerHTML = '';

    if (state.weights.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding: 20px; color: var(--text-dim); font-size: 13px;">Aucune pesée enregistrée pour le moment.</div>';
    } else {
        state.weights.forEach((item, index) => {
            const row = document.createElement('div');
            row.className = 'weight-item';
            const dateStr = new Date(item.date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
            row.innerHTML = '<div><div class="weight-item-val">' + item.weight + ' kg</div><div class="weight-item-date">' + dateStr + '</div></div><button class="weight-delete-btn" onclick="deleteWeight(' + index + ')">🗑</button>';
            list.appendChild(row);
        });
    }

    drawWeightChart();
}

function drawWeightChart() {
    const canvas = document.getElementById('weightCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.offsetWidth;
    const height = canvas.offsetHeight;
    
    canvas.width = width * window.devicePixelRatio;
    canvas.height = height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

    ctx.clearRect(0, 0, width, height);

    const data = [...state.weights].reverse();
    if (data.length < 2) {
        ctx.fillStyle = '#646E82';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('Ajoutez au moins 2 pesées pour tracer la courbe', width / 2, height / 2);
        return;
    }

    const weights = data.map(d => d.weight);
    const minW = Math.min(...weights) - 1;
    const maxW = Math.max(...weights) + 1;
    const pad = 24;

    const getX = (i) => pad + (i / (data.length - 1)) * (width - pad * 2);
    const getY = (w) => height - pad - ((w - minW) / (maxW - minW)) * (height - pad * 2);

    const grad = ctx.createLinearGradient(0, 0, 0, height);
    grad.addColorStop(0, 'rgba(255, 140, 0, 0.3)');
    grad.addColorStop(1, 'rgba(255, 140, 0, 0.0)');

    ctx.beginPath();
    ctx.moveTo(getX(0), getY(weights[0]));
    for (let i = 1; i < weights.length; i++) {
        ctx.lineTo(getX(i), getY(weights[i]));
    }
    ctx.lineTo(getX(weights.length - 1), height - pad);
    ctx.lineTo(getX(0), height - pad);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(getX(0), getY(weights[0]));
    for (let i = 1; i < weights.length; i++) {
        ctx.lineTo(getX(i), getY(weights[i]));
    }
    ctx.strokeStyle = '#FF8C00';
    ctx.lineWidth = 3;
    ctx.stroke();

    for (let i = 0; i < weights.length; i++) {
        ctx.beginPath();
        ctx.arc(getX(i), getY(weights[i]), 4, 0, Math.PI * 2);
        ctx.fillStyle = '#FFFFFF';
        ctx.fill();
        ctx.strokeStyle = '#FF8C00';
        ctx.lineWidth = 2;
        ctx.stroke();
    }
}

function addWeightEntry(weightKg) {
    if (!weightKg || isNaN(weightKg)) return;
    state.user.weight = parseFloat(weightKg);
    state.weights.unshift({
        id: Date.now().toString(),
        weight: parseFloat(weightKg),
        date: new Date().toISOString()
    });
    state.save();
    renderWeightView();
    showToast('Pesée de ' + weightKg + ' kg enregistrée !');
}

function deleteWeight(index) {
    state.weights.splice(index, 1);
    if (state.weights.length > 0) {
        state.user.weight = state.weights[0].weight;
    }
    state.save();
    renderWeightView();
}

function renderHistoryView() {
    const list = document.getElementById('historyList');
    if (!list) return;
    list.innerHTML = '';

    const totalFasts = state.sessions.length;
    const completedFasts = state.sessions.filter(s => s.completed).length;
    let totalMs = 0;
    state.sessions.forEach(s => totalMs += (s.durationMillis || 0));

    const cntEl = document.getElementById('histTotalCount');
    const compEl = document.getElementById('histCompletedCount');
    const hrsEl = document.getElementById('histTotalHours');
    const rateEl = document.getElementById('histSuccessRate');

    if (cntEl) cntEl.textContent = totalFasts;
    if (compEl) compEl.textContent = completedFasts;
    if (hrsEl) hrsEl.textContent = Math.floor(totalMs / 3600000) + 'h';
    if (rateEl) rateEl.textContent = totalFasts > 0 ? Math.floor((completedFasts / totalFasts) * 100) + '%' : '0%';

    if (state.sessions.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding: 30px; color: var(--text-dim); font-size: 14px;">Aucun jeûne enregistré pour l\'instant. Commencez votre premier jeûne sur l\'écran Accueil !</div>';
        return;
    }

    state.sessions.forEach((s) => {
        const durHrs = Math.floor(s.durationMillis / 3600000);
        const durMins = Math.floor((s.durationMillis % 3600000) / 60000);
        const dateStr = new Date(s.startTime).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });

        const card = document.createElement('div');
        card.className = 'session-card';
        card.innerHTML = '<div><div class="session-plan">Plan ' + (s.planName || '16:8') + '</div><div class="session-dates">' + dateStr + '</div></div><div class="session-right"><div class="session-duration">' + durHrs + 'h ' + durMins + 'm</div><div class="session-status ' + (s.completed ? '' : 'interrupted') + '">' + (s.completed ? '✓ Complété' : 'Interrompu') + '</div></div>';
        list.appendChild(card);
    });
}

function openStageDetail() {
    const elapsedHours = state.fasting.isActive ? (Date.now() - state.fasting.startTime) / 3600000 : 0;
    let currentStage = STAGES[0];
    for (let i = STAGES.length - 1; i >= 0; i--) {
        if (elapsedHours >= STAGES[i].startHour) {
            currentStage = STAGES[i];
            break;
        }
    }

    document.getElementById('modalStageEmoji').textContent = currentStage.emoji;
    document.getElementById('modalStageName').textContent = 'Étape ' + currentStage.id + ' : ' + currentStage.name + ' (' + currentStage.startHour + 'h - ' + currentStage.endHour + 'h)';
    document.getElementById('modalStageDesc').textContent = currentStage.fullDesc;
    
    const effectsList = document.getElementById('modalStageEffects');
    effectsList.innerHTML = currentStage.effects.map(e => '<li>' + e + '</li>').join('');
    
    document.getElementById('modalStageTip').textContent = currentStage.tip;

    document.getElementById('stageModal').classList.add('open');
}

function openTimeAdjustModal() {
    if (!state.fasting.isActive) {
        showToast('Aucun jeûne en cours à ajuster.');
        return;
    }
    const startDate = new Date(state.fasting.startTime);
    const timeInput = document.getElementById('adjustTimeInput');
    const hrs = String(startDate.getHours()).padStart(2, '0');
    const mins = String(startDate.getMinutes()).padStart(2, '0');
    timeInput.value = hrs + ':' + mins;
    document.getElementById('timeModal').classList.add('open');
}

function saveAdjustedTime() {
    const timeInput = document.getElementById('adjustTimeInput').value;
    if (!timeInput) return;
    const parts = timeInput.split(':').map(Number);
    const h = parts[0];
    const m = parts[1];
    const d = new Date(state.fasting.startTime || Date.now());
    d.setHours(h, m, 0, 0);

    if (d.getTime() > Date.now()) {
        d.setDate(d.getDate() - 1);
    }

    state.fasting.startTime = d.getTime();
    state.save();
    closeModal('timeModal');
    updateTimer();
    showToast('Heure de début ajustée !');
}

function showToast(msg) {
    const toast = document.getElementById('toastBanner');
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove('open');
}

function exportDataBackup() {
    const backupObj = {
        exportDate: new Date().toISOString(),
        version: '1.6-pwa',
        user: state.user,
        fasting: state.fasting,
        sessions: state.sessions,
        weights: state.weights
    };

    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(backupObj, null, 2));
    const dlAnchor = document.createElement('a');
    dlAnchor.setAttribute('href', dataStr);
    dlAnchor.setAttribute('download', 'fastmaster_backup_' + new Date().toISOString().slice(0,10) + '.json');
    dlAnchor.click();
    showToast('Sauvegarde exportée avec succès !');
}

function importDataBackup(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
        try {
            const data = JSON.parse(e.target.result);
            if (data.user) state.user = { ...state.user, ...data.user };
            if (data.sessions) state.sessions = data.sessions;
            if (data.weights) state.weights = data.weights;
            state.save();
            updateTimer();
            renderPlans();
            renderWeightView();
            renderHistoryView();
            showToast('Données restaurées avec succès !');
        } catch (err) {
            showToast('Fichier de sauvegarde invalide.');
        }
    };
    reader.readAsText(file);
}

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    const banner = document.getElementById('pwaInstallBanner');
    if (banner) banner.classList.add('show');
});

function installPWA() {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    deferredPrompt.userChoice.then((choice) => {
        if (choice.outcome === 'accepted') {
            showToast('Installation en cours...');
        }
        deferredPrompt = null;
        const banner = document.getElementById('pwaInstallBanner');
        if (banner) banner.classList.remove('show');
    });
}

// --- Google Cloud & Drive Sync Engine ---
const GOOGLE_CLIENT_ID = '511788825944-lq3emrev9q9eeoir22nfpoc51655oech.apps.googleusercontent.com';
const GOOGLE_SCOPES = 'https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile';
const BACKUP_FILE_NAME = 'fastmaster_backup.json';

class GoogleDriveSync {
    constructor() {
        this.tokenClient = null;
        this.auth = this.loadAuth();
        this.autoSyncTimer = null;
    }

    loadAuth() {
        const saved = localStorage.getItem('fastmaster_google_auth');
        return saved ? JSON.parse(saved) : {
            accessToken: null,
            expiresAt: 0,
            email: null,
            name: null,
            picture: null,
            lastSyncTime: null,
            autoSync: true
        };
    }

    saveAuth() {
        localStorage.setItem('fastmaster_google_auth', JSON.stringify(this.auth));
        this.updateUI();
    }

    isConnected() {
        return !!(this.auth.accessToken && this.auth.expiresAt > Date.now());
    }

    init() {
        if (window.google && window.google.accounts && window.google.accounts.oauth2) {
            try {
                this.tokenClient = google.accounts.oauth2.initTokenClient({
                    client_id: GOOGLE_CLIENT_ID,
                    scope: GOOGLE_SCOPES,
                    callback: (resp) => this.handleTokenResponse(resp)
                });
            } catch (e) {
                console.warn('Google TokenClient init failed:', e);
            }
        }
        this.updateUI();

        if (this.isConnected() && this.auth.autoSync !== false) {
            setTimeout(() => this.downloadBackup(false), 2000);
        }
    }

    signIn() {
        if (!this.tokenClient) {
            if (window.google && window.google.accounts && window.google.accounts.oauth2) {
                this.init();
            } else {
                showToast('Google Identity Services en cours de chargement...');
                return;
            }
        }
        if (this.tokenClient) {
            this.tokenClient.requestAccessToken({ prompt: 'consent' });
        }
    }

    signOut() {
        if (this.auth.accessToken && window.google && window.google.accounts && window.google.accounts.oauth2) {
            try {
                google.accounts.oauth2.revoke(this.auth.accessToken, () => {});
            } catch (e) {}
        }
        this.auth = {
            accessToken: null,
            expiresAt: 0,
            email: null,
            name: null,
            picture: null,
            lastSyncTime: null,
            autoSync: true
        };
        this.saveAuth();
        showToast('Déconnecté de Google Cloud.');
    }

    async handleTokenResponse(resp) {
        if (resp.error) {
            console.error('Google Auth Error:', resp);
            showToast('Erreur de connexion Google.');
            return;
        }

        this.auth.accessToken = resp.access_token;
        this.auth.expiresAt = Date.now() + ((resp.expires_in || 3600) * 1000) - 60000;

        try {
            const userResp = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
                headers: { Authorization: 'Bearer ' + this.auth.accessToken }
            });
            if (userResp.ok) {
                const info = await userResp.json();
                this.auth.email = info.email;
                this.auth.name = info.name;
                this.auth.picture = info.picture;
            }
        } catch (e) {
            console.warn('Could not fetch user profile:', e);
        }

        this.saveAuth();
        showToast('Connecté avec succès : ' + (this.auth.email || 'Google Drive'));
        
        // Immediate sync
        this.uploadBackup(false);
    }

    async searchBackupFile() {
        const query = encodeURIComponent("name = '" + BACKUP_FILE_NAME + "' and trashed = false");
        const url = 'https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=' + query + '&fields=files(id,name,modifiedTime)';
        
        const resp = await fetch(url, {
            headers: { Authorization: 'Bearer ' + this.auth.accessToken }
        });

        if (resp.status === 401) {
            this.auth.accessToken = null;
            this.saveAuth();
            throw new Error('Token expiré');
        }

        const data = await resp.json();
        return (data.files && data.files.length > 0) ? data.files[0] : null;
    }

    async uploadBackup(isManual = false) {
        if (!this.isConnected()) {
            if (isManual) showToast('Veuillez vous connecter avec Google.');
            return false;
        }

        try {
            const payload = {
                exportDate: new Date().toISOString(),
                version: '1.6',
                user: state.user,
                fasting: state.fasting,
                sessions: state.sessions,
                weights: state.weights
            };

            const jsonStr = JSON.stringify(payload);
            const file = await this.searchBackupFile();

            if (file) {
                const updateUrl = 'https://www.googleapis.com/upload/drive/v3/files/' + file.id + '?uploadType=media';
                const updateResp = await fetch(updateUrl, {
                    method: 'PATCH',
                    headers: {
                        Authorization: 'Bearer ' + this.auth.accessToken,
                        'Content-Type': 'application/json'
                    },
                    body: jsonStr
                });

                if (!updateResp.ok) throw new Error('Échec mise à jour fichier');
            } else {
                const metadata = {
                    name: BACKUP_FILE_NAME,
                    parents: ['appDataFolder']
                };

                const boundary = '-------314159265358979323846';
                const delimiter = '\r\n--' + boundary + '\r\n';
                const close_delim = '\r\n--' + boundary + '--';

                const multipartRequestBody =
                    delimiter +
                    'Content-Type: application/json; charset=UTF-8\r\n\r\n' +
                    JSON.stringify(metadata) +
                    delimiter +
                    'Content-Type: application/json\r\n\r\n' +
                    jsonStr +
                    close_delim;

                const createResp = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart', {
                    method: 'POST',
                    headers: {
                        Authorization: 'Bearer ' + this.auth.accessToken,
                        'Content-Type': 'multipart/related; boundary="' + boundary + '"'
                    },
                    body: multipartRequestBody
                });

                if (!createResp.ok) throw new Error('Échec création fichier');
            }

            this.auth.lastSyncTime = new Date().toISOString();
            this.saveAuth();

            if (isManual) {
                showToast('☁️ Données sauvegardées sur Google Drive !');
            }
            return true;
        } catch (e) {
            console.error('Upload backup failed:', e);
            if (isManual) showToast('Erreur lors de la sauvegarde Google Drive.');
            return false;
        }
    }

    async downloadBackup(isManual = false) {
        if (!this.isConnected()) {
            if (isManual) showToast('Veuillez vous connecter avec Google.');
            return false;
        }

        try {
            const file = await this.searchBackupFile();
            if (!file) {
                if (isManual) showToast('Aucune sauvegarde trouvée sur Google Drive.');
                return false;
            }

            const downloadUrl = 'https://www.googleapis.com/drive/v3/files/' + file.id + '?alt=media';
            const resp = await fetch(downloadUrl, {
                headers: { Authorization: 'Bearer ' + this.auth.accessToken }
            });

            if (!resp.ok) throw new Error('Échec téléchargement');

            const cloudData = await resp.json();
            
            if (cloudData.user) state.user = { ...state.user, ...cloudData.user };
            if (cloudData.fasting) state.fasting = { ...state.fasting, ...cloudData.fasting };
            if (cloudData.sessions) state.sessions = cloudData.sessions;
            if (cloudData.weights) state.weights = cloudData.weights;
            
            state.save();
            updateTimer();
            renderPlans();
            renderWeightView();
            renderHistoryView();

            this.auth.lastSyncTime = new Date().toISOString();
            this.saveAuth();

            showToast('📥 Données restaurées depuis Google Drive !');
            return true;
        } catch (e) {
            console.error('Download backup error:', e);
            if (isManual) showToast('Erreur lors de la restauration Google Drive.');
            return false;
        }
    }

    updateUI() {
        const icon = document.getElementById('googleStatusIcon');
        const emailEl = document.getElementById('googleAccountEmail');
        const timeEl = document.getElementById('googleSyncTime');
        const btnAuth = document.getElementById('btnGoogleAuth');
        const driveActions = document.getElementById('googleDriveActions');
        const autoSyncSwitch = document.getElementById('switchGoogleAutoSync');

        if (!emailEl) return;

        if (this.isConnected()) {
            if (icon) {
                icon.style.background = '#10B981';
                icon.style.boxShadow = '0 0 10px #10B981';
            }
            emailEl.textContent = 'Connecté : ' + (this.auth.email || 'Compte Google');
            if (this.auth.lastSyncTime) {
                const date = new Date(this.auth.lastSyncTime);
                const timeStr = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
                timeEl.textContent = 'Dernière synchro : Aujourd\'hui à ' + timeStr;
            } else {
                timeEl.textContent = 'Synchronisation automatique active';
            }

            if (btnAuth) {
                btnAuth.innerHTML = '<span>Se déconnecter de Google</span>';
                btnAuth.style.background = 'rgba(239, 68, 68, 0.15)';
                btnAuth.style.color = '#EF4444';
                btnAuth.style.border = '1px solid rgba(239, 68, 68, 0.3)';
                btnAuth.style.boxShadow = 'none';
                btnAuth.onclick = () => this.signOut();
            }

            if (driveActions) driveActions.style.display = 'grid';
        } else {
            if (icon) {
                icon.style.background = '#64748B';
                icon.style.boxShadow = 'none';
            }
            emailEl.textContent = 'Non connecté';
            timeEl.textContent = 'Connectez-vous pour synchroniser vos données';

            if (btnAuth) {
                btnAuth.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" fill="#EA4335"/></svg><span style="color: white; font-weight: 700;">Se connecter avec Google</span>';
                btnAuth.style.background = '#4285F4';
                btnAuth.style.color = '#FFFFFF';
                btnAuth.style.border = 'none';
                btnAuth.style.boxShadow = '0 4px 15px rgba(66, 133, 244, 0.4)';
                btnAuth.onclick = () => this.signIn();
            }

            if (driveActions) driveActions.style.display = 'none';
        }

        if (autoSyncSwitch) {
            autoSyncSwitch.checked = this.auth.autoSync !== false;
        }
    }
}

const googleSync = new GoogleDriveSync();

function handleGoogleAuth() {
    if (googleSync.isConnected()) {
        googleSync.signOut();
    } else {
        googleSync.signIn();
    }
}

function syncUploadToGoogleDrive(isManual = true) {
    googleSync.uploadBackup(isManual);
}

function syncDownloadFromGoogleDrive(isManual = true) {
    googleSync.downloadBackup(isManual);
}

function triggerAutoSync() {
    if (!googleSync.isConnected() || googleSync.auth.autoSync === false) return;
    clearTimeout(googleSync.autoSyncTimer);
    googleSync.autoSyncTimer = setTimeout(() => {
        googleSync.uploadBackup(false);
    }, 2000);
}

document.addEventListener('DOMContentLoaded', () => {
    if (!state.user.darkMode) {
        document.body.classList.add('light-theme');
    }

    const nameInput = document.getElementById('inputUserName');
    const heightInput = document.getElementById('inputUserHeight');
    const greetingEl = document.getElementById('greetingName');

    if (nameInput) nameInput.value = state.user.name || 'Jeûneur';
    if (heightInput) heightInput.value = state.user.height || 175;
    if (greetingEl) greetingEl.textContent = 'Bonjour, ' + (state.user.name || 'Jeûneur') + ' 👋';

    const switchDark = document.getElementById('switchDarkMode');
    const switchSnd = document.getElementById('switchSound');
    const switchAuto = document.getElementById('switchAutoStart');
    const switchGoogleSync = document.getElementById('switchGoogleAutoSync');

    if (switchDark) switchDark.checked = state.user.darkMode;
    if (switchSnd) switchSnd.checked = state.user.sound;
    if (switchAuto) switchAuto.checked = state.user.autoStart;
    if (switchGoogleSync) {
        switchGoogleSync.checked = googleSync.auth.autoSync !== false;
        switchGoogleSync.addEventListener('change', (e) => {
            googleSync.auth.autoSync = e.target.checked;
            googleSync.saveAuth();
            showToast(e.target.checked ? 'Synchro automatique activée' : 'Synchro automatique désactivée');
        });
    }

    if (nameInput) {
        nameInput.addEventListener('change', (e) => {
            state.user.name = e.target.value;
            state.save();
            if (greetingEl) greetingEl.textContent = 'Bonjour, ' + state.user.name + ' 👋';
        });
    }

    if (heightInput) {
        heightInput.addEventListener('change', (e) => {
            state.user.height = parseInt(e.target.value) || 175;
            state.save();
            renderWeightView();
        });
    }

    if (switchDark) {
        switchDark.addEventListener('change', (e) => {
            state.user.darkMode = e.target.checked;
            state.save();
            document.body.classList.toggle('light-theme', !state.user.darkMode);
        });
    }

    if (switchSnd) {
        switchSnd.addEventListener('change', (e) => {
            state.user.sound = e.target.checked;
            state.save();
        });
    }

    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }

    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('./sw.js')
            .then(reg => console.log('FastMaster ServiceWorker enregistré:', reg.scope))
            .catch(err => console.error('Erreur ServiceWorker:', err));
    }

    // Initialize Google Drive Sync
    setTimeout(() => {
        googleSync.init();
    }, 1000);

    renderPlans();
    updateTimer();
    setInterval(updateTimer, 1000);
});