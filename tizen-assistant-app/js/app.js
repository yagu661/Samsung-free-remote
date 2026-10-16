/**
 * Samsung Smart TV AI Hub - Tizen Web Application Controller
 * Target: Samsung UA32T4410AKXXL (Tizen 5.5 / 5.0 HD)
 * Built with love by Soumya for complete TV entertainment & AI assistance
 */

(function () {
    'use strict';

    // Curated Playable Video Database (including Bengali songs as requested)
    var VIDEO_DATABASE = {
        bengali: [
            { id: "Qz3x8jDvZ-Q", title: "Arijit Singh Bengali Romance Mashup", channel: "SVF Music", duration: "18:42", thumb: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80" },
            { id: "5qap5aO4i9A", title: "All Time Hit Bengali Songs Collection", channel: "Saregama Bengali", duration: "42:15", thumb: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80" },
            { id: "J7uX8U2Q7aA", title: "Best of Rabindra Sangeet | Soulful", channel: "Bhavna Records", duration: "35:10", thumb: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80" },
            { id: "2g811Eo7K8U", title: "Modern Bengali Hits 2024", channel: "Zee Music Bangla", duration: "24:50", thumb: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80" }
        ],
        arijit: [
            { id: "Qz3x8jDvZ-Q", title: "Arijit Singh Greatest Hits | Bengali & Hindi", channel: "SVF Music", duration: "32:00", thumb: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80" },
            { id: "kJQP7kiw5Fk", title: "Despacito & Acoustic Melodies", channel: "Arijit Official", duration: "15:20", thumb: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80" }
        ],
        lofi: [
            { id: "jfKfPfyJRdk", title: "Lofi Hip Hop Radio - Beats to Relax/Study", channel: "Lofi Girl", duration: "LIVE", thumb: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80" },
            { id: "5yx6BWlEVcY", title: "Chillhop Essentials • Peaceful Evening", channel: "Chillhop Music", duration: "55:30", thumb: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80" }
        ],
        movies: [
            { id: "TcMBFSGVi1c", title: "Avengers: Secret Wars - Official Teaser", channel: "Marvel Studios", duration: "2:45", thumb: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80" },
            { id: "d9MyW72ELq0", title: "Avatar: The Way of Water 4K Trailer", channel: "20th Century", duration: "3:10", thumb: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80" }
        ],
        news: [
            { id: "21X5lGlDOfg", title: "NASA Live: Earth from Space Station", channel: "NASA", duration: "LIVE", thumb: "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80" },
            { id: "lTRiuFIWV54", title: "Top Tech Gadgets of 2026", channel: "MKBHD", duration: "14:10", thumb: "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=500&q=80" }
        ]
    };

    // DOM Elements
    var topClockEl = document.getElementById('topTvClock');
    var navTabs = Array.prototype.slice.call(document.querySelectorAll('.nav-tab'));
    var tabViews = Array.prototype.slice.call(document.querySelectorAll('.tab-view'));
    var ytIframe = document.getElementById('ytIframe');
    var ytCurrentTitle = document.getElementById('ytCurrentTitle');
    var ytCurrentChannel = document.getElementById('ytCurrentChannel');
    var ytVideoGrid = document.getElementById('ytVideoGrid');
    var spotifyIframe = document.getElementById('spotifyIframe');
    var assistantHudOverlay = document.getElementById('assistantHudOverlay');
    var hudQueryText = document.getElementById('hudQueryText');
    var hudResponseText = document.getElementById('hudResponseText');
    var hudActionPill = document.getElementById('hudActionPill');
    var tvVolumeToast = document.getElementById('tvVolumeToast');
    var volBarFill = document.getElementById('volBarFill');
    var volText = document.getElementById('volText');
    var globalTvSearchInput = document.getElementById('globalTvSearchInput');
    var assistantChatHistory = document.getElementById('assistantChatHistory');
    var assistantDirectInput = document.getElementById('assistantDirectInput');
    var btnTraySend = document.getElementById('btnTraySend');
    var btnTrayVoice = document.getElementById('btnTrayVoice');
    var headerMicBtn = document.getElementById('headerMicBtn');

    var currentVolume = 25;
    var hudTimer = null;
    var volTimer = null;
    var currentActiveTab = 'home';

    // Clock
    function updateClock() {
        var now = new Date();
        var hours = now.getHours();
        var minutes = now.getMinutes();
        var ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        var minStr = minutes < 10 ? '0' + minutes : minutes;
        topClockEl.textContent = hours + ':' + minStr + ' ' + ampm;
    }
    setInterval(updateClock, 1000);
    updateClock();

    // Tab Navigation
    function switchTab(tabId) {
        currentActiveTab = tabId;
        navTabs.forEach(function (tab) {
            if (tab.getAttribute('data-tab') === tabId) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        tabViews.forEach(function (view) {
            if (view.id === 'view' + tabId.charAt(0).toUpperCase() + tabId.slice(1)) {
                view.classList.add('active');
            } else {
                view.classList.remove('active');
            }
        });

        if (tabId === 'youtube' && (!ytVideoGrid.children || ytVideoGrid.children.length === 0)) {
            filterYouTube('bengali');
        }
    }

    // YouTube Grid Population
    function filterYouTube(category) {
        var chips = Array.prototype.slice.call(document.querySelectorAll('.yt-cat-chip'));
        chips.forEach(function (chip) {
            chip.classList.toggle('active', chip.textContent.toLowerCase().indexOf(category) !== -1);
        });

        ytVideoGrid.innerHTML = '';
        var items = VIDEO_DATABASE[category] || VIDEO_DATABASE.bengali;
        items.forEach(function (video) {
            var card = document.createElement('div');
            card.className = 'video-card';
            card.tabIndex = 0;
            card.innerHTML = 
                '<div class="video-thumb" style="background-image: url(\'' + video.thumb + '\');">' +
                    '<span class="video-badge">' + video.duration + '</span>' +
                '</div>' +
                '<div class="video-info">' +
                    '<h5>' + video.title + '</h5>' +
                    '<span>' + video.channel + '</span>' +
                '</div>';

            card.addEventListener('click', function () {
                playYouTubeVideo(video.id, video.title, video.channel);
            });
            card.addEventListener('keydown', function (e) {
                if (e.keyCode === 13) {
                    playYouTubeVideo(video.id, video.title, video.channel);
                }
            });
            ytVideoGrid.appendChild(card);
        });
    }

    function playYouTubeVideo(videoId, title, channel) {
        ytIframe.src = "https://www.youtube-nocookie.com/embed/" + videoId + "?enablejsapi=1&autoplay=1&rel=0&modestbranding=1";
        ytCurrentTitle.textContent = title;
        ytCurrentChannel.textContent = channel + " • Playing Now";
        showAssistantHud('"' + title + '"', 'YOUTUBE PLAY', 'Now streaming on your TV...');
    }

    function searchYouTubeCustom(query) {
        switchTab('youtube');
        showAssistantHud('"' + query + '"', 'SEARCHING YOUTUBE', 'Searching YouTube for ' + query + '...');
        var encoded = encodeURIComponent(query);
        ytIframe.src = "https://www.youtube-nocookie.com/embed?listType=search&list=" + encoded + "&autoplay=1";
        ytCurrentTitle.textContent = "Search: " + query;
        ytCurrentChannel.textContent = "YouTube TV Player • Streaming Search Results";
    }

    // Spotify Hub
    function loadSpotifyPlaylist(playlistId, title) {
        spotifyIframe.src = "https://open.spotify.com/embed/playlist/" + playlistId + "?utm_source=generator&theme=0";
        showAssistantHud('"' + title + '"', 'SPOTIFY MUSIC', 'Now loading playlist on TV Spotify Hub...');
    }

    // Assistant Bottom HUD (Google Assistant / Android TV Style)
    function showAssistantHud(query, action, response) {
        hudQueryText.textContent = query;
        hudActionPill.textContent = action;
        hudResponseText.textContent = response;
        assistantHudOverlay.classList.remove('hidden');

        // Add to assistant chat history
        appendChatMessage('user', query);
        appendChatMessage('ai', response);

        clearTimeout(hudTimer);
        hudTimer = setTimeout(function () {
            hideAssistantHud();
        }, 7500);
    }

    function hideAssistantHud() {
        assistantHudOverlay.classList.add('hidden');
    }

    function appendChatMessage(sender, text) {
        var bubble = document.createElement('div');
        bubble.className = 'chat-bubble ' + sender;
        bubble.innerHTML = 
            '<div class="chat-avatar">' + (sender === 'ai' ? 'AI' : 'YOU') + '</div>' +
            '<div class="chat-body">' +
                '<strong>' + (sender === 'ai' ? 'Samsung TV Assistant' : 'You (Galaxy M30s)') + '</strong>' +
                '<p>' + text + '</p>' +
            '</div>';
        assistantChatHistory.appendChild(bubble);
        assistantChatHistory.scrollTop = assistantChatHistory.scrollHeight;
    }

    // Volume Control
    function adjustVolume(delta) {
        currentVolume = Math.max(0, Math.min(100, currentVolume + delta));
        volBarFill.style.width = currentVolume + '%';
        volText.textContent = currentVolume + '%';
        tvVolumeToast.classList.add('visible');

        if (typeof tizen !== 'undefined' && tizen.tvaudiocontrol) {
            try {
                tizen.tvaudiocontrol.setVolume(currentVolume);
            } catch (e) {}
        }

        clearTimeout(volTimer);
        volTimer = setTimeout(function () {
            tvVolumeToast.classList.remove('visible');
        }, 2200);
    }

    function toggleMute() {
        volText.textContent = "MUTED";
        volBarFill.style.width = "0%";
        tvVolumeToast.classList.add('visible');
        if (typeof tizen !== 'undefined' && tizen.tvaudiocontrol) {
            try {
                var muted = tizen.tvaudiocontrol.isMute();
                tizen.tvaudiocontrol.setMute(!muted);
                volText.textContent = !muted ? "MUTED" : currentVolume + "%";
            } catch (e) {}
        }
        clearTimeout(volTimer);
        volTimer = setTimeout(function () {
            tvVolumeToast.classList.remove('visible');
        }, 2200);
    }

    // Launch Native Samsung TV Apps (Netflix, Prime, Browser)
    function launchNativeApp(appId, appName) {
        showAssistantHud("Open " + appName, "LAUNCH APP", "Starting " + appName + " on Samsung TV...");
        if (typeof tizen !== 'undefined' && tizen.application) {
            try {
                tizen.application.launch(appId, function () {
                    console.log("Launched " + appId);
                }, function (err) {
                    console.error("Launch error:", err);
                    showAssistantHud("Launch Error", "ERROR", "Could not start " + appName + ": " + err.message);
                });
            } catch (e) {
                console.error("Launch exception:", e);
            }
        }
    }

    // Voice & Natural Language Command Dispatcher
    function executeVoiceCommand(spokenText) {
        if (!spokenText || !spokenText.trim()) return;
        var text = spokenText.trim().toLowerCase();

        // 1. YouTube commands
        if (text.indexOf('youtube') !== -1 || text.indexOf('play ') !== -1 || text.indexOf('search ') !== -1) {
            var q = spokenText.replace(/search/gi, '').replace(/youtube/gi, '').replace(/for/gi, '').replace(/play/gi, '').replace(/on/gi, '').trim();
            if (!q) q = "Bengali songs";
            searchYouTubeCustom(q);
            return;
        }

        // 2. Spotify commands
        if (text.indexOf('spotify') !== -1 || text.indexOf('music') !== -1 || text.indexOf('song') !== -1) {
            switchTab('spotify');
            showAssistantHud(spokenText, "SPOTIFY ACTIVE", "Opened Spotify Music Hub. Playing selected track...");
            return;
        }

        // 3. Netflix
        if (text.indexOf('netflix') !== -1) {
            launchNativeApp('11101200001', 'Netflix');
            return;
        }

        // 4. Volume
        if (text.indexOf('volume up') !== -1 || text.indexOf('louder') !== -1) {
            adjustVolume(3);
            showAssistantHud(spokenText, "VOLUME +3", "Volume increased on TV.");
            return;
        }
        if (text.indexOf('volume down') !== -1 || text.indexOf('lower') !== -1) {
            adjustVolume(-3);
            showAssistantHud(spokenText, "VOLUME -3", "Volume decreased on TV.");
            return;
        }
        if (text.indexOf('mute') !== -1) {
            toggleMute();
            showAssistantHud(spokenText, "MUTE", "Toggled TV Audio Mute.");
            return;
        }

        // 5. Navigation
        if (text.indexOf('home') !== -1) {
            switchTab('home');
            showAssistantHud(spokenText, "GO HOME", "Returned to Home Smart Hub.");
            return;
        }

        // General AI Conversational Reply
        showAssistantHud(spokenText, "AI ASSISTANT", "Processing via OpenRouter AI for your Samsung Smart TV...");
    }

    // Remote Activity Toast / Feedback Badge
    var tvRemoteBadge = document.getElementById('tvRemoteBadge');
    var badgeTimer = null;
    function showRemoteBadge(text) {
        if (!tvRemoteBadge) return;
        tvRemoteBadge.textContent = text;
        tvRemoteBadge.classList.add('visible');
        if (badgeTimer) clearTimeout(badgeTimer);
        badgeTimer = setTimeout(function () {
            tvRemoteBadge.classList.remove('visible');
        }, 1200);
    }

    // Register Tizen TV hardware keys
    function registerTizenKeys() {
        if (typeof tizen !== 'undefined' && tizen.tvinputdevice) {
            var keys = [
                'VolumeUp', 'VolumeDown', 'VolumeMute',
                'MediaPlay', 'MediaPause', 'MediaStop', 'MediaFastForward', 'MediaRewind',
                'ColorF0Red', 'ColorF1Green', 'ColorF2Yellow', 'ColorF3Blue',
                'ChannelUp', 'ChannelDown', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
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

    // 2D Spatial Navigation Engine for Samsung TV D-Pad (Up, Down, Left, Right)
    function getFocusableElements() {
        var selectors = [
            '.nav-tab',
            '#globalTvSearchInput',
            '#headerMicBtn',
            '.tab-view.active .prompt-chip',
            '.tab-view.active .feature-card',
            '.tab-view.active .yt-cat-chip',
            '.tab-view.active .video-card',
            '.tab-view.active .music-card',
            '.tab-view.active .stream-poster',
            '.tab-view.active .assistant-input-tray input',
            '.tab-view.active .assistant-input-tray button'
        ];
        var list = [];
        selectors.forEach(function (sel) {
            var els = document.querySelectorAll(sel);
            for (var i = 0; i < els.length; i++) {
                var el = els[i];
                if (el.offsetParent !== null && !el.disabled) {
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
        if (!current || elements.indexOf(current) === -1) {
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
                if (dy < -10) {
                    isCandidate = true;
                    primaryDist = Math.abs(dy);
                    secondaryDist = Math.abs(dx);
                }
            } else if (dir === 'down') {
                if (dy > 10) {
                    isCandidate = true;
                    primaryDist = Math.abs(dy);
                    secondaryDist = Math.abs(dx);
                }
            } else if (dir === 'left') {
                if (dx < -10) {
                    isCandidate = true;
                    primaryDist = Math.abs(dx);
                    secondaryDist = Math.abs(dy);
                }
            } else if (dir === 'right') {
                if (dx > 10) {
                    isCandidate = true;
                    primaryDist = Math.abs(dx);
                    secondaryDist = Math.abs(dy);
                }
            }

            if (isCandidate) {
                var score = primaryDist + secondaryDist * 2.2;
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = el;
                }
            }
        });

        if (bestMatch) {
            bestMatch.focus();
            if (typeof bestMatch.scrollIntoView === 'function') {
                bestMatch.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        }
    }

    // Media pause helper
    function pauseAllMedia() {
        try {
            if (ytIframe && ytIframe.contentWindow) {
                ytIframe.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*');
            }
        } catch (_) {}
    }

    // Media play helper
    function playCurrentMedia() {
        try {
            if (ytIframe && ytIframe.contentWindow) {
                ytIframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
            }
        } catch (_) {}
    }

    // Remote Control Keys (D-Pad navigation & Media Keys)
    document.addEventListener('keydown', function (e) {
        var keyCode = e.keyCode;
        console.log("TV KeyCode pressed:", keyCode);

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
                        // Enter in input handled by input keydown
                    } else {
                        document.activeElement.click();
                        e.preventDefault();
                    }
                }
                break;
            case 10009: // Tizen Return (Back)
            case 27:    // ESC
                showRemoteBadge("⮌ BACK");
                if (!assistantHudOverlay.classList.contains('hidden')) {
                    hideAssistantHud();
                } else if (currentActiveTab !== 'home') {
                    switchTab('home');
                    var homeTab = document.getElementById('tabHome');
                    if (homeTab) homeTab.focus();
                } else {
                    try { tizen.application.getCurrentApplication().exit(); } catch (ex) {}
                }
                e.preventDefault();
                break;
            case 447: // Volume Up
                showRemoteBadge("VOL +");
                adjustVolume(2);
                break;
            case 448: // Volume Down
                showRemoteBadge("VOL -");
                adjustVolume(-2);
                break;
            case 449: // Volume Mute
                showRemoteBadge("MUTE");
                toggleMute();
                break;
            case 19: // Pause
                showRemoteBadge("PAUSE");
                pauseAllMedia();
                break;
            case 415: // Play
                showRemoteBadge("PLAY");
                playCurrentMedia();
                break;
            case 403: // Red key -> Home
                showRemoteBadge("🔴 HOME");
                switchTab('home');
                break;
            case 404: // Green key -> YouTube
                showRemoteBadge("🟢 YOUTUBE");
                switchTab('youtube');
                break;
            case 405: // Yellow key -> Spotify
                showRemoteBadge("🟡 SPOTIFY");
                switchTab('spotify');
                break;
            case 406: // Blue key -> Assistant
                showRemoteBadge("🔵 ASSISTANT");
                switchTab('assistant');
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
            case 52: // '4' -> Streaming
                switchTab('streaming');
                break;
            case 53: // '5' -> Assistant
                switchTab('assistant');
                break;
            default:
                break;
        }
    });

    // Tab buttons click listeners
    navTabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            switchTab(this.getAttribute('data-tab'));
        });
    });

    // Global Search Input
    globalTvSearchInput.addEventListener('keydown', function (e) {
        if (e.keyCode === 13 && this.value.trim()) {
            executeVoiceCommand(this.value.trim());
            this.value = '';
            this.blur();
            moveFocus('down');
        }
    });

    // Assistant Tray Input
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
    btnTrayVoice.addEventListener('click', function () {
        showAssistantHud("Listening...", "VOICE LISTENER", "Speak into your Galaxy M30s remote or Samsung voice remote.");
    });
    headerMicBtn.addEventListener('click', function () {
        showAssistantHud("Listening...", "VOICE LISTENER", "Speak a TV command on your phone remote...");
    });

    // Expose Global App Interface for Companion Sync
    window.TvApp = {
        switchTab: switchTab,
        searchYouTube: searchYouTubeCustom,
        filterYouTube: filterYouTube,
        playYouTube: playYouTubeVideo,
        loadSpotifyPlaylist: loadSpotifyPlaylist,
        launchNativeApp: launchNativeApp,
        executeVoiceCommand: executeVoiceCommand,
        showAssistant: showAssistantHud,
        adjustVolume: adjustVolume,
        toggleMute: toggleMute,
        moveFocus: moveFocus
    };

    // Initialize Default View & Register TV Remote Keys
    registerTizenKeys();
    switchTab('home');
    filterYouTube('bengali');

    // Auto-focus default tab on app open so TV remote is active immediately
    setTimeout(function () {
        var tabHome = document.getElementById('tabHome');
        if (tabHome) {
            tabHome.focus();
        }
    }, 150);

    console.log("Samsung Smart TV AI Hub loaded with complete 2D D-Pad Spatial Navigation.");
})();

