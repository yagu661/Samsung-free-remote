/**
 * Samsung Smart TV AI Entertainment Hub - Controller
 * Target: Samsung UA32T4410AKXXL (Tizen 5.5 / 5.0 HD)
 * Built with love by Soumya for complete TV entertainment & AI companion control
 */

(function () {
    'use strict';

    // DOM Elements
    var topClockEl = document.getElementById('topTvClock');
    var navTabs = Array.prototype.slice.call(document.querySelectorAll('.nav-tab'));
    var tabViews = Array.prototype.slice.call(document.querySelectorAll('.tab-view'));
    var assistantHudOverlay = document.getElementById('assistantHudOverlay');
    var hudQueryText = document.getElementById('hudQueryText');
    var hudResponseText = document.getElementById('hudResponseText');
    var hudActionPill = document.getElementById('hudActionPill');
    var tvVolumeToast = document.getElementById('tvVolumeToast');
    var volBarFill = document.getElementById('volBarFill');
    var volText = document.getElementById('volText');
    var tvRemoteBadge = document.getElementById('tvRemoteBadge');
    var globalTvSearchInput = document.getElementById('globalTvSearchInput');
    var dedicatedSearchInput = document.getElementById('dedicatedSearchInput');
    var assistantChatHistory = document.getElementById('assistantChatHistory');
    var assistantDirectInput = document.getElementById('assistantDirectInput');
    var btnTraySend = document.getElementById('btnTraySend');
    var btnTrayVoice = document.getElementById('btnTrayVoice');
    var headerMicBtn = document.getElementById('headerMicBtn');
    var heroStatusPill = document.getElementById('heroStatusPill');
    var topCompanionText = document.getElementById('topCompanionText');
    var btnSearchYouTube = document.getElementById('btnSearchYouTube');
    var btnSearchSpotify = document.getElementById('btnSearchSpotify');
    var btnSearchAi = document.getElementById('btnSearchAi');
    var settingPhoneIpInput = document.getElementById('settingPhoneIpInput');
    var btnSavePhoneIp = document.getElementById('btnSavePhoneIp');
    var diagCompanionStatus = document.getElementById('diagCompanionStatus');

    // YouTube View Elements
    var ytCurrentTitle = document.getElementById('ytCurrentTitle');
    var ytSearchQueryLabel = document.getElementById('ytSearchQueryLabel');
    var ytSearchBox = document.getElementById('ytSearchBox');
    var ytBtnSearchSubmit = document.getElementById('ytBtnSearchSubmit');
    var ytBtnCinema = document.getElementById('ytBtnCinema');
    var ytFrameWrapper = document.getElementById('ytFrameWrapper');
    var embeddedYoutubeFrame = document.getElementById('embeddedYoutubeFrame');
    var cinemaExitHint = document.getElementById('cinemaExitHint');

    // Spotify View Elements
    var spotifyPlaylistLabel = document.getElementById('spotifyPlaylistLabel');
    var embeddedSpotifyFrame = document.getElementById('embeddedSpotifyFrame');
    var spotifyFrameWrapper = document.getElementById('spotifyFrameWrapper');

    var currentVolume = 25;
    var hudTimer = null;
    var volTimer = null;
    var badgeTimer = null;
    var currentActiveTab = 'home';
    var lastProcessedMessageId = 0;
    var installedAppMap = {};
    var isCinemaMode = false;

    // Detect Companion Phone Host IP from current URL or localStorage
    var companionPhoneIp = "192.168.1.15";
    try {
        var savedIp = localStorage.getItem('companion_phone_ip');
        if (savedIp && savedIp.trim()) {
            companionPhoneIp = savedIp.trim();
        } else if (window.location.hostname && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1' && window.location.hostname.indexOf('192.168.') !== -1) {
            companionPhoneIp = window.location.hostname;
            localStorage.setItem('companion_phone_ip', companionPhoneIp);
        }
    } catch (_) {}

    if (settingPhoneIpInput) {
        settingPhoneIpInput.value = companionPhoneIp;
    }

    if (btnSavePhoneIp) {
        btnSavePhoneIp.addEventListener('click', function () {
            if (settingPhoneIpInput && settingPhoneIpInput.value.trim()) {
                companionPhoneIp = settingPhoneIpInput.value.trim();
                try {
                    localStorage.setItem('companion_phone_ip', companionPhoneIp);
                } catch (_) {}
                showRemoteBadge("PHONE IP SAVED");
                showAssistantHud("Phone IP: " + companionPhoneIp, "CONFIG SAVED", "Linked to Galaxy M30s at " + companionPhoneIp, false);
                syncCompanion();
            }
        });
    }

    // 1. Digital Clock
    function updateClock() {
        var now = new Date();
        var hours = now.getHours();
        var minutes = now.getMinutes();
        var ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        var minStr = minutes < 10 ? '0' + minutes : minutes;
        if (topClockEl) {
            topClockEl.textContent = hours + ':' + minStr + ' ' + ampm;
        }
    }
    setInterval(updateClock, 1000);
    updateClock();

    // 2. Tab Navigation
    function switchTab(tabId) {
        if (!tabId) return;
        currentActiveTab = tabId;

        // If switching away from cinema mode, restore standard view
        if (isCinemaMode && tabId !== 'youtube') {
            exitCinemaMode();
        }

        navTabs.forEach(function (tab) {
            if (tab.getAttribute('data-tab') === tabId) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        tabViews.forEach(function (view) {
            var targetId = 'view' + tabId.charAt(0).toUpperCase() + tabId.slice(1);
            if (view.id === targetId) {
                view.classList.add('active');
            } else {
                view.classList.remove('active');
            }
        });

        // Lazy-load players on first tab entry so iframes never block initial page load or steal focus
        if (tabId === 'youtube') {
            if (embeddedYoutubeFrame && (!embeddedYoutubeFrame.src || embeddedYoutubeFrame.src === 'about:blank' || embeddedYoutubeFrame.src === window.location.href)) {
                loadEmbeddedYouTube("Bengali songs");
            }
        } else if (tabId === 'spotify') {
            if (embeddedSpotifyFrame && (!embeddedSpotifyFrame.src || embeddedSpotifyFrame.src === 'about:blank' || embeddedSpotifyFrame.src === window.location.href)) {
                loadEmbeddedSpotify('playlist/37i9dQZF1DX0XUfTFmZeMt', "Today's Top Hits");
            }
        }

        // Auto focus first interactive item in new tab
        setTimeout(function () {
            var firstItem = document.querySelector('.tab-view.active .media-chip') ||
                            document.querySelector('.tab-view.active .prompt-chip') ||
                            document.querySelector('.tab-view.active .yt-video-card') ||
                            document.querySelector('.tab-view.active input') ||
                            document.querySelector('.tab-view.active button');
            if (firstItem) {
                firstItem.focus();
            }
        }, 100);
    }

    // 3. Remote Button Feedback Toast
    function showRemoteBadge(text) {
        if (!tvRemoteBadge) return;
        tvRemoteBadge.textContent = text;
        tvRemoteBadge.classList.add('visible');
        if (badgeTimer) clearTimeout(badgeTimer);
        badgeTimer = setTimeout(function () {
            tvRemoteBadge.classList.remove('visible');
        }, 1200);
    }

    // 4. Volume Adjustment (for In-App Feedback & UI HUD)
    function adjustVolume(delta) {
        currentVolume = Math.min(100, Math.max(0, currentVolume + delta));
        if (volBarFill) volBarFill.style.width = currentVolume + '%';
        if (volText) volText.textContent = currentVolume + '%';
        if (tvVolumeToast) {
            tvVolumeToast.classList.add('visible');
            if (volTimer) clearTimeout(volTimer);
            volTimer = setTimeout(function () {
                tvVolumeToast.classList.remove('visible');
            }, 1800);
        }
    }

    function toggleMute() {
        if (tvVolumeToast) {
            tvVolumeToast.classList.toggle('visible');
            if (volText) volText.textContent = "MUTE";
        }
    }

    // 5. Voice Text-to-Speech (TTS) on TV Speakers
    function speakOnTv(text) {
        if (!text || !text.trim()) return;
        var cleanText = text.replace(/[*_#`[\]()]/g, ' ').replace(/\s+/g, ' ').trim();
        try {
            if (window.speechSynthesis) {
                window.speechSynthesis.cancel();
                var utterance = new SpeechSynthesisUtterance(cleanText);
                utterance.rate = 1.0;
                utterance.pitch = 1.0;
                utterance.volume = 1.0;
                window.speechSynthesis.speak(utterance);
            }
        } catch (ex) {
            console.log("TTS playback skipped on device:", ex);
        }
    }

    // 6. Floating Assistant HUD (NON-DISRUPTIVE POPUP OVER CURRENT TAB)
    function showAssistantHud(query, actionPill, responseText, speakAloud) {
        if (!assistantHudOverlay) return;

        if (hudQueryText) hudQueryText.textContent = '"' + query + '"';
        if (hudActionPill) hudActionPill.textContent = actionPill || 'AI VOICE PRO';
        if (hudResponseText) hudResponseText.textContent = responseText || 'Executing command...';

        assistantHudOverlay.classList.remove('hidden');

        if (speakAloud && responseText) {
            speakOnTv(responseText);
        }

        if (hudTimer) clearTimeout(hudTimer);
        hudTimer = setTimeout(function () {
            hideAssistantHud();
        }, 5500);
    }

    function hideAssistantHud() {
        if (!assistantHudOverlay) return;
        assistantHudOverlay.classList.add('hidden');
        if (hudTimer) {
            clearTimeout(hudTimer);
            hudTimer = null;
        }
    }

    // 7. REAL IN-APP YOUTUBE TV CONTROLLER
    function loadEmbeddedYouTube(query) {
        var cleanQuery = (query || "Bengali songs").trim();
        showRemoteBadge("YOUTUBE SEARCH");

        if (currentActiveTab !== 'youtube') {
            switchTab('youtube');
        }

        if (ytCurrentTitle) ytCurrentTitle.textContent = "YouTube: " + cleanQuery;
        if (ytSearchQueryLabel) ytSearchQueryLabel.textContent = "Currently Playing: " + cleanQuery;
        if (ytSearchBox) ytSearchBox.value = cleanQuery;

        var embedUrl = "https://www.youtube-nocookie.com/embed?listType=search&list=" + encodeURIComponent(cleanQuery) + "&autoplay=1&enablejsapi=1";
        if (embeddedYoutubeFrame) {
            embeddedYoutubeFrame.src = embedUrl;
        }

        showAssistantHud(
            cleanQuery,
            "YOUTUBE VOICE PRO",
            "Playing " + cleanQuery + " on in-app YouTube TV",
            true
        );
    }

    function toggleCinemaMode() {
        if (!ytFrameWrapper) return;
        isCinemaMode = !isCinemaMode;
        if (isCinemaMode) {
            ytFrameWrapper.classList.add('cinema-fullscreen');
            showRemoteBadge("CINEMA FULLSCREEN");
            showAssistantHud("Cinema Fullscreen", "DISPLAY", "Press RETURN/BACK on TV Remote to exit Fullscreen", false);
        } else {
            ytFrameWrapper.classList.remove('cinema-fullscreen');
            showRemoteBadge("WINDOW VIEW");
        }
    }

    function exitCinemaMode() {
        if (ytFrameWrapper && isCinemaMode) {
            isCinemaMode = false;
            ytFrameWrapper.classList.remove('cinema-fullscreen');
            showRemoteBadge("WINDOW VIEW");
        }
    }

    // 8. REAL IN-APP SPOTIFY CONTROLLER
    function loadEmbeddedSpotify(playlistUri, title) {
        var cleanUri = (playlistUri || "playlist/37i9dQZF1DX0XUfTFmZeMt").trim();
        var displayTitle = title || "Today's Top Hits";
        showRemoteBadge("SPOTIFY PLAY");

        if (currentActiveTab !== 'spotify') {
            switchTab('spotify');
        }

        if (spotifyPlaylistLabel) spotifyPlaylistLabel.textContent = "Current: " + displayTitle;

        var embedUrl = "https://open.spotify.com/embed/" + cleanUri + "?utm_source=generator&theme=0&autoplay=1";
        if (embeddedSpotifyFrame) {
            embeddedSpotifyFrame.src = embedUrl;
        }

        showAssistantHud(
            displayTitle,
            "SPOTIFY VOICE PRO",
            "Playing " + displayTitle + " on Spotify TV",
            true
        );
    }

    // 9. INSTALLED APPLICATION DISPATCHER (For Netflix, Prime Video, Browser)
    function launchApp(appId, appName) {
        showRemoteBadge("LAUNCHING " + appName.toUpperCase());
        showAssistantHud("Open " + appName, "APP LAUNCH", "Starting " + appName + " on your Samsung Smart TV...", true);

        if (typeof tizen !== 'undefined' && tizen.application) {
            try {
                tizen.application.launch(appId, function () {
                    showAssistantHud(appName + " Active", "SUCCESS", appName + " is running on your TV.", false);
                }, function (err) {
                    console.warn("Failed launching app ID:", appId, err);
                    showAssistantHud(appName + " Launch", "NOTICE", "Opening " + appName + " in Smart TV browser...", false);
                    if (appName.toLowerCase() === 'netflix') {
                        launchInBrowser("https://www.netflix.com");
                    } else if (appName.toLowerCase().indexOf('prime') !== -1) {
                        launchInBrowser("https://www.primevideo.com");
                    }
                });
            } catch (ex) {
                console.warn("App launch exception:", ex);
            }
        }
    }

    function launchInBrowser(url) {
        if (typeof tizen !== 'undefined' && tizen.application) {
            try {
                var appControl = new tizen.ApplicationControl(
                    "http://tizen.org/appcontrol/operation/view",
                    url, null, null, null
                );
                tizen.application.launchAppControl(appControl, null, null, null);
            } catch (_) {}
        }
    }

    // 10. LOCAL COMPANION HEARTBEAT & ASSISTANT SYNC (Galaxy M30s <-> Smart TV)
    function syncCompanion() {
        var host = companionPhoneIp;
        if (!host) return;

        // 1. Send Heartbeat to Phone
        try {
            var hbXhr = new XMLHttpRequest();
            hbXhr.open("POST", "http://" + host + ":8080/api/tv/heartbeat", true);
            hbXhr.setRequestHeader("Content-Type", "application/json");
            hbXhr.timeout = 2000;
            hbXhr.onreadystatechange = function () {
                if (hbXhr.readyState === 4 && hbXhr.status === 200) {
                    if (topCompanionText) topCompanionText.textContent = "Galaxy M30s Linked";
                    if (diagCompanionStatus) diagCompanionStatus.textContent = "Linked (" + host + ":8080)";
                }
            };
            hbXhr.send(JSON.stringify({ tvModel: "UA32T4410", status: "online" }));
        } catch (_) {}

        // 2. Poll Latest Command from Phone (Instant Pop-up without changing screen tab!)
        try {
            var pollXhr = new XMLHttpRequest();
            pollXhr.open("GET", "http://" + host + ":8080/api/assistant/latest", true);
            pollXhr.timeout = 2000;
            pollXhr.onreadystatechange = function () {
                if (pollXhr.readyState === 4 && pollXhr.status === 200) {
                    try {
                        var data = JSON.parse(pollXhr.responseText);
                        if (data && data.id && data.id !== lastProcessedMessageId) {
                            lastProcessedMessageId = data.id;
                            handleCompanionCommand(data);
                        }
                    } catch (_) {}
                }
            };
            pollXhr.send();
        } catch (_) {}
    }

    function handleCompanionCommand(cmd) {
        console.log("Received command from Galaxy M30s:", cmd);

        // CASE 1: Remote Key from Phone App (DUAL DISPATCH: Guarantees phone remote works even without WebSocket!)
        if (cmd.actionType === "REMOTE_KEY") {
            handleRemoteKeyCommand(cmd.actionTarget);
            return;
        }

        // CASE 2: Phone started listening -> Instant Pop-up HUD without leaving current tab!
        if (cmd.actionType === "LISTENING") {
            showAssistantHud(cmd.query || "Listening...", "VOICE LISTENER", cmd.response || "Speak into your Galaxy M30s microphone...", false);
            return;
        }

        // CASE 3: Phone is processing/thinking
        if (cmd.actionType === "THINKING") {
            showAssistantHud(cmd.query || "Processing...", "THINKING", cmd.response || "Analyzing your command...", false);
            return;
        }

        // CASE 4: Voice search for YouTube
        if (cmd.actionType === "SEARCH_YOUTUBE") {
            var ytQuery = cmd.actionTarget || cmd.query || "Bengali songs";
            loadEmbeddedYouTube(ytQuery);
            return;
        }

        // CASE 5: Launch In-App or Native App
        if (cmd.actionType === "LAUNCH_APP") {
            var target = (cmd.actionTarget || "").toLowerCase();
            if (target === "youtube" || target === "111299001912") {
                loadEmbeddedYouTube("Bengali songs");
                return;
            } else if (target === "spotify" || target === "3201606009684") {
                loadEmbeddedSpotify('playlist/37i9dQZF1DX0XUfTFmZeMt', "Today's Top Hits");
                return;
            } else if (target === "netflix" || target === "11101200001") {
                launchApp('11101200001', 'Netflix');
                return;
            } else if (target === "prime" || target === "3201512006785") {
                launchApp('3201512006785', 'Prime Video');
                return;
            }
        }

        // CASE 6: Volume Adjustment
        if (cmd.actionType === "VOLUME") {
            var delta = parseInt(cmd.actionTarget, 10) || 3;
            adjustVolume(delta);
            return;
        }

        // CASE 7: Tab Navigation
        if (cmd.actionType === "NAV_TAB") {
            switchTab(cmd.actionTarget || 'home');
            return;
        }

        // CASE 8: General Voice Query / AI Conversational Response
        // IMPORTANT: NEVER SWITCH AWAY TO SEPARATE ASSISTANT TAB!
        // Display floating HUD popup directly over whatever the user is watching (YouTube, Spotify, etc.)
        showAssistantHud(
            cmd.query || "Voice Assistant",
            cmd.actionType || "AI VOICE PRO",
            cmd.response || "Command executed",
            true // Speak aloud on TV speakers!
        );
    }

    function handleRemoteKeyCommand(keyName) {
        if (!keyName) return;
        var k = keyName.toUpperCase();
        showRemoteBadge(k.replace('KEY_', ''));

        switch (k) {
            case 'KEY_UP':
                moveFocus('up');
                break;
            case 'KEY_DOWN':
                moveFocus('down');
                break;
            case 'KEY_LEFT':
                moveFocus('left');
                break;
            case 'KEY_RIGHT':
                moveFocus('right');
                break;
            case 'KEY_ENTER':
                if (document.activeElement && typeof document.activeElement.click === 'function') {
                    document.activeElement.click();
                }
                break;
            case 'KEY_RETURN':
            case 'KEY_BACK':
                handleBackKey();
                break;
            case 'KEY_HOME':
                switchTab('home');
                break;
            case 'KEY_VOLUP':
                adjustVolume(3);
                break;
            case 'KEY_VOLDOWN':
                adjustVolume(-3);
                break;
            case 'KEY_MUTE':
                toggleMute();
                break;
            case 'KEY_PLAY':
                showRemoteBadge("▶ PLAY");
                break;
            case 'KEY_PAUSE':
                showRemoteBadge("⏸ PAUSE");
                break;
            case 'KEY_YOUTUBE':
                loadEmbeddedYouTube("Bengali songs");
                break;
            case 'KEY_SPOTIFY':
                loadEmbeddedSpotify('playlist/37i9dQZF1DX0XUfTFmZeMt', "Today's Top Hits");
                break;
            default:
                break;
        }
    }

    // Poll companion every 1000ms for instantaneous reaction
    setInterval(syncCompanion, 1000);

    // 11. NATURAL LANGUAGE COMMAND DISPATCHER (For In-App Search Inputs)
    function executeVoiceCommand(spokenText) {
        if (!spokenText || !spokenText.trim()) return;
        var text = spokenText.toLowerCase().trim();

        if (text.indexOf('youtube') !== -1 || text.indexOf('video') !== -1 || text.indexOf('song') !== -1 || text.indexOf('bengali') !== -1) {
            var q = spokenText.replace(/search/gi, '').replace(/youtube/gi, '').replace(/for/gi, '').replace(/play/gi, '').trim();
            loadEmbeddedYouTube(q || "Bengali songs");
            return;
        }

        if (text.indexOf('spotify') !== -1 || text.indexOf('music') !== -1) {
            loadEmbeddedSpotify('playlist/37i9dQZF1DX0XUfTFmZeMt', "Today's Top Hits");
            return;
        }

        if (text.indexOf('cinema') !== -1 || text.indexOf('fullscreen') !== -1) {
            toggleCinemaMode();
            return;
        }

        if (text.indexOf('netflix') !== -1) {
            launchApp('11101200001', 'Netflix');
            return;
        }

        if (text.indexOf('prime') !== -1 || text.indexOf('amazon') !== -1) {
            launchApp('3201512006785', 'Prime Video');
            return;
        }

        if (text.indexOf('home') !== -1) {
            switchTab('home');
            showAssistantHud(spokenText, "GO HOME", "Returned to Home Smart Hub.", true);
            return;
        }

        if (text.indexOf('volume up') !== -1 || text.indexOf('louder') !== -1) {
            adjustVolume(3);
            showAssistantHud(spokenText, "VOLUME +3", "Volume adjusted.", true);
            return;
        }

        if (text.indexOf('volume down') !== -1 || text.indexOf('lower') !== -1) {
            adjustVolume(-3);
            showAssistantHud(spokenText, "VOLUME -3", "Volume adjusted.", true);
            return;
        }

        // General AI Conversational Query: query phone server
        showAssistantHud(spokenText, "AI ASSISTANT", "Querying assistant on Galaxy M30s...", false);
        queryOpenRouterCompanion(spokenText);
    }

    function queryOpenRouterCompanion(prompt) {
        var host = companionPhoneIp;
        var url = "http://" + host + ":8080/api/assistant/command";
        try {
            var xhr = new XMLHttpRequest();
            xhr.open("POST", url, true);
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.timeout = 5000;
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    try {
                        var res = JSON.parse(xhr.responseText);
                        if (res.response) {
                            showAssistantHud(prompt, "AI RESPONSE", res.response, true);
                        }
                    } catch (_) {}
                }
            };
            xhr.send(JSON.stringify({ query: prompt }));
        } catch (_) {}
    }

    // 12. TIZEN TV HARDWARE REMOTE KEY REGISTRATION
    function registerTizenKeys() {
        if (typeof tizen !== 'undefined' && tizen.tvinputdevice) {
            // Register ONLY non-basic media and color keys
            var keys = [
                'MediaPlay', 'MediaPause', 'MediaStop', 'MediaFastForward', 'MediaRewind',
                'ColorF0Red', 'ColorF1Green', 'ColorF2Yellow', 'ColorF3Blue',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
            ];
            keys.forEach(function (k) {
                try {
                    tizen.tvinputdevice.registerKey(k);
                } catch (e) {
                    console.warn("Key register skipped:", k);
                }
            });
        }
    }

    // 13. 2D SPATIAL NAVIGATION FOR D-PAD (Arrow Up, Down, Left, Right)
    function getFocusableElements() {
        var selectors = [
            '#tabHome', '#tabYoutube', '#tabSpotify', '#tabApps', '#tabSearch', '#tabAssistant', '#tabSettings',
            '#globalTvSearchInput',
            '#headerMicBtn',
            '.tab-view.active .media-chip',
            '.tab-view.active .yt-video-card',
            '.tab-view.active .player-action-btn',
            '.tab-view.active .yt-search-input-box input',
            '.tab-view.active .yt-search-input-box button',
            '.tab-view.active .prompt-chip',
            '.tab-view.active .feature-card',
            '.tab-view.active .app-detail-card',
            '.tab-view.active .btn-launch',
            '.tab-view.active .btn-search-shortcut',
            '.tab-view.active .btn-search-action',
            '.tab-view.active .suggest-chip',
            '.tab-view.active #dedicatedSearchInput',
            '.tab-view.active #assistantDirectInput',
            '.tab-view.active #btnTraySend',
            '.tab-view.active #btnTrayVoice',
            '.tab-view.active #settingPhoneIpInput',
            '.tab-view.active #btnSavePhoneIp',
            '.tab-view.active button',
            '.tab-view.active input'
        ];
        var list = [];
        selectors.forEach(function (sel) {
            var els = document.querySelectorAll(sel);
            for (var i = 0; i < els.length; i++) {
                var el = els[i];
                if (el.offsetParent !== null && !el.disabled && list.indexOf(el) === -1) {
                    list.push(el);
                }
            }
        });
        return list;
    }

    function moveFocus(dir) {
        var elements = getFocusableElements();
        if (!elements || elements.length === 0) return;

        var current = document.activeElement;
        // If nothing is focused or focus is on document.body, pick active tab
        if (!current || current === document.body || elements.indexOf(current) === -1) {
            var activeTab = document.querySelector('.nav-tab.active') || elements[0];
            activeTab.focus();
            return;
        }

        var curRect = current.getBoundingClientRect();
        var curCenterX = curRect.left + curRect.width / 2;
        var curCenterY = curRect.top + curRect.height / 2;

        var bestMatch = null;
        var bestScore = Infinity;

        elements.forEach(function (el) {
            if (el === current) return;
            var r = el.getBoundingClientRect();
            var cx = r.left + r.width / 2;
            var cy = r.top + r.height / 2;

            var dx = cx - curCenterX;
            var dy = cy - curCenterY;

            var isCandidate = false;
            var primaryDist = 0;
            var secondaryDist = 0;

            if (dir === 'up') {
                if (dy < -6) {
                    isCandidate = true;
                    primaryDist = Math.abs(dy);
                    secondaryDist = Math.abs(dx);
                }
            } else if (dir === 'down') {
                if (dy > 6) {
                    isCandidate = true;
                    primaryDist = Math.abs(dy);
                    secondaryDist = Math.abs(dx);
                }
            } else if (dir === 'left') {
                if (dx < -6) {
                    isCandidate = true;
                    primaryDist = Math.abs(dx);
                    secondaryDist = Math.abs(dy);
                }
            } else if (dir === 'right') {
                if (dx > 6) {
                    isCandidate = true;
                    primaryDist = Math.abs(dx);
                    secondaryDist = Math.abs(dy);
                }
            }

            if (isCandidate) {
                var score = primaryDist + secondaryDist * 1.8;
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = el;
                }
            }
        });

        // Fallback transitions between Rail and Content
        if (!bestMatch) {
            if (dir === 'left') {
                var activeRailTab = document.querySelector('.nav-tab.active') || document.getElementById('tabHome');
                if (activeRailTab && activeRailTab !== current) {
                    bestMatch = activeRailTab;
                }
            } else if (dir === 'right') {
                var firstActionEl = document.querySelector('.tab-view.active .media-chip') ||
                                    document.querySelector('.tab-view.active .prompt-chip') ||
                                    document.querySelector('.tab-view.active .yt-video-card') ||
                                    document.querySelector('.tab-view.active input') ||
                                    document.getElementById('globalTvSearchInput');
                if (firstActionEl && firstActionEl !== current) {
                    bestMatch = firstActionEl;
                }
            }
        }

        if (bestMatch) {
            bestMatch.focus();
            if (typeof bestMatch.scrollIntoView === 'function') {
                bestMatch.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        }
    }

    // 14. BACK KEY HANDLING (Physical Return button and ESC)
    function handleBackKey() {
        showRemoteBadge("⮌ BACK");
        if (isCinemaMode) {
            exitCinemaMode();
        } else if (assistantHudOverlay && !assistantHudOverlay.classList.contains('hidden')) {
            hideAssistantHud();
        } else if (currentActiveTab !== 'home') {
            switchTab('home');
            var tabHome = document.getElementById('tabHome');
            if (tabHome) tabHome.focus();
        } else {
            try {
                if (typeof tizen !== 'undefined' && tizen.application) {
                    tizen.application.getCurrentApplication().exit();
                }
            } catch (ex) {
                console.log("Exit exception:", ex);
            }
        }
    }

    // Listen to Tizen Hardware Key Event (MANDATORY for Samsung Smart TV)
    window.addEventListener('tizenhwkey', function (e) {
        if (e.keyName === 'back') {
            handleBackKey();
        }
    });

    // 15. REMOTE KEY EVENT LISTENER
    function onRemoteKeyDown(e) {
        var keyCode = e.keyCode;
        console.log("TV Remote Keycode:", keyCode);

        switch (keyCode) {
            case 38: // Arrow Up
                showRemoteBadge("▲ UP");
                moveFocus('up');
                e.preventDefault();
                break;
            case 40: // Arrow Down
                showRemoteBadge("▼ DOWN");
                moveFocus('down');
                e.preventDefault();
                break;
            case 37: // Arrow Left
                showRemoteBadge("◄ LEFT");
                moveFocus('left');
                e.preventDefault();
                break;
            case 39: // Arrow Right
                showRemoteBadge("► RIGHT");
                moveFocus('right');
                e.preventDefault();
                break;
            case 13: // Enter / OK / Select
                showRemoteBadge("✔ SELECT");
                if (document.activeElement) {
                    if (document.activeElement.tagName === 'INPUT') {
                        // Let input handle enter natively
                    } else if (typeof document.activeElement.click === 'function') {
                        document.activeElement.click();
                        e.preventDefault();
                    }
                }
                break;
            case 10009: // Tizen Return (Back)
            case 27:    // ESC
                handleBackKey();
                e.preventDefault();
                break;
            case 10182: // Tizen Exit Key
                showRemoteBadge("EXIT");
                try {
                    if (typeof tizen !== 'undefined' && tizen.application) {
                        tizen.application.getCurrentApplication().exit();
                    }
                } catch (_) {}
                break;
            case 415:   // Media Play
            case 10252:
                showRemoteBadge("▶ PLAY");
                break;
            case 19:    // Media Pause
                showRemoteBadge("⏸ PAUSE");
                break;
            case 413:   // Media Stop
                showRemoteBadge("⏹ STOP");
                break;
            case 417:   // Media FastForward
                showRemoteBadge("⏩ FAST FORWARD");
                break;
            case 412:   // Media Rewind
                showRemoteBadge("⏪ REWIND");
                break;
            case 403:   // Red key -> Home
                showRemoteBadge("🔴 HOME");
                switchTab('home');
                break;
            case 404:   // Green key -> YouTube
                showRemoteBadge("🟢 YOUTUBE");
                switchTab('youtube');
                break;
            case 405:   // Yellow key -> Spotify
                showRemoteBadge("🟡 SPOTIFY");
                switchTab('spotify');
                break;
            case 406:   // Blue key -> Search
                showRemoteBadge("🔵 SEARCH");
                switchTab('search');
                break;
            case 49: // '1' -> Home
                switchTab('home');
                break;
            case 50: // '2' -> YouTube
                switchTab('youtube');
                break;
            case 51: // '3' -> Spotify
                switchTab('spotify');
                break;
            case 52: // '4' -> Apps
                switchTab('apps');
                break;
            case 53: // '5' -> Search
                switchTab('search');
                break;
            default:
                break;
        }
    }

    window.addEventListener('keydown', onRemoteKeyDown, true);
    document.addEventListener('keydown', onRemoteKeyDown, true);

    // 16. UI EVENT LISTENERS
    navTabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            switchTab(this.getAttribute('data-tab'));
        });
    });

    if (globalTvSearchInput) {
        globalTvSearchInput.addEventListener('keydown', function (e) {
            if (e.keyCode === 13 && this.value.trim()) {
                executeVoiceCommand(this.value.trim());
                this.value = '';
                this.blur();
                moveFocus('down');
            }
        });
    }

    if (ytSearchBox) {
        ytSearchBox.addEventListener('keydown', function (e) {
            if (e.keyCode === 13 && this.value.trim()) {
                loadEmbeddedYouTube(this.value.trim());
            }
        });
    }

    if (dedicatedSearchInput) {
        dedicatedSearchInput.addEventListener('keydown', function (e) {
            if (e.keyCode === 13 && this.value.trim()) {
                loadEmbeddedYouTube(this.value.trim());
            }
        });
    }

    if (btnSearchYouTube) {
        btnSearchYouTube.addEventListener('click', function () {
            var q = dedicatedSearchInput ? dedicatedSearchInput.value.trim() : "";
            loadEmbeddedYouTube(q || "Bengali songs");
        });
    }

    if (btnSearchSpotify) {
        btnSearchSpotify.addEventListener('click', function () {
            loadEmbeddedSpotify('playlist/37i9dQZF1DX0XUfTFmZeMt', "Today's Top Hits");
        });
    }

    if (btnSearchAi) {
        btnSearchAi.addEventListener('click', function () {
            var q = dedicatedSearchInput ? dedicatedSearchInput.value.trim() : "";
            if (q) executeVoiceCommand(q);
        });
    }

    if (btnTraySend && assistantDirectInput) {
        btnTraySend.addEventListener('click', function () {
            if (assistantDirectInput.value.trim()) {
                executeVoiceCommand(assistantDirectInput.value.trim());
                assistantDirectInput.value = '';
            }
        });
        assistantDirectInput.addEventListener('keydown', function (e) {
            if (e.keyCode === 13 && this.value.trim()) {
                executeVoiceCommand(this.value.trim());
                this.value = '';
                moveFocus('up');
            }
        });
    }

    if (btnTrayVoice) {
        btnTrayVoice.addEventListener('click', function () {
            showAssistantHud("Listening...", "VOICE LISTENER", "Speak into your Galaxy M30s remote microphone...", false);
        });
    }

    if (headerMicBtn) {
        headerMicBtn.addEventListener('click', function () {
            showAssistantHud("Listening...", "VOICE LISTENER", "Speak a TV command on your Galaxy M30s remote...", false);
        });
    }

    // 17. EXPOSE GLOBAL TV APP INTERFACE
    window.TvApp = {
        switchTab: switchTab,
        launchApp: launchApp,
        launchYouTube: function () { loadEmbeddedYouTube("Bengali songs"); },
        launchSpotify: function () { loadEmbeddedSpotify('playlist/37i9dQZF1DX0XUfTFmZeMt', "Today's Top Hits"); },
        loadEmbeddedYouTube: loadEmbeddedYouTube,
        loadEmbeddedSpotify: loadEmbeddedSpotify,
        searchYouTube: loadEmbeddedYouTube,
        toggleCinemaMode: toggleCinemaMode,
        exitCinemaMode: exitCinemaMode,
        executeVoiceCommand: executeVoiceCommand,
        showAssistant: showAssistantHud,
        speak: speakOnTv,
        adjustVolume: adjustVolume,
        toggleMute: toggleMute,
        moveFocus: moveFocus
    };

    // 18. INITIALIZATION
    registerTizenKeys();
    switchTab('home');

    // Auto-focus default tab on app open so TV remote is active immediately
    document.body.tabIndex = 0;
    setTimeout(function () {
        var tabHome = document.getElementById('tabHome');
        if (tabHome) {
            tabHome.focus();
        }
    }, 150);

    console.log("Samsung Smart TV AI Entertainment Hub loaded with In-App Real YouTube TV & Spotify Web Player.");
})();
