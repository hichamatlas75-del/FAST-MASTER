# ⏳ Fast Master - Application PWA de Jeûne Intermittent

[![PWA](https://img.shields.io/badge/PWA-Progressive_Web_App-orange.svg)](https://hichamatlas75-del.github.io/FAST-MASTER/)
[![Google Cloud Sync](https://img.shields.io/badge/Google_Drive-Auto_Sync-blue.svg)](https://hichamatlas75-del.github.io/FAST-MASTER/)
[![Offline](https://img.shields.io/badge/Offline-100%25_Supported-green.svg)](https://hichamatlas75-del.github.io/FAST-MASTER/)

Fast Master est une **Progressive Web App (PWA)** moderne, fluide et complète dédiée au suivi du jeûne intermittent et à la compréhension des phases métaboliques du corps humain.

---

## 🚀 Fonctionnalités Clés

- ⏱️ **Minuteur Circulaire SVG en Temps Réel :** Affichage interactif des heures, minutes, secondes et pourcentage d'avancement.
- 🔬 **6 Étapes Métaboliques & Biologiques :**
  1. *Digestion (0h - 4h)*
  2. *Baisse de l'insuline (4h - 8h)*
  3. *Combustion des graisses (8h - 12h)*
  4. *Cétose & Clarté mentale (12h - 18h)*
  5. *Autophagie & Recyclage cellulaire (18h - 24h)*
  6. *Régénération profonde des cellules souches (24h+)*
- 🎯 **5 Protocoles de Jeûne :** 16:8, 18:6, 20:4 (Warrior), OMAD (23:1) et 16:8 Flexible.
- ⚖️ **Suivi du Poids & IMC Dynamique :** Graphique d'évolution interactif en courbe (Canvas) et jauge IMC.
- 📊 **Historique & Statistiques :** Compteur de jeûnes réussis, total d'heures et taux de succès.
- ☁️ **Synchronisation Automatique Google Cloud & Google Drive :** Sauvegarde automatique des données dans `appDataFolder` en arrière-plan avec OAuth 2.0.
- 📲 **100% Hors-ligne & Installable :** Fonctionne partout grâce au Service Worker et au manifeste PWA.

---

## 📂 Structure des Fichiers PWA

```text
├── index.html        # Interface de l'application PWA (5 onglets, minuteur, modals)
├── style.css         # Thèmes Sombre OLED / Clair & design mobile-first
├── app.js            # Moteur JavaScript, calculs métaboliques & Cloud Sync
├── sw.js             # Service Worker pour le fonctionnement hors-ligne
├── manifest.json     # Manifeste Web PWA pour installation native
├── icons/            # Icônes de l'application (192x192, 512x512, favicon)
├── privacy.html      # Politique de confidentialité
└── README.md         # Documentation du projet
```

---

## 🌐 Déploiement sur GitHub Pages

1. Rendez-vous dans **Settings** > **Pages** de votre dépôt GitHub.
2. Sous **Build and deployment** > **Branch**, sélectionnez la branche `main` et le dossier `/ (root)`.
3. Cliquez sur **Save**.
4. Votre PWA sera accessible en ligne sur `https://hichamatlas75-del.github.io/FAST-MASTER/` !