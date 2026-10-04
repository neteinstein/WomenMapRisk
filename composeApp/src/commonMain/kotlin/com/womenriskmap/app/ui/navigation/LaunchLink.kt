package com.womenriskmap.app.ui.navigation

/** Invite code from the launch link, if any (web: `?invite=CODE`; mobile deep links are a follow-up). */
expect fun launchInviteCode(): String?
